package com.lavyoung.gateweave.center.adapter.in.web;

import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayGroupNotFoundException;
import com.lavyoung.gateweave.center.gateway.domain.exception.GatewayNodeAddressConflictException;
import com.lavyoung.gateweave.contract.error.CommonResponseCode;
import com.lavyoung.gateweave.contract.error.GatewayResponseCode;
import com.lavyoung.gateweave.contract.error.ResponseCode;
import com.lavyoung.gateweave.contract.web.ErrorDetails;
import com.lavyoung.gateweave.contract.web.Result;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Objects;

/**
 * 将控制面 Web 异常统一转换为稳定且可国际化的 HTTP 响应。
 *
 * <p>异常堆栈只保留在服务端日志。客户端仅获得稳定响应码、本地化文案、请求路径和请求追踪标识，
 * 不暴露 SQL、内部地址或底层异常类型。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final LocalizedResultFactory resultFactory;

    /**
     * 创建全局异常处理器。
     *
     * @param resultFactory 本地化结果工厂
     * @throws NullPointerException resultFactory 为 null 时抛出
     */
    public GlobalExceptionHandler(LocalizedResultFactory resultFactory) {
        this.resultFactory = Objects.requireNonNull(resultFactory, "resultFactory must not be null");
    }

    /**
     * 处理网关节点注册时指定的分组不存在。
     *
     * @param exception 网关分组不存在异常
     * @param request   当前 HTTP 请求
     * @return HTTP 404 网关分组不存在响应
     */
    @ExceptionHandler(GatewayGroupNotFoundException.class)
    public ResponseEntity<Result<ErrorDetails>> handleGatewayGroupNotFound(
            GatewayGroupNotFoundException exception, HttpServletRequest request) {
        return this.failure(
                HttpStatus.NOT_FOUND,
                GatewayResponseCode.GATEWAY_GROUP_NOT_FOUND,
                request,
                exception.gatewayGroupId());
    }

    /**
     * 处理网关节点公布地址已被占用。
     *
     * @param exception 节点地址冲突异常
     * @param request   当前 HTTP 请求
     * @return HTTP 409 节点地址冲突响应
     */
    @ExceptionHandler(GatewayNodeAddressConflictException.class)
    public ResponseEntity<Result<ErrorDetails>> handleGatewayNodeAddressConflict(
            GatewayNodeAddressConflictException exception, HttpServletRequest request) {
        return this.failure(
                HttpStatus.CONFLICT,
                GatewayResponseCode.GATEWAY_NODE_ADDRESS_CONFLICT,
                request,
                exception.advertisedAddress().toASCIIString());
    }

    /**
     * 处理请求参数约束失败。
     *
     * @param exception 参数校验异常
     * @param request   当前 HTTP 请求
     * @return HTTP 400 参数错误响应
     */
    @ExceptionHandler({
            MethodArgumentNotValidException.class,
            HandlerMethodValidationException.class,
            ConstraintViolationException.class
    })
    public ResponseEntity<Result<ErrorDetails>> handleInvalidParameter(
            Exception exception, HttpServletRequest request) {
        return this.failure(HttpStatus.BAD_REQUEST, CommonResponseCode.PARAM_INVALID, request);
    }

    /**
     * 处理请求体缺失或无法反序列化。
     *
     * @param exception 请求体读取异常
     * @param request   当前 HTTP 请求
     * @return HTTP 400 请求体错误响应
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Result<ErrorDetails>> handleUnreadableBody(
            HttpMessageNotReadableException exception, HttpServletRequest request) {
        return this.failure(HttpStatus.BAD_REQUEST, CommonResponseCode.REQUEST_BODY_INVALID, request);
    }

    /**
     * 处理没有匹配到控制器或静态资源的请求。
     *
     * @param exception 资源不存在异常
     * @param request   当前 HTTP 请求
     * @return HTTP 404 资源不存在响应
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Result<ErrorDetails>> handleResourceNotFound(
            NoResourceFoundException exception, HttpServletRequest request) {
        return this.failure(HttpStatus.NOT_FOUND, CommonResponseCode.RESOURCE_NOT_FOUND, request);
    }

    /**
     * 处理通过基础校验但违反领域约束的请求参数。
     *
     * @param exception 非法领域参数异常
     * @param request   当前 HTTP 请求
     * @return HTTP 400 参数错误响应
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<ErrorDetails>> handleInvalidArgument(
            IllegalArgumentException exception, HttpServletRequest request) {
        return this.failure(HttpStatus.BAD_REQUEST, CommonResponseCode.PARAM_INVALID, request);
    }

    /**
     * 处理未被其他处理器识别的系统异常。
     *
     * @param exception 未知系统异常
     * @param request   当前 HTTP 请求
     * @return HTTP 500 系统错误响应
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<ErrorDetails>> handleUnexpectedException(
            Exception exception, HttpServletRequest request) {
        String requestId = RequestIdFilter.requestId(request);
        LOGGER.error("Unhandled request exception requestId={} method={} path={}",
                requestId, request.getMethod(), request.getRequestURI(), exception);
        return this.failureWithRequestId(
                HttpStatus.INTERNAL_SERVER_ERROR,
                CommonResponseCode.SYSTEM_ERROR,
                request,
                requestId);
    }

    private ResponseEntity<Result<ErrorDetails>> failure(
            HttpStatus status, ResponseCode responseCode, HttpServletRequest request, Object... arguments) {
        return this.failureWithRequestId(
                status, responseCode, request, RequestIdFilter.requestId(request), arguments);
    }

    private ResponseEntity<Result<ErrorDetails>> failureWithRequestId(
            HttpStatus status, ResponseCode responseCode, HttpServletRequest request,
            String requestId, Object... arguments) {
        ErrorDetails details = new ErrorDetails(request.getRequestURI(), requestId);
        Result<ErrorDetails> result = this.resultFactory.failure(responseCode, details, arguments);
        return ResponseEntity.status(status).body(result);
    }
}
