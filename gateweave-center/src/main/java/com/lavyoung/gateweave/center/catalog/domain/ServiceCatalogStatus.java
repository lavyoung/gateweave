package com.lavyoung.gateweave.center.catalog.domain;

/**
 * 定义服务目录中的服务和操作状态。
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @since 1.0
 */
public enum ServiceCatalogStatus {

    /**
     * 服务或操作可以参与路由配置和发布。
     */
    ACTIVE,

    /**
     * 服务或操作被主动禁用，不允许进入新的发布版本。
     */
    DISABLED
}
