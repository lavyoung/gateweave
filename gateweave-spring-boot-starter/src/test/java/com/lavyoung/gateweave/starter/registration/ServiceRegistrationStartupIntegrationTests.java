package com.lavyoung.gateweave.starter.registration;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import com.lavyoung.gateweave.starter.annotation.GateWeaveOperation;
import com.lavyoung.gateweave.starter.annotation.GateWeaveService;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

class ServiceRegistrationStartupIntegrationTests {

    @Test
    void shouldCollectAndRegisterAnnotatedServiceWhenApplicationStarts() throws IOException {
        // Given
        AtomicReference<String> requestMethod = new AtomicReference<>();
        AtomicReference<String> requestPath = new AtomicReference<>();
        AtomicReference<String> requestBody = new AtomicReference<>();
        HttpServer controlPlane = createControlPlaneServer(requestMethod, requestPath, requestBody);
        controlPlane.start();

        try {
            int controlPlanePort = controlPlane.getAddress().getPort();

            // When
            try (ConfigurableApplicationContext context = new SpringApplicationBuilder(TestApplication.class)
                    .web(WebApplicationType.NONE)
                    .properties("spring.application.name=order-provider",
                            "gateweave.registration.base-uri=http://127.0.0.1:" + controlPlanePort,
                            "spring.main.banner-mode=off")
                    .run()) {
                assertThat(context).isNotNull();
            }

            // Then
            assertThat(requestMethod).hasValue("POST");
            assertThat(requestPath).hasValue("/api/v1/catalog/services");
            assertThat(requestBody.get()).contains(
                    "\"serviceId\":\"order-service\"",
                    "\"applicationName\":\"order-provider\"",
                    "\"protocol\":\"dubbo\"",
                    "\"operationId\":\"find-order\"",
                    "\"operationName\":\"findOrder\"",
                    "\"name\":\"orderId\"",
                    "\"typeName\":\"java.lang.String\"");
        } finally {
            controlPlane.stop(0);
        }
    }

    private static HttpServer createControlPlaneServer(AtomicReference<String> requestMethod,
            AtomicReference<String> requestPath, AtomicReference<String> requestBody) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/catalog/services", exchange -> {
            requestMethod.set(exchange.getRequestMethod());
            requestPath.set(exchange.getRequestURI().getPath());
            requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            writeCreatedResponse(exchange);
        });
        return server;
    }

    private static void writeCreatedResponse(HttpExchange exchange) throws IOException {
        byte[] responseBody = """
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Success",
                  "data": {
                    "serviceId": "order-service",
                    "outcome": "CREATED"
                  }
                }
                """.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(201, responseBody.length);
        exchange.getResponseBody().write(responseBody);
        exchange.close();
    }

    @Configuration(proxyBeanMethods = false)
    @EnableAutoConfiguration
    static class TestApplication {

        @Bean
        OrderService orderService() {
            return new OrderServiceImpl();
        }
    }

    @GateWeaveService(serviceId = "order-service", namespace = "production", version = "1.0.0")
    private interface OrderService {

        @GateWeaveOperation(operationId = "find-order")
        String findOrder(String orderId);
    }

    private static final class OrderServiceImpl implements OrderService {

        @Override
        public String findOrder(String orderId) {
            return orderId;
        }
    }
}
