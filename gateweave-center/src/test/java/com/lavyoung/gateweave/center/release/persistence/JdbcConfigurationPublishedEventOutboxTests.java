package com.lavyoung.gateweave.center.release.persistence;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import com.lavyoung.gateweave.center.GateWeaveCenterApplication;
import com.lavyoung.gateweave.center.release.application.ConfigurationPublishedEventOutbox;
import com.lavyoung.gateweave.center.release.application.ConfigurationReleaseRepository;
import com.lavyoung.gateweave.center.release.configuration.ConfigurationEventDeliveryProperties;
import com.lavyoung.gateweave.contract.configuration.GatewayConfigurationPublishedEvent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(classes = GateWeaveCenterApplication.class, properties = {
        "spring.datasource.url=jdbc:h2:mem:configuration-event-outbox;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.username=sa", "spring.datasource.password=",
        "gateweave.configuration-events.delivery.batch-size=2",
        "gateweave.configuration-events.delivery.lock-duration=30s",
        "gateweave.configuration-events.delivery.retry-delay=5s",
        "gateweave.configuration-events.delivery.max-attempts=2"})
class JdbcConfigurationPublishedEventOutboxTests {

    private static final Instant FIRST_OCCURRED_AT = Instant.parse("2026-11-15T15:00:00Z");

    private final ConfigurationPublishedEventOutbox outbox;
    private final ConfigurationReleaseRepository releaseRepository;
    private final JdbcTemplate jdbcTemplate;
    private final NamedParameterJdbcTemplate namedJdbcTemplate;
    private final ConfigurationEventDeliveryProperties properties;

    @Autowired
    JdbcConfigurationPublishedEventOutboxTests(ConfigurationPublishedEventOutbox outbox,
            ConfigurationReleaseRepository releaseRepository, JdbcTemplate jdbcTemplate,
            NamedParameterJdbcTemplate namedJdbcTemplate, ConfigurationEventDeliveryProperties properties) {
        this.outbox = outbox;
        this.releaseRepository = releaseRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.namedJdbcTemplate = namedJdbcTemplate;
        this.properties = properties;
    }

    @BeforeEach
    void deletePublishedData() {
        this.jdbcTemplate.update("DELETE FROM gw_event_outbox");
        this.jdbcTemplate.update("DELETE FROM gw_config_release");
    }

    @Test
    void shouldClaimDueEventsUpToConfiguredBatchSize() {
        // Given
        publishAt(FIRST_OCCURRED_AT);
        publishAt(FIRST_OCCURRED_AT.plusSeconds(1));
        publishAt(FIRST_OCCURRED_AT.plusSeconds(2));

        // When
        List<GatewayConfigurationPublishedEvent> events = this.outbox.claimDue(FIRST_OCCURRED_AT.plusSeconds(2));

        // Then
        assertThat(events).extracting(GatewayConfigurationPublishedEvent::version).containsExactly(1L, 2L);
        assertThat(countByStatus("PUBLISHING")).isEqualTo(2);
        assertThat(countByStatus("PENDING")).isEqualTo(1);
        assertThat(this.jdbcTemplate.queryForList(
                "SELECT delivery_attempts FROM gw_event_outbox WHERE delivery_status = 'PUBLISHING'",
                Integer.class)).containsOnly(1);
        assertThat(this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gw_event_outbox WHERE locked_by IS NOT NULL AND locked_until IS NOT NULL",
                Integer.class)).isEqualTo(2);
    }

    @Test
    void shouldReclaimPublishingEventOnlyAfterItsLockExpires() {
        // Given
        publishAt(FIRST_OCCURRED_AT);
        GatewayConfigurationPublishedEvent event = this.outbox.claimDue(FIRST_OCCURRED_AT).get(0);

        // When
        List<GatewayConfigurationPublishedEvent> beforeExpiration =
                this.outbox.claimDue(FIRST_OCCURRED_AT.plusSeconds(29));
        List<GatewayConfigurationPublishedEvent> afterExpiration =
                this.outbox.claimDue(FIRST_OCCURRED_AT.plusSeconds(30));

        // Then
        assertThat(beforeExpiration).isEmpty();
        assertThat(afterExpiration).containsExactly(event);
        assertThat(deliveryAttempts(event.eventId())).isEqualTo(2);
    }

    @Test
    void shouldMarkOwnedEventAsPublished() {
        // Given
        publishAt(FIRST_OCCURRED_AT);
        GatewayConfigurationPublishedEvent event = this.outbox.claimDue(FIRST_OCCURRED_AT).get(0);
        Instant deliveredAt = FIRST_OCCURRED_AT.plusSeconds(1);

        // When
        this.outbox.markPublished(event.eventId(), deliveredAt);

        // Then
        assertThat(deliveryStatus(event.eventId())).isEqualTo("PUBLISHED");
        assertThat(this.jdbcTemplate.queryForObject(
                "SELECT delivered_at FROM gw_event_outbox WHERE event_id = ?", Instant.class,
                event.eventId().toString())).isEqualTo(deliveredAt);
        assertThat(this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gw_event_outbox WHERE event_id = ? AND locked_by IS NULL "
                        + "AND locked_until IS NULL",
                Integer.class, event.eventId().toString())).isEqualTo(1);
        assertThatThrownBy(() -> this.outbox.markPublished(event.eventId(), deliveredAt))
                .isInstanceOf(OptimisticLockingFailureException.class);
    }

    @Test
    void shouldRetryFailedEventAndMoveItToDeadAfterMaximumAttempts() {
        // Given
        publishAt(FIRST_OCCURRED_AT);
        GatewayConfigurationPublishedEvent firstAttempt = this.outbox.claimDue(FIRST_OCCURRED_AT).get(0);

        // When
        this.outbox.markFailed(firstAttempt.eventId(), FIRST_OCCURRED_AT, "redis unavailable");
        List<GatewayConfigurationPublishedEvent> beforeRetry =
                this.outbox.claimDue(FIRST_OCCURRED_AT.plusSeconds(4));
        GatewayConfigurationPublishedEvent secondAttempt =
                this.outbox.claimDue(FIRST_OCCURRED_AT.plusSeconds(5)).get(0);
        this.outbox.markFailed(secondAttempt.eventId(), FIRST_OCCURRED_AT.plusSeconds(5), "redis unavailable");

        // Then
        assertThat(beforeRetry).isEmpty();
        assertThat(deliveryStatus(firstAttempt.eventId())).isEqualTo("DEAD");
        assertThat(deliveryAttempts(firstAttempt.eventId())).isEqualTo(2);
        assertThat(this.jdbcTemplate.queryForObject(
                "SELECT last_error FROM gw_event_outbox WHERE event_id = ?", String.class,
                firstAttempt.eventId().toString())).isEqualTo("redis unavailable");
        assertThat(this.outbox.claimDue(FIRST_OCCURRED_AT.plus(Duration.ofHours(1)))).isEmpty();
    }

    @Test
    void shouldRejectStateChangeFromWorkerThatDoesNotOwnEvent() {
        // Given
        publishAt(FIRST_OCCURRED_AT);
        GatewayConfigurationPublishedEvent event = this.outbox.claimDue(FIRST_OCCURRED_AT).get(0);
        ConfigurationPublishedEventOutbox anotherWorker =
                new JdbcConfigurationPublishedEventOutbox(this.namedJdbcTemplate, this.properties);

        // When & Then
        assertThatThrownBy(() -> anotherWorker.markPublished(event.eventId(), FIRST_OCCURRED_AT.plusSeconds(1)))
                .isInstanceOf(OptimisticLockingFailureException.class)
                .hasMessageContaining(event.eventId().toString());
        assertThat(deliveryStatus(event.eventId())).isEqualTo("PUBLISHING");
    }

    private void publishAt(Instant publishedAt) {
        this.releaseRepository.publish("default", List.of(), publishedAt);
    }

    private int countByStatus(String status) {
        return this.jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM gw_event_outbox WHERE delivery_status = ?", Integer.class, status);
    }

    private int deliveryAttempts(UUID eventId) {
        return this.jdbcTemplate.queryForObject(
                "SELECT delivery_attempts FROM gw_event_outbox WHERE event_id = ?", Integer.class,
                eventId.toString());
    }

    private String deliveryStatus(UUID eventId) {
        return this.jdbcTemplate.queryForObject(
                "SELECT delivery_status FROM gw_event_outbox WHERE event_id = ?", String.class,
                eventId.toString());
    }
}
