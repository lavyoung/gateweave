package com.lavyoung.gateweave.contract.web;

import java.util.Objects;

import com.lavyoung.gateweave.contract.error.CommonResponseCode;
import com.lavyoung.gateweave.contract.error.ResponseCode;

/**
 * 表示跨进程 HTTP API 的统一响应结果。
 *
 * <p>成功和失败结果都必须携带非空数据。响应码用于程序判断，
 * message 只保存当前请求语言环境下已经解析完成的展示文案。</p>
 *
 * @param success 请求是否成功
 * @param code    稳定响应码
 * @param message 本地化后的响应文案
 * @param data    非空响应数据
 * @param <T>     响应数据类型
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public record Result<T>(boolean success, String code, String message, T data) {

    /**
     * 创建统一响应结果。
     *
     * @param success 请求是否成功
     * @param code    稳定响应码
     * @param message 本地化后的响应文案
     * @param data    非空响应数据
     * @throws NullPointerException     code、message 或 data 为 null 时抛出
     * @throws IllegalArgumentException code 或 message 为空白，或者成功状态与响应码不一致时抛出
     */
    public Result {
        code = requireText(code, "code");
        message = requireText(message, "message");
        data = Objects.requireNonNull(data, "data must not be null");
        boolean successCode = CommonResponseCode.SUCCESS.code().equals(code);
        if (success != successCode) {
            throw new IllegalArgumentException("success status and code must be consistent");
        }
    }

    /**
     * 使用默认成功文案创建成功结果。
     *
     * @param data 非空响应数据
     * @param <T>  响应数据类型
     * @return 成功结果
     * @throws NullPointerException data 为 null 时抛出
     */
    public static <T> Result<T> success(T data) {
        return success(CommonResponseCode.SUCCESS.defaultMessage(), data);
    }

    /**
     * 使用本地化文案创建成功结果。
     *
     * @param message 本地化后的成功文案
     * @param data    非空响应数据
     * @param <T>     响应数据类型
     * @return 成功结果
     * @throws NullPointerException     message 或 data 为 null 时抛出
     * @throws IllegalArgumentException message 为空白时抛出
     */
    public static <T> Result<T> success(String message, T data) {
        return new Result<>(true, CommonResponseCode.SUCCESS.code(), message, data);
    }

    /**
     * 使用响应码的默认文案创建失败结果。
     *
     * @param responseCode 失败响应码
     * @param data         非空错误详情
     * @param <T>          错误详情类型
     * @return 失败结果
     * @throws NullPointerException     responseCode 或 data 为 null 时抛出
     * @throws IllegalArgumentException responseCode 表示成功时抛出
     */
    public static <T> Result<T> failure(ResponseCode responseCode, T data) {
        ResponseCode checkedResponseCode = Objects.requireNonNull(responseCode, "responseCode must not be null");
        return failure(checkedResponseCode, checkedResponseCode.defaultMessage(), data);
    }

    /**
     * 使用本地化文案创建失败结果。
     *
     * @param responseCode 失败响应码
     * @param message      本地化后的失败文案
     * @param data         非空错误详情
     * @param <T>          错误详情类型
     * @return 失败结果
     * @throws NullPointerException     responseCode、message 或 data 为 null 时抛出
     * @throws IllegalArgumentException message 为空白，或者 responseCode 表示成功时抛出
     */
    public static <T> Result<T> failure(ResponseCode responseCode, String message, T data) {
        ResponseCode checkedResponseCode = Objects.requireNonNull(responseCode, "responseCode must not be null");
        return new Result<>(false, checkedResponseCode.code(), message, data);
    }

    private static String requireText(String value, String name) {
        String checkedValue = Objects.requireNonNull(value, name + " must not be null");
        if (checkedValue.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return checkedValue;
    }
}
