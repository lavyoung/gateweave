package com.lavyoung.gateweave.center.shared.exception;

import com.lavyoung.gateweave.contract.error.ResponseCode;

import java.io.Serial;
import java.text.MessageFormat;
import java.util.Objects;

/**
 * 表示控制面执行过程中可预期、可向调用方返回稳定错误码的业务异常。
 *
 * <p>异常只保存业务响应码和国际化文案参数，不包含 HTTP 状态等传输协议信息。
 * Web、消息或其他入站适配器可以根据自己的协议将异常转换为对外响应。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public class BusinessException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final ResponseCode responseCode;
    private final Object[] arguments;

    /**
     * 根据响应码和国际化文案参数创建业务异常。
     *
     * @param responseCode 稳定业务响应码
     * @param arguments    国际化文案格式化参数
     * @throws NullPointerException responseCode 为 null 时抛出
     */
    public BusinessException(ResponseCode responseCode, Object... arguments) {
        this(responseCode, null, arguments);
    }

    /**
     * 根据响应码、底层原因和国际化文案参数创建业务异常。
     *
     * @param responseCode 稳定业务响应码
     * @param cause        导致当前业务异常的底层原因
     * @param arguments    国际化文案格式化参数
     * @throws NullPointerException responseCode 为 null 时抛出
     */
    public BusinessException(ResponseCode responseCode, Throwable cause, Object... arguments) {
        super(formatMessage(responseCode, arguments), cause);
        this.responseCode = Objects.requireNonNull(responseCode, "responseCode must not be null");
        this.arguments = copyArguments(arguments);
    }

    /**
     * 获取异常对应的稳定业务响应码。
     *
     * @return 业务响应码
     */
    public ResponseCode responseCode() {
        return this.responseCode;
    }

    /**
     * 获取国际化文案参数快照。
     *
     * @return 文案参数副本
     */
    public Object[] arguments() {
        return this.arguments.clone();
    }

    private static String formatMessage(ResponseCode responseCode, Object[] arguments) {
        ResponseCode checkedResponseCode = Objects.requireNonNull(responseCode, "responseCode must not be null");
        return MessageFormat.format(checkedResponseCode.defaultMessage(), copyArguments(arguments));
    }

    private static Object[] copyArguments(Object[] arguments) {
        return arguments == null ? new Object[0] : arguments.clone();
    }
}
