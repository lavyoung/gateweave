package com.lavyoung.gateweave.center.configuration;

import com.lavyoung.gateweave.center.web.RequestIdFilter;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.method.HandlerTypePredicate;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Objects;

/**
 * 统一装配控制面对外 HTTP API 的 Spring MVC 规则。
 *
 * <p>所有 {@link RestController} 自动获得同一个 API 前缀，Controller 本身只维护资源路径。
 * 以后调整部署前缀时只需修改配置，不需要逐个修改 Controller。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/19
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ControlPlaneWebProperties.class)
public class ControlPlaneWebConfiguration implements WebMvcConfigurer {

    private static final String[] ALLOWED_METHODS = {HttpMethod.GET.name(),
            HttpMethod.POST.name(), HttpMethod.PUT.name(), HttpMethod.DELETE.name()};
    private static final String[] ALLOWED_HEADERS = {HttpHeaders.ACCEPT,
            HttpHeaders.ACCEPT_LANGUAGE, HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE,
            RequestIdFilter.REQUEST_ID_HEADER};

    private final ControlPlaneWebProperties properties;

    /**
     * 创建控制面 Web 配置。
     *
     * @param properties 控制面 Web 配置属性
     * @throws NullPointerException properties 为 null 时抛出
     */
    public ControlPlaneWebConfiguration(ControlPlaneWebProperties properties) {
        this.properties = Objects.requireNonNull(properties, "properties must not be null");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        configurer.addPathPrefix(this.properties.apiPrefix(), HandlerTypePredicate.forAnnotation(RestController.class));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping(this.properties.apiPrefix() + "/**")
                .allowedOrigins(this.properties.allowedOrigins().toArray(String[]::new))
                .allowedMethods(ALLOWED_METHODS)
                .allowedHeaders(ALLOWED_HEADERS)
                .exposedHeaders(RequestIdFilter.REQUEST_ID_HEADER)
                .allowCredentials(false)
                .maxAge(this.properties.corsMaxAge().toSeconds());
    }
}
