package com.lavyoung.gateweave.engine.application.mapper;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.lavyoung.gateweave.contract.extension.GenericInvocationArguments;
import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.domain.invocation.InvocationParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.*;

/**
 * 将已经解析的 HTTP 请求参数绑定为协议无关的泛化调用参数。
 *
 * <p>GET 请求从查询参数中按名称取值；POST 请求根据 Content-Type，
 * 从 JSON 请求体或已经解析的表单字段中绑定参数。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/28
 */
public final class GatewayInvocationArgumentsResolver {

    private final ObjectMapper objectMapper;

    /**
     * 创建 HTTP 调用参数解析器。
     *
     * @param objectMapper 共享的 JSON 解析器
     * @throws NullPointerException objectMapper 为 null 时抛出
     */
    public GatewayInvocationArgumentsResolver(ObjectMapper objectMapper) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper must not be null");
    }


    /**
     * 根据 HTTP 方法和有序参数定义生成泛化调用参数。
     *
     * @param command    网关请求命令
     * @param parameters 路由声明的有序参数定义
     * @return 类型与参数值一一对应的泛化调用参数
     * @throws NullPointerException     command 或 parameters 为 null 时抛出
     * @throws IllegalArgumentException 请求参数无法与参数定义安全绑定时抛出
     */
    public GenericInvocationArguments resolve(GatewayRequestCommand command, List<InvocationParameter> parameters) {
        Objects.requireNonNull(command, "command must not be null");
        Objects.requireNonNull(parameters, "parameters must not be null");

        return switch (command.method().toUpperCase(Locale.ROOT)) {
            case "GET" -> resolveGet(command, parameters);
            case "POST" -> resolvePost(command, parameters);
            default -> throw new IllegalArgumentException(
                    "unsupported HTTP method for parameter binding: %s".formatted(command.method()));
        };
    }

    /**
     * 按参数定义顺序绑定 GET 查询参数。
     *
     * @param command    GET 请求命令
     * @param parameters 路由声明的有序参数定义
     * @return 按目标方法顺序排列的参数类型和参数值
     * @throws IllegalArgumentException 缺少参数或标量参数包含多个值时抛出
     */
    private GenericInvocationArguments resolveGet(
            GatewayRequestCommand command, List<InvocationParameter> parameters) {

        return resolveNamedParameters(command.queryParameters().values(), parameters, "query");
    }

    /**
     * 根据 Content-Type 选择 POST 参数绑定方式。
     *
     * <p>目标方法没有参数时不解析请求体，保证 HTTP 协议插件可以透传任意类型的内容。</p>
     *
     * @param command    POST 请求命令
     * @param parameters 路由声明的有序参数定义
     * @return 按目标方法顺序排列的参数类型和参数值
     * @throws IllegalArgumentException Content-Type 缺失、非法、不受支持或参数绑定失败时抛出
     */
    private GenericInvocationArguments resolvePost(
            GatewayRequestCommand command, List<InvocationParameter> parameters) {

        List<String> parameterTypes = parameters.stream().map(InvocationParameter::typeName).toList();
        if (parameterTypes.isEmpty()) {
            return new GenericInvocationArguments(parameterTypes, List.of());
        }

        String contentTypeValue = command.headers().firstValue(HttpHeaders.CONTENT_TYPE)
                .orElseThrow(() -> new IllegalArgumentException("POST request Content-Type is required"));

        MediaType contentType;
        try {
            contentType = MediaType.parseMediaType(contentTypeValue);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException(
                    "invalid POST request Content-Type: %s".formatted(contentTypeValue), exception);
        }

        if (MediaType.APPLICATION_JSON.isCompatibleWith(contentType)) {
            return resolveJsonPost(command, parameters, parameterTypes);
        }
        if (MediaType.APPLICATION_FORM_URLENCODED.isCompatibleWith(contentType)
                || MediaType.MULTIPART_FORM_DATA.isCompatibleWith(contentType)) {

            return resolveNamedParameters(command.formParameters().values(), parameters, "form");
        }

        throw new IllegalArgumentException(
                "unsupported POST request Content-Type: %s".formatted(contentTypeValue));
    }

    /**
     * 将 JSON 请求体绑定为泛化调用参数。
     *
     * <p>目标方法只有一个参数时，整个 JSON 值作为该参数；存在多个参数时，
     * JSON 请求体必须是对象，并按照参数名称取值。</p>
     *
     * @param command        POST 请求命令
     * @param parameters     路由声明的有序参数定义
     * @param parameterTypes 已提取的参数类型
     * @return JSON 请求体对应的泛化调用参数
     * @throws IllegalArgumentException 请求体为空、JSON 非法或缺少目标参数时抛出
     */
    private GenericInvocationArguments resolveJsonPost(GatewayRequestCommand command,
            List<InvocationParameter> parameters, List<String> parameterTypes) {

        Object bodyValue = readJsonBody(command.body());
        if (parameters.size() == 1) {
            return new GenericInvocationArguments(parameterTypes, Collections.singletonList(bodyValue));
        }
        if (!(bodyValue instanceof Map<?, ?> bodyValues)) {
            throw new IllegalArgumentException("POST request body must be a JSON object for multiple parameters");
        }

        List<Object> arguments = new ArrayList<>(parameters.size());
        for (InvocationParameter parameter : parameters) {
            if (!bodyValues.containsKey(parameter.name())) {
                throw new IllegalArgumentException("missing body parameter: %s".formatted(parameter.name()));
            }
            arguments.add(bodyValues.get(parameter.name()));
        }
        return new GenericInvocationArguments(parameterTypes, arguments);
    }

    /**
     * 按参数定义顺序绑定查询参数或表单参数。
     *
     * <p>数组和 Collection 参数保留全部同名值，其他参数必须恰好包含一个值。</p>
     *
     * @param valuesByName HTTP 参数名称与多值数据
     * @param parameters   路由声明的有序参数定义
     * @param source       参数来源，用于生成明确的异常信息
     * @return 按目标方法顺序排列的参数类型和参数值
     * @throws IllegalArgumentException 缺少参数或标量参数包含多个值时抛出
     */
    private GenericInvocationArguments resolveNamedParameters(Map<String, List<String>> valuesByName,
            List<InvocationParameter> parameters, String source) {

        List<String> parameterTypes = new ArrayList<>(parameters.size());
        List<Object> arguments = new ArrayList<>(parameters.size());

        for (InvocationParameter parameter : parameters) {
            List<String> parameterValues = valuesByName.get(parameter.name());
            if (parameterValues == null) {
                throw new IllegalArgumentException(
                        "missing %s parameter: %s".formatted(source, parameter.name()));
            }

            parameterTypes.add(parameter.typeName());
            if (isMultiValueType(parameter.typeName())) {
                arguments.add(List.copyOf(parameterValues));
            } else {
                if (parameterValues.size() != 1) {
                    throw new IllegalArgumentException(
                            "%s parameter must have exactly one value: %s".formatted(source, parameter.name()));
                }
                arguments.add(parameterValues.get(0));
            }
        }

        return new GenericInvocationArguments(parameterTypes, arguments);
    }

    /**
     * 使用共享 ObjectMapper 解析 JSON 请求体。
     *
     * @param body 原始请求体
     * @return JSON 对应的标量、集合、Map 或 null
     * @throws IllegalArgumentException 请求体为空或不是合法 JSON 时抛出
     */
    private Object readJsonBody(byte[] body) {
        if (body.length == 0) {
            throw new IllegalArgumentException("POST request body must not be empty");
        }
        try {
            return this.objectMapper.readValue(body, Object.class);
        } catch (IOException exception) {
            throw new IllegalArgumentException("POST request body must be valid JSON", exception);
        }
    }

    /**
     * 判断目标参数是否由数组或 Collection 承载多个值。
     *
     * <p>这里只识别网关类路径能够加载的标准类型。Provider 专属 DTO 无法加载时按标量处理，
     * 复杂对象由请求体解析链路负责。</p>
     *
     * @param typeName 目标方法参数类型名称
     * @return 数组或 Collection 类型返回 true，否则返回 false
     */
    private boolean isMultiValueType(String typeName) {
        try {
            Class<?> parameterType = Class.forName(
                    typeName, false, GatewayInvocationArgumentsResolver.class.getClassLoader());
            return parameterType.isArray() || Collection.class.isAssignableFrom(parameterType);
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }


}
