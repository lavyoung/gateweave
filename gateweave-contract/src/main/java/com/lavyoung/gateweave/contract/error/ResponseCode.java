package com.lavyoung.gateweave.contract.error;

/**
 * 定义跨进程 API 使用的稳定响应码契约。
 *
 * <p>响应码负责程序判断，国际化资源键负责查找本地化文案，
 * 默认文案只在资源缺失或解析失败时兜底。</p>
 *
 * <p>该契约不包含 HTTP 状态码。相同业务错误可能由 HTTP、消息或其他协议传输，
 * HTTP 状态映射应由 Web 入站适配器负责。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/31
 */
public interface ResponseCode {

    /**
     * 获取全局唯一且保持稳定的响应码。
     *
     * @return 稳定响应码
     */
    String code();

    /**
     * 获取国际化资源键。
     *
     * @return 国际化资源键
     */
    String messageKey();

    /**
     * 获取国际化资源不可用时使用的默认文案。
     *
     * @return 默认响应文案
     */
    String defaultMessage();
}
