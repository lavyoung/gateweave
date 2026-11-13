package com.lavyoung.gateweave.starter.registration.http;

import java.util.List;

import com.lavyoung.gateweave.contract.catalog.CatalogRegistrationOutcome;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest.ServiceOperationParameterRequest;
import com.lavyoung.gateweave.contract.catalog.RegisterServiceDefinitionRequest.ServiceOperationRequest;
import com.lavyoung.gateweave.contract.catalog.ServiceDefinitionRegistrationResponse;
import com.lavyoung.gateweave.contract.web.Result;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class ServiceRegistrationHttpApiTests {

    @Test
    void shouldSendCompleteDefinitionAndReadCreatedResponse() {
        // Given
        RestClient.Builder restClientBuilder = RestClient.builder().baseUrl("http://127.0.0.1:8080");
        MockRestServiceServer server = MockRestServiceServer.bindTo(restClientBuilder).build();
        RestClient restClient = restClientBuilder.build();
        ServiceRegistrationHttpApi httpApi =
                HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build()
                .createClient(ServiceRegistrationHttpApi.class);
        server.expect(requestTo("http://127.0.0.1:8080/api/v1/catalog/services"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json(requestJson()))
                .andRespond(withStatus(HttpStatus.CREATED).contentType(MediaType.APPLICATION_JSON)
                        .body(responseJson()));

        // When
        ResponseEntity<Result<ServiceDefinitionRegistrationResponse>> response =
                httpApi.register(request());

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull().satisfies(result -> {
            assertThat(result.success()).isTrue();
            assertThat(result.data().serviceId()).isEqualTo("activity-service");
            assertThat(result.data().outcome()).isEqualTo(CatalogRegistrationOutcome.CREATED);
        });
        server.verify();
    }

    private static RegisterServiceDefinitionRequest request() {
        ServiceOperationParameterRequest parameter =
                new ServiceOperationParameterRequest(0, "name", "java.lang.String");
        ServiceOperationRequest operation =
                new ServiceOperationRequest("activity-query", "com.example.ActivityService", "query",
                        List.of(parameter));
        return new RegisterServiceDefinitionRequest("activity-service", "activity-application", "dubbo", "", "1.0.0",
                List.of(operation));
    }

    private static String requestJson() {
        return """
                {
                  "serviceId": "activity-service",
                  "applicationName": "activity-application",
                  "protocol": "dubbo",
                  "namespace": "",
                  "version": "1.0.0",
                  "operations": [
                    {
                      "operationId": "activity-query",
                      "interfaceName": "com.example.ActivityService",
                      "operationName": "query",
                      "parameters": [
                        {
                          "position": 0,
                          "name": "name",
                          "typeName": "java.lang.String"
                        }
                      ]
                    }
                  ]
                }
                """;
    }

    private static String responseJson() {
        return """
                {
                  "success": true,
                  "code": "SUCCESS",
                  "message": "Success",
                  "data": {
                    "serviceId": "activity-service",
                    "outcome": "CREATED"
                  }
                }
                """;
    }
}
