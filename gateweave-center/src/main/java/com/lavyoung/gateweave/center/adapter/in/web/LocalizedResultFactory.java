package com.lavyoung.gateweave.center.adapter.in.web;

import com.lavyoung.gateweave.contract.error.CommonResponseCode;
import com.lavyoung.gateweave.contract.error.ResponseCode;
import com.lavyoung.gateweave.contract.web.ErrorDetails;
import com.lavyoung.gateweave.contract.web.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Objects;

/**
 * 根据当前请求语言环境创建统一 HTTP 响应。
 *
 * <p>国际化解析属于 Web 入站适配边界。契约、应用和领域层只传递稳定响应码与文案参数，
 * 不依赖 Spring 的 {@link MessageSource} 或线程绑定的语言环境。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
@Component
public class LocalizedResultFactory {

    private final MessageSource messageSource;

    /**
     * 创建本地化结果工厂。
     *
     * @param messageSource Spring 国际化消息源
     * @throws NullPointerException messageSource 为 null 时抛出
     */
    public LocalizedResultFactory(MessageSource messageSource) {
        this.messageSource = Objects.requireNonNull(messageSource, "messageSource must not be null");
    }

    /**
     * 使用当前请求语言创建成功结果。
     *
     * @param data 非空响应数据
     * @param <T>  响应数据类型
     * @return 本地化成功结果
     * @throws NullPointerException data 为 null 时抛出
     */
    public <T> Result<T> success(T data) {
        return Result.success(this.resolve(CommonResponseCode.SUCCESS), data);
    }

    /**
     * 根据当前请求上下文创建本地化 HTTP 失败响应。
     *
     * @param status       HTTP 状态
     * @param responseCode 稳定失败响应码
     * @param request      当前 HTTP 请求
     * @param arguments    国际化文案参数
     * @return 包含请求追踪信息的失败响应
     * @throws NullPointerException status、responseCode 或 request 为 null 时抛出
     */
    public ResponseEntity<Result<ErrorDetails>> failureResponse(
            HttpStatus status, ResponseCode responseCode, HttpServletRequest request, Object... arguments) {
        HttpStatus checkedStatus = Objects.requireNonNull(status, "status must not be null");
        HttpServletRequest checkedRequest = Objects.requireNonNull(request, "request must not be null");
        String requestId = RequestIdFilter.requestId(checkedRequest);
        ErrorDetails details = new ErrorDetails(checkedRequest.getRequestURI(), requestId);
        Result<ErrorDetails> result = this.failure(responseCode, details, arguments);
        return ResponseEntity.status(checkedStatus).body(result);
    }

    /**
     * 使用当前请求语言创建失败结果。
     *
     * @param responseCode 稳定失败响应码
     * @param data         非空错误详情
     * @param arguments    国际化文案占位参数
     * @param <T>          错误详情类型
     * @return 本地化失败结果
     * @throws NullPointerException responseCode 或 data 为 null 时抛出
     */
    public <T> Result<T> failure(ResponseCode responseCode, T data, Object... arguments) {
        ResponseCode checkedResponseCode = Objects.requireNonNull(responseCode, "responseCode must not be null");
        return Result.failure(checkedResponseCode, this.resolve(checkedResponseCode, arguments), data);
    }

    private String resolve(ResponseCode responseCode, Object... arguments) {
        Locale locale = LocaleContextHolder.getLocale();
        Object[] safeArguments = arguments == null ? new Object[0] : arguments.clone();
        return this.messageSource.getMessage(
                responseCode.messageKey(), safeArguments, responseCode.defaultMessage(), locale);
    }
}
