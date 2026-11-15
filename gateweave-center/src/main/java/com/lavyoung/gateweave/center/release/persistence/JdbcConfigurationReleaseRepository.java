package com.lavyoung.gateweave.center.release.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayGroupNotFoundException;
import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationSnapshot;
import com.lavyoung.gateweave.contract.configuration.RouteConfiguration;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * 使用关系数据库保存和读取不可变网关配置快照。
 *
 * <p>发布时先锁定网关分组记录，再读取当前最大版本并分配下一版本，随后在同一事务中保存快照和
 * 待投递事件。同一分组的并发发布因此按数据库行锁串行执行，不同分组之间互不阻塞。</p>
 *
 * <p>路由列表使用项目统一的 ObjectMapper 序列化，摘要使用 JDK SHA-256 实现。
 * 摘要只针对数据库保存的原始 JSON，用于读取时识别存储内容损坏；数据面不得通过重新序列化
 * 路由列表复制该计算逻辑。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/02
 */
@Repository
public class JdbcConfigurationReleaseRepository implements ConfigurationReleaseRepository {

    private static final String PUBLISHED_STATUS = "PUBLISHED";
    private static final String PENDING_DELIVERY_STATUS = "PENDING";
    private static final String CONFIGURATION_AGGREGATE_TYPE = "GATEWAY_CONFIGURATION";
    private static final String CONFIGURATION_PUBLISHED_EVENT_TYPE = "GATEWAY_CONFIGURATION_PUBLISHED";
    private static final String CHECKSUM_ALGORITHM = "SHA-256";
    private static final String CHECKSUM_PREFIX = "sha256:";
    private static final TypeReference<List<RouteConfiguration>> ROUTE_LIST_TYPE =
            new TypeReference<List<RouteConfiguration>>() {
            };

    private static final String LOCK_GATEWAY_GROUP_SQL = """
            SELECT gateway_group_id
            FROM gw_gateway_group
            WHERE gateway_group_id = :gatewayGroupId
            FOR UPDATE
            """;

    private static final String FIND_NEXT_VERSION_SQL = """
            SELECT COALESCE(MAX(config_version), 0) + 1
            FROM gw_config_release
            WHERE gateway_group_id = :gatewayGroupId
            """;

    private static final String INSERT_RELEASE_SQL = """
            INSERT INTO gw_config_release
                (gateway_group_id, config_version, checksum, snapshot_content, release_status, published_at)
            VALUES
                (:gatewayGroupId, :configVersion, :checksum, :snapshotContent, :releaseStatus, :publishedAt)
            """;

    private static final String INSERT_OUTBOX_EVENT_SQL = """
            INSERT INTO gw_event_outbox
                (event_id, aggregate_type, aggregate_id, aggregate_version, event_type, event_payload,
                 delivery_status, next_attempt_at, occurred_at)
            VALUES
                (:eventId, :aggregateType, :aggregateId, :aggregateVersion, :eventType, :eventPayload,
                 :deliveryStatus, :nextAttemptAt, :occurredAt)
            """;

    private static final String FIND_LATEST_PUBLISHED_SQL = """
            SELECT gateway_group_id,
                   config_version,
                   checksum,
                   snapshot_content,
                   published_at
            FROM gw_config_release
            WHERE gateway_group_id = :gatewayGroupId
              AND release_status = :releaseStatus
            ORDER BY config_version DESC
            LIMIT 1
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 创建配置发布 JDBC 适配器。
     *
     * @param jdbcTemplate Spring JDBC 命名参数模板
     * @param objectMapper 项目统一的 JSON 映射器
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public JdbcConfigurationReleaseRepository(NamedParameterJdbcTemplate jdbcTemplate, ObjectMapper objectMapper) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }


    @Override
    @Transactional
    public GatewayConfigurationSnapshot publish(String gatewayGroupId, List<RouteConfiguration> routes,
            Instant publishedAt) {
        String groupId = Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null").strip();
        if (groupId.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        List<RouteConfiguration> checkedRoutes = List.copyOf(Objects.requireNonNull(routes, "routes must not be null"));
        Instant checkedPublishedAt = Objects.requireNonNull(publishedAt, "publishedAt must not be null");
        MapSqlParameterSource groupParameters = new MapSqlParameterSource("gatewayGroupId", groupId);
        lockGatewayGroup(groupId, groupParameters);
        long version = findNextVersion(groupParameters);
        String snapshotContent = writeJson(checkedRoutes, "configuration release routes");
        String checksum = calculateChecksum(snapshotContent);
        insertRelease(groupId, version, checksum, snapshotContent, checkedPublishedAt);

        GatewayConfigurationPublishedEvent event = new GatewayConfigurationPublishedEvent(
                UUID.randomUUID(), groupId, version, checkedPublishedAt);
        insertOutboxEvent(event, writeJson(event, "configuration published event"));
        return new GatewayConfigurationSnapshot(groupId, version, checkedPublishedAt, checksum, checkedRoutes);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<GatewayConfigurationSnapshot> findLatestPublished(String gatewayGroupId) {
        String groupId = Objects.requireNonNull(gatewayGroupId, "gatewayGroupId must not be null").strip();
        if (groupId.isBlank()) {
            throw new IllegalArgumentException("gatewayGroupId must not be blank");
        }
        MapSqlParameterSource parameters = new MapSqlParameterSource().addValue("gatewayGroupId", groupId)
                .addValue("releaseStatus", PUBLISHED_STATUS);
        List<GatewayConfigurationSnapshot> snapshots =
                this.jdbcTemplate.query(FIND_LATEST_PUBLISHED_SQL, parameters, this::mapSnapshot);
        return snapshots.stream().findFirst();
    }

    private void lockGatewayGroup(String groupId, MapSqlParameterSource groupParameters) {
        List<String> gatewayGroupIds =
                this.jdbcTemplate.queryForList(LOCK_GATEWAY_GROUP_SQL, groupParameters, String.class);
        if (gatewayGroupIds.isEmpty()) {
            throw new GatewayGroupNotFoundException(groupId);
        }
    }

    private long findNextVersion(MapSqlParameterSource parameters) {
        Long version = this.jdbcTemplate.queryForObject(FIND_NEXT_VERSION_SQL, parameters, Long.class);
        return Objects.requireNonNull(version, "next configuration version must not be null");
    }

    private String calculateChecksum(String snapshotContent) {
        try {
            MessageDigest messageDigest = MessageDigest.getInstance(CHECKSUM_ALGORITHM);
            byte[] digest = messageDigest.digest(snapshotContent.getBytes(StandardCharsets.UTF_8));
            return CHECKSUM_PREFIX + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private void insertRelease(String groupId, long version, String checksum, String snapshotContent,
            Instant checkedPublishedAt) {
        MapSqlParameterSource parameters =
                new MapSqlParameterSource().addValue("gatewayGroupId", groupId).addValue("configVersion", version)
                        .addValue("checksum", checksum).addValue("snapshotContent", snapshotContent)
                        .addValue("releaseStatus", PUBLISHED_STATUS)
                        .addValue("publishedAt", Timestamp.from(checkedPublishedAt));
        this.jdbcTemplate.update(INSERT_RELEASE_SQL, parameters);
    }

    /**
     * 将配置发布事件保存为待投递 Outbox 记录。
     *
     * @param event        已完成版本分配的配置发布事件
     * @param eventPayload 事件 JSON 载荷
     */
    private void insertOutboxEvent(GatewayConfigurationPublishedEvent event, String eventPayload) {
        Timestamp occurredAt = Timestamp.from(event.publishedAt());
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", event.eventId().toString())
                .addValue("aggregateType", CONFIGURATION_AGGREGATE_TYPE)
                .addValue("aggregateId", event.gatewayGroupId())
                .addValue("aggregateVersion", event.version())
                .addValue("eventType", CONFIGURATION_PUBLISHED_EVENT_TYPE)
                .addValue("eventPayload", eventPayload)
                .addValue("deliveryStatus", PENDING_DELIVERY_STATUS)
                .addValue("nextAttemptAt", occurredAt)
                .addValue("occurredAt", occurredAt);
        this.jdbcTemplate.update(INSERT_OUTBOX_EVENT_SQL, parameters);
    }

    private GatewayConfigurationSnapshot mapSnapshot(ResultSet resultSet, int rowNumber) throws SQLException {
        String gatewayGroupId = resultSet.getString("gateway_group_id");
        long version = resultSet.getLong("config_version");
        String storedChecksum = resultSet.getString("checksum");
        String snapshotContent = resultSet.getString("snapshot_content");
        Instant publishedAt = resultSet.getTimestamp("published_at").toInstant();
        String actualChecksum = calculateChecksum(snapshotContent);

        if (!storedChecksum.equals(actualChecksum)) {
            throw new DataRetrievalFailureException("Configuration release checksum does not match stored content");
        }

        List<RouteConfiguration> routes = readRoutes(snapshotContent);
        return new GatewayConfigurationSnapshot(gatewayGroupId, version, publishedAt, storedChecksum, routes);
    }

    private String writeJson(Object value, String contentDescription) {
        try {
            return this.objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to serialize " + contentDescription, exception);
        }
    }

    private List<RouteConfiguration> readRoutes(String snapshotContent) {
        try {
            List<RouteConfiguration> routes = this.objectMapper.readValue(snapshotContent, ROUTE_LIST_TYPE);
            return List.copyOf(Objects.requireNonNull(routes, "routes must not be null"));
        } catch (JsonProcessingException | NullPointerException exception) {
            throw new DataRetrievalFailureException("Unable to read configuration release routes", exception);
        }
    }
}
