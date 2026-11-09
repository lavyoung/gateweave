package com.lavyoung.gateweave.engine.adapter.out.controlplane;

import com.lavyoung.gateweave.contract.gateway.GatewayNodeHeartbeatRequest;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class ControlPlaneHttpApiTests {

    private static final UUID LEASE_ID = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @Test
    void shouldSendLeaseRenewalThroughConfiguredHttpContract() throws Exception {
        // Given
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/gateway-nodes/node-1/lease", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            exchange.sendResponseHeaders(HttpStatus.NO_CONTENT.value(), -1);
            exchange.close();
        });
        server.start();

        try {
            RestClient restClient = RestClient.builder()
                    .baseUrl("http://127.0.0.1:" + server.getAddress().getPort())
                    .build();
            ControlPlaneHttpApi httpApi = HttpServiceProxyFactory
                    .builderFor(RestClientAdapter.create(restClient))
                    .build()
                    .createClient(ControlPlaneHttpApi.class);

            // When
            ResponseEntity<Void> response =
                    httpApi.renewNodeLease("node-1", new GatewayNodeHeartbeatRequest(LEASE_ID, 7));

            // Then
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
            assertThat(requestMethod.get()).isEqualTo("PUT");
            assertThat(requestPath.get()).isEqualTo("/api/v1/gateway-nodes/node-1/lease");
            assertThat(requestBody.get()).isEqualTo(
                    "{\"leaseId\":\"00000000-0000-0000-0000-000000000001\",\"currentConfigVersion\":7}");
        } finally {
            server.stop(0);
        }
    }
}
