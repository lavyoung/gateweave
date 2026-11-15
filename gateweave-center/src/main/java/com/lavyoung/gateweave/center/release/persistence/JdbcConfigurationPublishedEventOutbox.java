package com.lavyoung.gateweave.center.release.persistence;

import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventOutbox;
import com.lavyoung.gateweave.center.release.configuration.ConfigurationEventDeliveryProperties;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * 使用关系数据库领取并更新配置发布事件。
 *
 * <p>每个适配器实例拥有独立工作者标识。领取过程使用行锁和跳过已锁定记录，
 * 支持多个控制面实例并行扫描且不会相互等待。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/15
 */
@Repository
public class JdbcConfigurationPublishedEventOutbox implements ConfigurationPublishedEventOutbox {

    private static final String PENDING_STATUS = "PENDING";
    private static final String PUBLISHING_STATUS = "PUBLISHING";
    private static final String PUBLISHED_STATUS = "PUBLISHED";

    private static final String CLAIM_DUE_SQL = """
            SELECT event_id,
                   aggregate_id,
                   aggregate_version,
                   occurred_at
            FROM gw_event_outbox
            WHERE (delivery_status = :pendingStatus AND next_attempt_at <= :claimedAt)
               OR (delivery_status = :publishingStatus AND locked_until <= :claimedAt)
            ORDER BY id
            LIMIT :batchSize
            FOR UPDATE SKIP LOCKED
            """;

    private static final String LOCK_CLAIMED_SQL = """
            UPDATE gw_event_outbox
            SET delivery_status = :publishingStatus,
                delivery_attempts = delivery_attempts + 1,
                locked_by = :workerId,
                locked_until = :lockedUntil,
                updated_at = :claimedAt
            WHERE event_id IN (:eventIds)
            """;

    private static final String MARK_PUBLISHED_SQL = """
            UPDATE gw_event_outbox
            SET delivery_status = :publishedStatus,
                delivered_at = :deliveredAt,
                locked_by = NULL,
                locked_until = NULL,
                last_error = NULL,
                updated_at = :deliveredAt
            WHERE event_id = :eventId
              AND delivery_status = :publishingStatus
              AND locked_by = :workerId
            """;

    private static final String MARK_FAILED_SQL = """
            UPDATE gw_event_outbox
            SET delivery_status =
                    CASE WHEN delivery_attempts >= :maxAttempts THEN 'DEAD' ELSE 'PENDING' END,
                next_attempt_at = :nextAttemptAt,
                locked_by = NULL,
                locked_until = NULL,
                last_error = :errorMessage,
                updated_at = :failedAt
            WHERE event_id = :eventId
              AND delivery_status = :publishingStatus
              AND locked_by = :workerId
            """;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final ConfigurationEventDeliveryProperties properties;
    private final String workerId = UUID.randomUUID().toString();

    /**
     * 创建 JDBC Outbox 适配器。
     *
     * @param jdbcTemplate Spring JDBC 命名参数模板
     * @param properties   事件领取和重试策略
     * @throws NullPointerException 任一依赖为 null 时抛出
     */
    public JdbcConfigurationPublishedEventOutbox(NamedParameterJdbcTemplate jdbcTemplate,
            ConfigurationEventDeliveryProperties properties) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "jdbcTemplate must not be null");
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    @Override
    @Transactional
    public List<GatewayConfigurationPublishedEvent> claimDue(Instant claimedAt) {
        Objects.requireNonNull(claimedAt, "claimedAt must not be null");
        MapSqlParameterSource parameterSource = new MapSqlParameterSource()
                .addValue("pendingStatus", PENDING_STATUS)
                .addValue("publishingStatus", PUBLISHING_STATUS)
                .addValue("claimedAt", Timestamp.from(claimedAt))
                .addValue("batchSize", this.properties.batchSize());
        List<GatewayConfigurationPublishedEvent> events =
                this.jdbcTemplate.query(CLAIM_DUE_SQL, parameterSource, this::mapEvent);
        if (events.isEmpty()) {
            return List.of();
        }
        List<String> eventIds = events.stream().map(event -> event.eventId().toString()).toList();
        parameterSource.addValue("eventIds", eventIds)
                .addValue("workerId", this.workerId)
                .addValue("lockedUntil", Timestamp.from(claimedAt.plus(this.properties.lockDuration())));
        int updatedCount = this.jdbcTemplate.update(LOCK_CLAIMED_SQL, parameterSource);
        if (updatedCount != events.size()) {
            throw new OptimisticLockingFailureException("Unable to lock every claimed configuration event");
        }
        return List.copyOf(events);
    }

    @Override
    public void markPublished(UUID eventId, Instant deliveredAt) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", eventId.toString())
                .addValue("workerId", this.workerId)
                .addValue("publishingStatus", PUBLISHING_STATUS)
                .addValue("publishedStatus", PUBLISHED_STATUS)
                .addValue("deliveredAt", Timestamp.from(deliveredAt));
        int updated = this.jdbcTemplate.update(MARK_PUBLISHED_SQL, parameters);
        ensureOwnedUpdate(updated, eventId);
    }

    @Override
    public void markFailed(UUID eventId, Instant failedAt, String errorMessage) {
        Objects.requireNonNull(eventId, "eventId must not be null");
        Objects.requireNonNull(failedAt, "failedAt must not be null");
        Objects.requireNonNull(errorMessage, "errorMessage must not be null");
        MapSqlParameterSource parameters = new MapSqlParameterSource()
                .addValue("eventId", eventId.toString())
                .addValue("workerId", this.workerId)
                .addValue("publishingStatus", PUBLISHING_STATUS)
                .addValue("failedAt", Timestamp.from(failedAt))
                .addValue("nextAttemptAt", Timestamp.from(failedAt.plus(this.properties.retryDelay())))
                .addValue("maxAttempts", this.properties.maxAttempts())
                .addValue("errorMessage", errorMessage);
        int updated = this.jdbcTemplate.update(MARK_FAILED_SQL, parameters);
        ensureOwnedUpdate(updated, eventId);
    }

    private GatewayConfigurationPublishedEvent mapEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new GatewayConfigurationPublishedEvent(
                UUID.fromString(resultSet.getString("event_id")),
                resultSet.getString("aggregate_id"),
                resultSet.getLong("aggregate_version"),
                resultSet.getTimestamp("occurred_at").toInstant());
    }

    private void ensureOwnedUpdate(int updatedCount, UUID eventId) {
        if (updatedCount != 1) {
            throw new OptimisticLockingFailureException(
                    "Configuration event is not owned by this worker: " + eventId);
        }
    }
}
