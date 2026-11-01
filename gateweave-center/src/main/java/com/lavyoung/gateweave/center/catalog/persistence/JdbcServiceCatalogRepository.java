package com.lavyoung.gateweave.center.catalog.persistence;

import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogStatus;
import com.lavyoung.gateweave.center.catalog.domain.ServiceDefinition;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperation;
import com.lavyoung.gateweave.center.catalog.domain.ServiceOperationParameter;
import com.lavyoung.gateweave.center.catalog.domain.ServiceCatalogRepository;
import com.lavyoung.gateweave.center.shared.exception.BusinessException;
import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.error.CatalogResponseCode;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataRetrievalFailureException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * 使用 Spring JDBC 原子、幂等地持久化服务目录聚合。
 *
 * <p>首次注册在同一事务中写入服务、操作和参数。唯一键冲突时，插入事务先回滚，
 * 再读取数据库中的完整聚合并判断本次请求是幂等重试还是定义冲突。</p>
 *
 * <p>现有聚合通过一条连接查询恢复，禁止为每个操作单独查询参数。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
@Repository
public class JdbcServiceCatalogRepository implements ServiceCatalogRepository {

    private static final String INSERT_SERVICE_SQL = """
            INSERT INTO gw_service
                (service_id, application_name, protocol, namespace, service_version, status, revision)
            VALUES
                (:serviceId, :applicationName, :protocol, :namespace, :version, :status, :revision)
            """;

    private static final String INSERT_OPERATION_SQL = """
            INSERT INTO gw_service_operation
                (operation_id, service_id, interface_name, operation_name, status, revision)
            VALUES
                (:operationId, :serviceId, :interfaceName, :operationName, :status, :revision)
            """;

    private static final String INSERT_PARAMETER_SQL = """
            INSERT INTO gw_service_operation_parameter
                (operation_id, parameter_index, parameter_name, type_name)
            VALUES
                (:operationId, :position, :name, :typeName)
            """;

    private static final String FIND_DEFINITION_SQL = """
            SELECT service.service_id,
                   service.application_name,
                   service.protocol,
                   service.namespace,
                   service.service_version,
                   service.status AS service_status,
                   service.revision AS service_revision,
                   operation.operation_id,
                   operation.interface_name,
                   operation.operation_name,
                   operation.status AS operation_status,
                   operation.revision AS operation_revision,
                   parameter.parameter_index,
                   parameter.parameter_name,
                   parameter.type_name
            FROM gw_service service
            LEFT JOIN gw_service_operation operation
                ON operation.service_id = service.service_id
            LEFT JOIN gw_service_operation_parameter parameter
                ON parameter.operation_id = operation.operation_id
            WHERE service.service_id = :serviceId
            ORDER BY operation.id, parameter.parameter_index
            """;

    private static final String FIND_CONFLICTING_OPERATION_SQL = """
            SELECT operation_id
            FROM gw_service_operation
            WHERE operation_id IN (:operationIds)
            ORDER BY id
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;

    /**
     * 创建服务目录 JDBC 持久化适配器。
     *
     * @param jdbcTemplate       Spring JDBC 命名参数模板
     * @param transactionManager Spring 事务管理器
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public JdbcServiceCatalogRepository(
            NamedParameterJdbcTemplate jdbcTemplate, PlatformTransactionManager transactionManager) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.transactionTemplate = new TransactionTemplate(
                Objects.requireNonNull(transactionManager, "transactionManager must not be null"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public CatalogRegistrationOutcome register(ServiceDefinition serviceDefinition) {
        ServiceDefinition checkedDefinition = Objects.requireNonNull(
                serviceDefinition, "serviceDefinition must not be null");
        try {
            return insertDefinition(checkedDefinition);
        } catch (DuplicateKeyException exception) {
            return resolveDuplicate(checkedDefinition, exception);
        } catch (DataAccessException exception) {
            throw registrationFailed(exception);
        }
    }

    private CatalogRegistrationOutcome insertDefinition(ServiceDefinition definition) {
        CatalogRegistrationOutcome outcome = this.transactionTemplate.execute(status -> {
            insertService(definition);
            insertOperations(definition.operations());
            insertParameters(definition.operations());
            return CatalogRegistrationOutcome.CREATED;
        });
        if (outcome == null) {
            throw new DataRetrievalFailureException("Service registration transaction returned no outcome");
        }
        return outcome;
    }

    private void insertService(ServiceDefinition definition) {
        int affectedRows = this.jdbcTemplate.update(INSERT_SERVICE_SQL, serviceParameters(definition));
        if (affectedRows != 1) {
            throw new DataRetrievalFailureException("Service registration did not insert exactly one row");
        }
    }

    private void insertOperations(List<ServiceOperation> operations) {
        if (operations.isEmpty()) {
            return;
        }
        MapSqlParameterSource[] parameters = operations.stream()
                .map(this::operationParameters)
                .toArray(MapSqlParameterSource[]::new);
        this.jdbcTemplate.batchUpdate(INSERT_OPERATION_SQL, parameters);
    }

    private void insertParameters(List<ServiceOperation> operations) {
        List<MapSqlParameterSource> parameters = new ArrayList<>();
        for (ServiceOperation operation : operations) {
            for (ServiceOperationParameter parameter : operation.parameters()) {
                parameters.add(parameterSource(operation.operationId(), parameter));
            }
        }
        if (!parameters.isEmpty()) {
            this.jdbcTemplate.batchUpdate(
                    INSERT_PARAMETER_SQL, parameters.toArray(MapSqlParameterSource[]::new));
        }
    }

    /**
     * 在插入事务回滚后判断唯一键冲突的业务含义。
     *
     * @param requestedDefinition 本次请求定义
     * @param duplicateException  数据库唯一键异常
     * @return 已有定义完全一致时返回 UNCHANGED
     * @throws BusinessException 服务定义或操作定义冲突时抛出
     */
    private CatalogRegistrationOutcome resolveDuplicate(
            ServiceDefinition requestedDefinition, DuplicateKeyException duplicateException) {
        try {
            Optional<ServiceDefinition> existingDefinition = findByServiceId(requestedDefinition.serviceId());
            if (existingDefinition.isPresent()) {
                return compareExistingDefinition(
                        existingDefinition.orElseThrow(), requestedDefinition, duplicateException);
            }

            Optional<String> conflictingOperationId = findConflictingOperationId(requestedDefinition.operations());
            if (conflictingOperationId.isPresent()) {
                throw new BusinessException(
                        CatalogResponseCode.OPERATION_DEFINITION_CONFLICT,
                        duplicateException,
                        conflictingOperationId.orElseThrow());
            }
            throw registrationFailed(duplicateException);
        } catch (DataAccessException exception) {
            throw registrationFailed(exception);
        }
    }

    private CatalogRegistrationOutcome compareExistingDefinition(
            ServiceDefinition existingDefinition,
            ServiceDefinition requestedDefinition,
            DuplicateKeyException duplicateException) {
        if (sameDefinition(existingDefinition, requestedDefinition)) {
            return CatalogRegistrationOutcome.UNCHANGED;
        }
        if (!sameServiceMetadata(existingDefinition, requestedDefinition)) {
            throw new BusinessException(
                    CatalogResponseCode.SERVICE_DEFINITION_CONFLICT,
                    duplicateException,
                    requestedDefinition.serviceId());
        }
        throw new BusinessException(
                CatalogResponseCode.OPERATION_DEFINITION_CONFLICT,
                duplicateException,
                mismatchedOperationId(existingDefinition, requestedDefinition));
    }

    private boolean sameDefinition(ServiceDefinition first, ServiceDefinition second) {
        if (!sameServiceMetadata(first, second) || first.operations().size() != second.operations().size()) {
            return false;
        }
        Map<String, ServiceOperation> firstOperations = indexOperationsById(first.operations());
        return second.operations().stream()
                .allMatch(operation -> operation.equals(firstOperations.get(operation.operationId())));
    }

    private boolean sameServiceMetadata(ServiceDefinition first, ServiceDefinition second) {
        return first.serviceId().equals(second.serviceId())
                && first.applicationName().equals(second.applicationName())
                && first.protocol().equals(second.protocol())
                && first.namespace().equals(second.namespace())
                && first.version().equals(second.version())
                && first.status() == second.status()
                && first.revision() == second.revision();
    }

    private String mismatchedOperationId(ServiceDefinition existingDefinition, ServiceDefinition requestedDefinition) {
        Map<String, ServiceOperation> existingOperations = indexOperationsById(existingDefinition.operations());
        for (ServiceOperation requestedOperation : requestedDefinition.operations()) {
            if (!requestedOperation.equals(existingOperations.get(requestedOperation.operationId()))) {
                return requestedOperation.operationId();
            }
        }

        Map<String, ServiceOperation> requestedOperations = indexOperationsById(requestedDefinition.operations());
        return existingDefinition.operations().stream()
                .map(ServiceOperation::operationId)
                .filter(operationId -> !requestedOperations.containsKey(operationId))
                .findFirst()
                .orElse(requestedDefinition.serviceId());
    }

    private Map<String, ServiceOperation> indexOperationsById(List<ServiceOperation> operations) {
        Map<String, ServiceOperation> operationsById = new LinkedHashMap<>();
        for (ServiceOperation operation : operations) {
            operationsById.put(operation.operationId(), operation);
        }
        return operationsById;
    }

    private Optional<ServiceDefinition> findByServiceId(String serviceId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("serviceId", serviceId);
        ResultSetExtractor<Optional<ServiceDefinition>> extractor = resultSet -> readDefinition(resultSet, serviceId);
        return this.jdbcTemplate.query(FIND_DEFINITION_SQL, parameters, extractor);
    }

    /**
     * 从连接查询结果中恢复完整服务目录聚合。
     *
     * @param resultSet 查询结果
     * @param serviceId 期望读取的服务标识
     * @return 服务不存在时返回空值，否则返回完整聚合
     * @throws SQLException 数据库字段读取失败时抛出
     */
    private Optional<ServiceDefinition> readDefinition(ResultSet resultSet, String serviceId) throws SQLException {
        ServiceRow serviceRow = null;
        Map<String, OperationRow> operationRows = new LinkedHashMap<>();

        try {
            while (resultSet.next()) {
                if (serviceRow == null) {
                    serviceRow = mapServiceRow(resultSet);
                }

                String operationId = resultSet.getString("operation_id");
                if (operationId == null) {
                    continue;
                }

                OperationRow operationRow = operationRows.get(operationId);
                if (operationRow == null) {
                    operationRow = mapOperationRow(resultSet);
                    operationRows.put(operationId, operationRow);
                }

                Integer position = resultSet.getObject("parameter_index", Integer.class);
                if (position != null) {
                    operationRow.parameters().add(new ServiceOperationParameter(
                            position,
                            resultSet.getString("parameter_name"),
                            resultSet.getString("type_name")));
                }
            }

            if (serviceRow == null) {
                return Optional.empty();
            }

            List<ServiceOperation> operations = operationRows.values().stream()
                    .map(OperationRow::toDomain)
                    .toList();
            return Optional.of(serviceRow.toDomain(operations));
        } catch (IllegalArgumentException exception) {
            throw new DataRetrievalFailureException(
                    "Service catalog contains invalid persisted data: " + serviceId, exception);
        }
    }

    private ServiceRow mapServiceRow(ResultSet resultSet) throws SQLException {
        return new ServiceRow(
                resultSet.getString("service_id"),
                resultSet.getString("application_name"),
                resultSet.getString("protocol"),
                resultSet.getString("namespace"),
                resultSet.getString("service_version"),
                ServiceCatalogStatus.valueOf(resultSet.getString("service_status")),
                resultSet.getLong("service_revision"));
    }

    private OperationRow mapOperationRow(ResultSet resultSet) throws SQLException {
        return new OperationRow(
                resultSet.getString("operation_id"),
                resultSet.getString("service_id"),
                resultSet.getString("interface_name"),
                resultSet.getString("operation_name"),
                ServiceCatalogStatus.valueOf(resultSet.getString("operation_status")),
                resultSet.getLong("operation_revision"),
                new ArrayList<>());
    }

    private Optional<String> findConflictingOperationId(List<ServiceOperation> operations) {
        if (operations.isEmpty()) {
            return Optional.empty();
        }
        List<String> operationIds = operations.stream().map(ServiceOperation::operationId).toList();
        List<String> conflicts = this.jdbcTemplate.queryForList(
                FIND_CONFLICTING_OPERATION_SQL, Map.of("operationIds", operationIds), String.class);
        return conflicts.stream().findFirst();
    }

    private MapSqlParameterSource serviceParameters(ServiceDefinition definition) {
        return new MapSqlParameterSource()
                .addValue("serviceId", definition.serviceId())
                .addValue("applicationName", definition.applicationName())
                .addValue("protocol", definition.protocol())
                .addValue("namespace", definition.namespace())
                .addValue("version", definition.version())
                .addValue("status", definition.status().name())
                .addValue("revision", definition.revision());
    }

    private MapSqlParameterSource operationParameters(ServiceOperation operation) {
        return new MapSqlParameterSource()
                .addValue("operationId", operation.operationId())
                .addValue("serviceId", operation.serviceId())
                .addValue("interfaceName", operation.interfaceName())
                .addValue("operationName", operation.operationName())
                .addValue("status", operation.status().name())
                .addValue("revision", operation.revision());
    }

    private MapSqlParameterSource parameterSource(
            String operationId, ServiceOperationParameter parameter) {
        return new MapSqlParameterSource()
                .addValue("operationId", operationId)
                .addValue("position", parameter.position())
                .addValue("name", parameter.name())
                .addValue("typeName", parameter.typeName());
    }

    private BusinessException registrationFailed(Throwable cause) {
        return new BusinessException(CatalogResponseCode.REGISTRATION_FAILED, cause);
    }

    /**
     * 保存服务主表数据，等待操作连接查询完成后创建聚合。
     *
     * @param serviceId       服务标识
     * @param applicationName 应用名称
     * @param protocol        调用协议
     * @param namespace       服务命名空间
     * @param version         服务版本
     * @param status          服务状态
     * @param revision        修订号
     */
    private record ServiceRow(
            String serviceId,
            String applicationName,
            String protocol,
            String namespace,
            String version,
            ServiceCatalogStatus status,
            long revision) {

        private ServiceDefinition toDomain(List<ServiceOperation> operations) {
            return new ServiceDefinition(
                    this.serviceId,
                    this.applicationName,
                    this.protocol,
                    this.namespace,
                    this.version,
                    this.status,
                    this.revision,
                    operations);
        }
    }

    /**
     * 保存操作主表数据和当前连接查询恢复的参数集合。
     *
     * @param operationId   操作标识
     * @param serviceId     服务标识
     * @param interfaceName 接口名称
     * @param operationName 操作名称
     * @param status        操作状态
     * @param revision      修订号
     * @param parameters    按位置排列的参数集合
     */
    private record OperationRow(
            String operationId,
            String serviceId,
            String interfaceName,
            String operationName,
            ServiceCatalogStatus status,
            long revision,
            List<ServiceOperationParameter> parameters) {

        private ServiceOperation toDomain() {
            return new ServiceOperation(
                    this.operationId,
                    this.serviceId,
                    this.interfaceName,
                    this.operationName,
                    this.status,
                    this.revision,
                    this.parameters);
        }
    }
}
