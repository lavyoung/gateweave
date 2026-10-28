package com.lavyoung.gateweave.engine.adapter.in.netty;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.command.HttpParameterValues;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 将 Netty HTTP 请求转换为网关应用层命令。
 *
 * <p>该类是 Netty 入站适配器的防腐转换边界，负责隔离 Netty 请求、
 * 请求头、查询参数、表单参数和 ByteBuf，避免网络框架类型进入应用层。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public final class NettyGatewayRequestMapper {
    private static final String REQUEST_ID_HEADER = "X-Request-Id";

    /**
     * 将完整的 Netty HTTP 请求转换为应用层命令。
     *
     * @param request 已聚合的 Netty HTTP 请求
     * @return 与 Netty 无关的网关请求命令
     * @throws NullPointerException     request 为 null 时抛出
     * @throws IllegalArgumentException URI、查询参数或表单参数不合法时抛出
     */
    public GatewayRequestCommand map(FullHttpRequest request) {
        Objects.requireNonNull(request, "request must not be null");
        QueryStringDecoder queryDecoder = new QueryStringDecoder(request.uri());
        ByteBuf content = request.content();

        byte[] body = ByteBufUtil.getBytes(content, content.readerIndex(), content.readableBytes(), true);
        HttpParameterValues queryParameters = new HttpParameterValues(queryDecoder.parameters());
        HttpParameterValues formParameters = mapFormParameters(request);
        return new GatewayRequestCommand(resolveRequestId(request), request.method().name(), URI.create(request.uri()),
                mapHeaders(request.headers()), queryParameters, formParameters, body);
    }

    /**
     * 解析 URL 编码表单或 multipart 表单中的文本字段。
     *
     * <p>解码器管理临时内存和文件资源，使用完成后必须调用 destroy。
     * 当前章节只支持文本字段，文件上传不能静默忽略。</p>
     *
     * @param request Netty 完整请求
     * @return 不包含 Netty 类型的表单参数快照
     * @throws IllegalArgumentException 表单包含文件或字段内容读取失败时抛出
     */
    private static HttpParameterValues mapFormParameters(FullHttpRequest request) {
        if (!isFormRequest(request)) {
            return new HttpParameterValues(Map.of());
        }
        HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
        try {
            Map<String, List<String>> valuesByName = new LinkedHashMap<>();
            for (InterfaceHttpData data : decoder.getBodyHttpDatas()) {
                if (!(data instanceof Attribute attribute)) {
                    // todo  未来支持
                    throw new IllegalArgumentException("file upload form fields are not supported");
                }
                valuesByName.computeIfAbsent(attribute.getName(), ignore -> new ArrayList<>())
                        .add(readAttributeValue(attribute));
            }
            return new HttpParameterValues(valuesByName);
        } finally {
            decoder.destroy();
        }
    }

    /**
     * 判断请求是否携带受支持的表单请求体。
     *
     * @param request Netty HTTP 请求
     * @return POST URL 编码表单或 multipart 表单返回 true
     */
    private static boolean isFormRequest(FullHttpRequest request) {
        if (!HttpMethod.POST.equals(request.method())) {
            return false;
        }
        CharSequence mimeType = HttpUtil.getMimeType(request);
        if (mimeType == null) {
            return false;
        }
        boolean urlEncoded = HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED.contentEqualsIgnoreCase(mimeType);
        boolean multipart = HttpHeaderValues.MULTIPART_FORM_DATA.contentEqualsIgnoreCase(mimeType);
        return urlEncoded || multipart;
    }

    /**
     * 读取 Netty 表单字段内容并转换异常边界。
     *
     * @param attribute Netty 表单字段
     * @return 字段文本值
     * @throws IllegalArgumentException 字段内容读取失败时抛出
     */
    private static String readAttributeValue(Attribute attribute) {
        try {
            return attribute.getValue();
        } catch (IOException e) {
            throw new IllegalArgumentException(
                    "failed to read form parameter: %s".formatted(attribute.getName()), e);
        }
    }

    /**
     * 获取上游请求标识；上游未提供时生成新标识。
     *
     * @param request Netty HTTP 请求
     * @return 非空白的请求标识
     */
    private static String resolveRequestId(FullHttpRequest request) {
        String requestId = request.headers().get(REQUEST_ID_HEADER);
        return requestId == null || requestId.isBlank() ? UUID.randomUUID().toString() : requestId;
    }

    /**
     * 将 Netty 请求头转换为 JDK 不可变请求头。
     *
     * @param nettyHeaders Netty 请求头
     * @return JDK 不可变请求头
     */
    private static HttpHeaders mapHeaders(io.netty.handler.codec.http.HttpHeaders nettyHeaders) {
        Map<String, List<String>> valuesByName = nettyHeaders.names().stream().collect(
                Collectors.toUnmodifiableMap(name -> name, name -> List.copyOf(nettyHeaders.getAll(name))));
        return HttpHeaders.of(valuesByName, (n, v) -> true);
    }
}
