package com.lavyoung.gateweave.center.gateway.application;

import com.lavyoung.gateweave.center.gateway.domain.GatewayNodeStatus;
import com.lavyoung.gateweave.center.shared.pagination.PageRequest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class GatewayNodePageQueryTests {

    @Test
    void shouldNormalizeFiltersAndCopyStatuses() {
        Set<GatewayNodeStatus> statuses = new HashSet<>(Set.of(GatewayNodeStatus.ONLINE));
        PageRequest pageRequest = new PageRequest(3, 10);

        GatewayNodePageQuery query = new GatewayNodePageQuery(" node-1 ", " default ", statuses, pageRequest);
        statuses.add(GatewayNodeStatus.OFFLINE);

        assertThat(query.nodeId()).isEqualTo("node-1");
        assertThat(query.gatewayGroupId()).isEqualTo("default");
        assertThat(query.statuses()).containsExactly(GatewayNodeStatus.ONLINE);
        assertThat(query.pageRequest()).isSameAs(pageRequest);
        assertThatThrownBy(() -> query.statuses().add(GatewayNodeStatus.OFFLINE))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void shouldTreatMissingFiltersAsUnrestricted() {
        PageRequest pageRequest = new PageRequest(1, 100);
        GatewayNodePageQuery query = new GatewayNodePageQuery(null, null, null, pageRequest);

        assertThat(query.nodeId()).isEmpty();
        assertThat(query.gatewayGroupId()).isEmpty();
        assertThat(query.statuses()).isEmpty();
        assertThat(query.pageRequest()).isSameAs(pageRequest);
    }

    @Test
    void shouldRejectMissingPageRequest() {
        assertThatThrownBy(() -> new GatewayNodePageQuery("", "", Set.of(), null))
                .isInstanceOf(NullPointerException.class).hasMessage("pageRequest must not be null");
    }

    @Test
    void shouldRejectNullStatusElement() {
        Set<GatewayNodeStatus> statuses = new HashSet<>();
        statuses.add(null);

        assertThatThrownBy(() -> new GatewayNodePageQuery("", "", statuses, new PageRequest(1, 20)))
                .isInstanceOf(NullPointerException.class);
    }
}
