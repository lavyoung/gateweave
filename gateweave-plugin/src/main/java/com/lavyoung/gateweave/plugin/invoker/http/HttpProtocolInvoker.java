package com.lavyoung.gateweave.plugin.invoker.http;


import com.lavyoung.gateweave.contract.extension.InvocationRequest;
import com.lavyoung.gateweave.contract.extension.ProtocolInvoker;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletionStage;

/**
 * 使用 JDK HTTP Client 执行统一调用请求。
 *
 * <p>该适配器负责把网关调用契约转换为 HTTP 请求，并复用外部注入的
 * HttpClient 管理连接。应用层和领域层不感知 JDK HTTP 客户端类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public final class HttpProtocolInvoker implements ProtocolInvoker {

    private static final String PROTOCOL = "http";
    private static final Set<String> NON_FORWARDABLE_HEADERS = Set.of(
            "connection", "content-length", "expect", "host", "keep-alive", "proxy-authenticate",
            "proxy-authorization", "proxy-connection", "te", "trailer", "transfer-encoding", "upgrade");
    private final HttpClient httpClient;

    /**
     * 创建 HTTP 协议调用器。
     *
     * @param httpClient 负责连接复用和异步请求的 HTTP 客户端
     * @throws NullPointerException httpClient 为 null 时抛出
     */
    public HttpProtocolInvoker(HttpClient httpClient) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
    }

    @Override
    public String protocol() {
        return PROTOCOL;
    }

    @Override
    public CompletionStage<String> invoke(InvocationRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        HttpInvocationOptions options = HttpInvocationOptions.from(request.attributes());
        URI targetUri = URI.create(request.service()).resolve(request.operation());
        HttpRequest.Builder requestBuilder = HttpRequest.newBuilder().uri(targetUri).timeout(request.timeout());
        request.headers().forEach((name, value) -> {
            if (isForwardableHeader(name)) {
                requestBuilder.header(name, value);
            }
        });

        byte[] payload = request.payload();
        HttpRequest.BodyPublisher bodyPublisher = payload.length == 0
                ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofByteArray(payload);
        HttpRequest httpRequest = requestBuilder.method(options.method(), bodyPublisher).build();
        return this.httpClient.sendAsync(httpRequest, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8))
                .thenApply(HttpResponse::body);
    }

    /**
     * 判断请求头能否由网关转发给下游 HTTP 服务。
     *
     * @param name 请求头名称
     * @return 可以转发时返回 true
     */
    private boolean isForwardableHeader(String name) {
        return !NON_FORWARDABLE_HEADERS.contains(name.toLowerCase(Locale.ROOT));
    }
}
