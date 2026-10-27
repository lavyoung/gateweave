package com.lavyoung.gateweave.engine.application.session;

import com.lavyoung.gateweave.engine.application.command.GatewayRequestCommand;

import java.util.Optional;

/**
 * 定义根据网关请求打开一次应用会话的工厂。
 *
 * <p>工厂负责为请求查找匹配路由并建立会话快照。
 * 路由未命中是正常业务结果，因此使用 Optional 表达，而不是抛出异常。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/27
 */
public interface GatewaySessionFactory {

    /**
     * 根据当前请求查找路由并打开会话。
     *
     * @param command 当前网关请求命令
     * @return 路由命中时返回会话，否则返回空
     * @throws NullPointerException command 为 null 时抛出
     */
    Optional<GatewaySession> openSession(GatewayRequestCommand command);
}
