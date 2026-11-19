package com.lavyoung.gateweave.center.gateway.persistence;

import com.lavyoung.gateweave.center.gateway.application.GatewayNodePageQuery;
import com.lavyoung.gateweave.center.gateway.application.GatewayNodeQueryRepository;
import com.lavyoung.gateweave.center.gateway.domain.*;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayGroupNotFoundException;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayNodeAddressConflictException;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 使用 Spring JDBC 持久化网关算力节点。
 *
 * <p>适配器使用节点唯一索引处理重复注册，并保留节点已经加载的配置版本。
 * 数据库记录在适配器边界转换为领域对象，不向领域层暴露 JDBC 类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
@Repository
public class JdbcGatewayNodeRepository implements GatewayNodeRepository, GatewayNodeQueryRepository {

    private static final String UPDATE_NODE_SQL = """
            UPDATE gw_gateway_node
            SET gateway_group_id = :gatewayGroupId,
                node_name = :nodeName,
                advertised_address = :advertisedAddress,
                status = :status,
                lease_id = :leaseId,
                last_heartbeat_at = :lastHeartbeatAt,
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE node_id = :nodeId
            """;

    private static final String INSERT_NODE_SQL = """
            INSERT INTO gw_gateway_node
                (node_id, gateway_group_id, node_name, advertised_address, status, lease_id,
                 last_heartbeat_at, current_config_version)
            VALUES
                (:nodeId, :gatewayGroupId, :nodeName, :advertisedAddress, :status, :leaseId,
                 :lastHeartbeatAt, :currentConfigVersion)
            """;

    private static final String EXISTS_NODE_SQL = """
            SELECT COUNT(*)
            FROM gw_gateway_node
            WHERE node_id = :nodeId
            """;

    private static final String EXISTS_GATEWAY_GROUP_SQL = """
            SELECT COUNT(*)
            FROM gw_gateway_group
            WHERE gateway_group_id = :gatewayGroupId
            """;

    private static final String FIND_NODE_SQL = """
            SELECT node_id,
                   gateway_group_id,
                   node_name,
                   advertised_address,
                   status,
                   lease_id,
                   last_heartbeat_at,
                   current_config_version
            FROM gw_gateway_node
            WHERE node_id = :nodeId
            """;

    private static final String RENEW_LEASE_SQL = """
            UPDATE gw_gateway_node
            SET last_heartbeat_at = :receivedAt,
                current_config_version = :currentConfigVersion,
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE node_id = :nodeId
              AND lease_id = :leaseId
              AND status = :status
              AND current_config_version <= :currentConfigVersion
            """;

    private static final String RELEASE_LEASE_SQL = """
            UPDATE gw_gateway_node
            SET status = :offlineStatus,
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE node_id = :nodeId
              AND lease_id = :leaseId
              AND status = :onlineStatus
            """;

    private static final String MARK_EXPIRED_LEASES_OFFLINE_SQL = """
            UPDATE gw_gateway_node
            SET status = :offlineStatus,
                updated_at = CURRENT_TIMESTAMP(6)
            WHERE status = :onlineStatus
              AND last_heartbeat_at <= :heartbeatDeadline
            """;

    private static final String COUNT_MATCHING_NODES_SQL = """
            SELECT COUNT(*)
            FROM gw_gateway_node
            WHERE 1 = 1
            %s
            """;

    private static final String FIND_MATCHING_NODES_SQL = """
            SELECT node_id,
                   gateway_group_id,
                   node_name,
                   advertised_address,
                   status,
                   lease_id,
                   last_heartbeat_at,
                   current_config_version
            FROM gw_gateway_node
            WHERE 1 = 1
            %s
            ORDER BY id, node_id
            LIMIT :limit OFFSET :offset
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * 创建网关节点 JDBC 持久化适配器。
     *
     * @param jdbcTemplate Spring JDBC 命名参数模板
     * @throws NullPointerException jdbcTemplate 为 null 时抛出
     */
    public JdbcGatewayNodeRepository(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional
    public GatewayNodeRegistrationResult registerOrRefresh(GatewayNode node) {
        GatewayNode checkedNode = Objects.requireNonNull(node, "node must not be null");
        GatewayNodeRegistration registration = checkedNode.registration();
        String gatewayGroupId = registration.gatewayGroupId();

        if (!existsGatewayGroup(gatewayGroupId)) {
            throw new GatewayGroupNotFoundException(gatewayGroupId);
        }

        try {
            return registerOrRefreshExistingGroup(checkedNode);
        } catch (DuplicateKeyException exception) {
            throw new GatewayNodeAddressConflictException(registration.advertisedAddress());
        } catch (DataIntegrityViolationException exception) {
            if (!existsGatewayGroup(gatewayGroupId)) {
                throw new GatewayGroupNotFoundException(gatewayGroupId);
            }
            throw exception;
        }
    }

    @Override
    public boolean renewLease(GatewayNodeHeartbeat heartbeat) {
        Objects.requireNonNull(heartbeat, "heartbeat must not be null");
        MapSqlParameterSource parameterSource = new MapSqlParameterSource().addValue("nodeId", heartbeat.nodeId())
                .addValue("leaseId", heartbeat.leaseId().toString()).addValue("status", GatewayNodeStatus.ONLINE.name())
                .addValue("receivedAt", Timestamp.from(heartbeat.receivedAt()))
                .addValue("currentConfigVersion", heartbeat.currentConfigVersion());
        return this.jdbcTemplate.update(RENEW_LEASE_SQL, parameterSource) == 1;
    }

    @Override
    public boolean releaseLease(String nodeId, UUID leaseId) {
        Objects.requireNonNull(nodeId, "nodeId must not be null");
        Objects.requireNonNull(leaseId, "leaseId must not be null");
        MapSqlParameterSource parameterSource =
                new MapSqlParameterSource().addValue("nodeId", nodeId).addValue("leaseId", leaseId.toString())
                        .addValue("onlineStatus", GatewayNodeStatus.ONLINE.name())
                        .addValue("offlineStatus", GatewayNodeStatus.OFFLINE.name());
        return this.jdbcTemplate.update(RELEASE_LEASE_SQL, parameterSource) == 1;
    }

    @Override
    public int markExpiredLeasesOffline(Instant heartbeatDeadline) {
        Objects.requireNonNull(heartbeatDeadline, "heartbeatDeadline must not be null");
        MapSqlParameterSource parameterSource =
                new MapSqlParameterSource().addValue("onlineStatus", GatewayNodeStatus.ONLINE.name())
                        .addValue("offlineStatus", GatewayNodeStatus.OFFLINE.name())
                        .addValue("heartbeatDeadline", Timestamp.from(heartbeatDeadline));
        return this.jdbcTemplate.update(MARK_EXPIRED_LEASES_OFFLINE_SQL, parameterSource);
    }

    @Override
    public long countMatching(GatewayNodePageQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        String sql = COUNT_MATCHING_NODES_SQL.formatted(filterSql(query));
        Long count = this.jdbcTemplate.queryForObject(sql, queryParameters(query), Long.class);
        if (count == null) {
            throw new DataRetrievalFailureException("Gateway node count query returned no result");
        }
        return count;
    }

    @Override
    public List<GatewayNode> findMatchingPage(GatewayNodePageQuery query) {
        Objects.requireNonNull(query, "query must not be null");

        String sql = FIND_MATCHING_NODES_SQL.formatted(filterSql(query));
        PageRequest pageRequest = query.pageRequest();
        MapSqlParameterSource parameters = queryParameters(query).addValue("offset", pageRequest.offset())
                .addValue("limit", pageRequest.pageSize());
        return this.jdbcTemplate.query(sql, parameters, this::mapNode);
    }

    /**
     * 在网关分组已经存在的前提下注册或刷新节点。
     *
     * @param node 待注册的在线节点
     * @return 节点注册结果
     * @throws DuplicateKeyException 唯一键冲突无法通过相同节点重试解决时抛出
     */
    private GatewayNodeRegistrationResult registerOrRefreshExistingGroup(GatewayNode node) {
        String nodeId = node.registration().nodeId();
        if (updateNode(node) > 0 || existsByNodeId(nodeId)) {
            return result(nodeId, GatewayNodeRegistrationOutcome.REFRESHED);
        }

        try {
            insertNode(node);
            return result(nodeId, GatewayNodeRegistrationOutcome.REGISTERED);
        } catch (DuplicateKeyException exception) {
            if (updateNode(node) == 0 && !existsByNodeId(nodeId)) {
                throw exception;
            }
            return result(nodeId, GatewayNodeRegistrationOutcome.REFRESHED);
        }
    }

    private int updateNode(GatewayNode node) {
        return this.jdbcTemplate.update(UPDATE_NODE_SQL, parameters(node));
    }

    private void insertNode(GatewayNode node) {
        this.jdbcTemplate.update(INSERT_NODE_SQL, parameters(node));
    }

    private boolean existsByNodeId(String nodeId) {
        Long count = this.jdbcTemplate.queryForObject(EXISTS_NODE_SQL, new MapSqlParameterSource("nodeId", nodeId),
                Long.class);
        if (count == null) {
            throw new DataRetrievalFailureException("Gateway node existence query returned no result");
        }
        return count > 0;
    }

    private boolean existsGatewayGroup(String gatewayGroupId) {
        Long count = this.jdbcTemplate.queryForObject(EXISTS_GATEWAY_GROUP_SQL,
                new MapSqlParameterSource("gatewayGroupId", gatewayGroupId), Long.class);
        if (count == null) {
            throw new DataRetrievalFailureException("Gateway group existence query returned no result");
        }
        return count > 0;
    }

    private GatewayNodeRegistrationResult result(String nodeId, GatewayNodeRegistrationOutcome outcome) {
        GatewayNode storedNode = findRequiredNode(nodeId);
        return new GatewayNodeRegistrationResult(storedNode, outcome);
    }

    private GatewayNode findRequiredNode(String nodeId) {
        List<GatewayNode> nodes =
                this.jdbcTemplate.query(FIND_NODE_SQL, new MapSqlParameterSource("nodeId", nodeId), this::mapNode);
        if (nodes.size() != 1) {
            throw new DataRetrievalFailureException("Gateway node was not found after registration: " + nodeId);
        }
        return nodes.get(0);
    }

    private MapSqlParameterSource parameters(GatewayNode node) {
        GatewayNodeRegistration registration = node.registration();
        return new MapSqlParameterSource().addValue("nodeId", registration.nodeId())
                .addValue("gatewayGroupId", registration.gatewayGroupId()).addValue("nodeName", registration.nodeName())
                .addValue("advertisedAddress", registration.advertisedAddress().toASCIIString())
                .addValue("status", node.status().name()).addValue("leaseId", node.leaseId().toString())
                .addValue("lastHeartbeatAt", Timestamp.from(node.lastHeartbeatAt()))
                .addValue("currentConfigVersion", node.currentConfigVersion());
    }

    /**
     * 将数据库节点记录转换为领域对象。
     *
     * @param resultSet 当前数据库结果集
     * @param rowNumber 当前行号
     * @return 网关节点领域对象
     * @throws SQLException                  数据库字段读取失败时抛出
     * @throws DataRetrievalFailureException 数据库数据不符合领域约束时抛出
     */
    private GatewayNode mapNode(ResultSet resultSet, int rowNumber) throws SQLException {
        String nodeId = resultSet.getString("node_id");
        try {
            GatewayNodeRegistration registration =
                    new GatewayNodeRegistration(nodeId, resultSet.getString("gateway_group_id"),
                            resultSet.getString("node_name"), URI.create(resultSet.getString("advertised_address")));
            return new GatewayNode(registration, GatewayNodeStatus.valueOf(resultSet.getString("status")),
                    UUID.fromString(resultSet.getString("lease_id")),
                    resultSet.getTimestamp("last_heartbeat_at").toInstant(),
                    resultSet.getLong("current_config_version"));
        } catch (IllegalArgumentException exception) {
            throw new DataRetrievalFailureException("Gateway node contains invalid persisted data: " + nodeId,
                    exception);
        }
    }

    /**
     * 根据应用查询条件生成固定的参数化筛选片段。
     *
     * <p>该方法只拼接代码内确定的 SQL 片段，用户输入始终通过命名参数绑定，
     * 不会直接进入 SQL 文本。</p>
     *
     * @param query 网关节点分页查询条件
     * @return 参数化 SQL 筛选片段
     */
    private String filterSql(GatewayNodePageQuery query) {
        StringBuilder sql = new StringBuilder();

        if (!query.nodeId().isEmpty()) {
            sql.append("AND node_id = :nodeId\n");
        }
        if (!query.gatewayGroupId().isEmpty()) {
            sql.append("AND gateway_group_id = :gatewayGroupId\n");
        }
        if (!query.statuses().isEmpty()) {
            sql.append("AND status IN (:statuses)\n");
        }
        return sql.toString();
    }

    /**
     * 创建与筛选 SQL 对应的命名参数。
     *
     * @param query 网关节点分页查询条件
     * @return JDBC 命名参数
     */
    private MapSqlParameterSource queryParameters(GatewayNodePageQuery query) {
        MapSqlParameterSource parameters = new MapSqlParameterSource();

        if (!query.nodeId().isEmpty()) {
            parameters.addValue("nodeId", query.nodeId());
        }
        if (!query.gatewayGroupId().isEmpty()) {
            parameters.addValue("gatewayGroupId", query.gatewayGroupId());
        }
        if (!query.statuses().isEmpty()) {
            parameters.addValue("statuses", query.statuses().stream().map(GatewayNodeStatus::name).toList());
        }
        return parameters;
    }

}
