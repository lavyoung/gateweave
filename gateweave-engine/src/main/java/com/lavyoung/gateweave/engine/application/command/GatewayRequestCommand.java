package com.lavyoung.gateweave.engine.application.command;

import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.Arrays;
import java.util.Map;
import java.util.Objects;

/**
 * 描述进入网关的、与网络框架无关的 HTTP 请求。
 *
 * <p>查询参数和表单参数分别保存，避免同名参数在协议转换阶段被静默覆盖。
 * 原始请求体继续保留，用于 JSON 参数解析和 HTTP 请求透传。</p>
 *
 * @param requestId       本次请求的唯一标识，用于日志与链路追踪
 * @param method          HTTP 请求方法
 * @param uri             请求路径及查询参数
 * @param headers         HTTP 请求头快照
 * @param queryParameters 已解析的查询参数
 * @param formParameters  已解析的表单参数
 * @param body            原始请求体
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public record GatewayRequestCommand(String requestId, String method, URI uri, HttpHeaders headers,
                                    HttpParameterValues queryParameters, HttpParameterValues formParameters,
                                    byte[] body) {

    /**
     * 校验请求属性并建立不可变快照。
     *
     * @throws NullPointerException     请求属性为 null 时抛出
     * @throws IllegalArgumentException requestId 或 method 为空白时抛出
     */
    public GatewayRequestCommand {
        Objects.requireNonNull(requestId, "requestId must not be null");
        Objects.requireNonNull(method, "method must not be null");
        Objects.requireNonNull(uri, "uri must not be null");

        if (requestId.isBlank()) {
            throw new IllegalArgumentException("requestId must not be blank");
        }
        if (method.isBlank()) {
            throw new IllegalArgumentException("method must not be blank");
        }

        Objects.requireNonNull(headers, "headers must not be null");
        Objects.requireNonNull(queryParameters, "queryParameters must not be null");
        Objects.requireNonNull(formParameters, "formParameters must not be null");
        Objects.requireNonNull(body, "body must not be null");
        body = Arrays.copyOf(body, body.length);
    }

    /**
     * 创建不包含表单参数的请求命令。
     *
     * <p>该构造器用于 GET、JSON 请求及尚未解析表单的入站适配器，
     * 表单参数使用空的不可变参数集合。</p>
     *
     * @param requestId       本次请求的唯一标识
     * @param method          HTTP 请求方法
     * @param uri             请求 URI
     * @param headers         HTTP 请求头快照
     * @param queryParameters 查询参数
     * @param body            原始请求体
     */
    public GatewayRequestCommand(String requestId, String method, URI uri, HttpHeaders headers,
            HttpParameterValues queryParameters, byte[] body) {
        this(requestId, method, uri, headers, queryParameters, new HttpParameterValues(Map.of()), body);
    }

    /**
     * 返回请求体副本，防止调用方修改内部数组。
     *
     * @return 请求体副本
     */
    @Override
    public byte[] body() {
        return Arrays.copyOf(this.body, this.body.length);
    }

}
