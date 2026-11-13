package com.lavyoung.gateweave.starter.configuration;

import com.lavyoung.gateweave.starter.registration.ServiceMetadataRegistry;
import com.lavyoung.gateweave.starter.registration.ServiceRegistrationClient;
import com.lavyoung.gateweave.starter.registration.ServiceRegistrationRunner;
import com.lavyoung.gateweave.starter.registration.http.HttpServiceRegistrationClient;
import com.lavyoung.gateweave.starter.registration.http.ServiceRegistrationHttpApi;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.web.client.RestClientAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.support.RestClientAdapter;
import org.springframework.web.service.invoker.HttpServiceProxyFactory;

import java.net.http.HttpClient;

/**
 * 自动装配 GateWeave 服务注册链路。
 *
 * <p>仅当应用配置控制面地址后启用。该配置负责创建专用 HTTP 客户端、
 * Spring HTTP Interface 代理、注册出站适配器和启动注册运行器。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/13
 */
@AutoConfiguration(after = {RestClientAutoConfiguration.class, GateWeaveServiceMetadataAutoConfiguration.class})
@ConditionalOnClass({RestClient.class, HttpServiceProxyFactory.class})
@ConditionalOnBean(ServiceMetadataRegistry.class)
@ConditionalOnProperty(prefix = "gateweave.registration", name = "base-uri")
@EnableConfigurationProperties(GateWeaveRegistrationProperties.class)
public class GateWeaveServiceRegistrationAutoConfiguration {

    /**
     * Starter 服务注册专用 HTTP 客户端 Bean 名称。
     */
    public static final String REGISTRATION_HTTP_CLIENT_BEAN_NAME = "gateWeaveRegistrationHttpClient";

    /**
     * 创建服务注册专用的 JDK HTTP 客户端。
     *
     * <p>该客户端负责连接复用，并禁止注册请求自动跟随重定向。
     * 单次请求读取超时由请求工厂配置。</p>
     *
     * @param properties 服务注册连接配置
     * @return 服务注册 HTTP 客户端
     */
    @Bean(name = REGISTRATION_HTTP_CLIENT_BEAN_NAME)
    @ConditionalOnMissingBean(name = REGISTRATION_HTTP_CLIENT_BEAN_NAME)
    public HttpClient gateWeaveRegistrationHttpClient(GateWeaveRegistrationProperties properties) {
        return HttpClient.newBuilder().connectTimeout(properties.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER).version(HttpClient.Version.HTTP_1_1).build();
    }

    /**
     * 创建由 Spring HTTP Interface 实现的服务注册代理。
     *
     * <p>HTTP 错误响应不在底层直接抛出，由注册适配器统一校验 HTTP 状态、
     * 返回契约和注册结果。</p>
     *
     * @param restClientBuilder Boot 管理的 RestClient 构建器
     * @param httpClient        服务注册专用 HTTP 客户端
     * @param properties        服务注册连接配置
     * @return 服务注册 HTTP 代理
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceRegistrationHttpApi serviceRegistrationHttpApi(RestClient.Builder restClientBuilder,
            @Qualifier(REGISTRATION_HTTP_CLIENT_BEAN_NAME) HttpClient httpClient,
            GateWeaveRegistrationProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.requestTimeout());

        RestClient restClient = restClientBuilder.baseUrl(properties.baseUri().toString())
                .requestFactory(requestFactory).defaultStatusHandler(HttpStatusCode::isError, (request, response) -> {
                    // HTTP 状态与统一响应契约由 HttpServiceRegistrationClient 一并校验。
                }).build();
        HttpServiceProxyFactory proxyFactory =
                HttpServiceProxyFactory.builderFor(RestClientAdapter.create(restClient)).build();
        return proxyFactory.createClient(ServiceRegistrationHttpApi.class);
    }

    /**
     * 创建服务注册出站端口的 HTTP 实现。
     *
     * @param httpApi Spring 生成的服务注册 HTTP 代理
     * @return 服务注册出站端口
     */
    @Bean
    @ConditionalOnMissingBean(ServiceRegistrationClient.class)
    public ServiceRegistrationClient serviceRegistrationClient(ServiceRegistrationHttpApi httpApi) {
        return new HttpServiceRegistrationClient(httpApi);
    }

    /**
     * 创建应用启动阶段的服务注册运行器。
     *
     * @param metadataRegistry  当前应用采集到的服务元数据注册表
     * @param registrationClient 服务注册出站端口
     * @return 服务注册运行器
     */
    @Bean
    @ConditionalOnMissingBean
    public ServiceRegistrationRunner serviceRegistrationRunner(ServiceMetadataRegistry metadataRegistry,
            ServiceRegistrationClient registrationClient) {
        return new ServiceRegistrationRunner(metadataRegistry, registrationClient);
    }
}
