package com.lavyoung.gateweave.engine.application.port.in;

import java.util.concurrent.CompletionStage;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;
import com.lavyoung.gateweave.engine.application.result.GatewayResponse;

/**
 * 定义客户端请求进入网关数据面的用例边界。
 *
 * <p>入站适配器负责将网络框架对象转换为
 * {@link GatewayRequestCommand}，应用服务负责处理请求并产生
 * {@link GatewayResponse}。该端口不依赖 Netty，因此可以被其他协议适配器复用。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
@FunctionalInterface
public interface HandleGatewayRequestUseCase {

    /**
     * 异步处理一次进入网关的客户端请求。
     *
     * <p>参数校验失败可以直接抛出异常；请求处理阶段发生的错误应通过返回的
     * {@link CompletionStage} 异常完成，避免阻塞 Netty EventLoop。</p>
     *
     * @param command 与网络框架无关的请求命令
     * @return 最终网关响应的异步计算结果
     * @throws NullPointerException command 为 null 时抛出
     */
    CompletionStage<GatewayResponse> handle(GatewayRequestCommand command);
}
