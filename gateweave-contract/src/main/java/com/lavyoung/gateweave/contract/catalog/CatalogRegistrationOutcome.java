package com.lavyoung.gateweave.contract.catalog;

/**
 * 表示服务目录幂等注册的处理结果。
 *
 * <p>服务定义和服务操作共同使用该结果。注册内容与已有定义不一致时属于业务冲突，
 * 不通过增加 UPDATED 状态静默覆盖现有元数据。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/11/01
 */
public enum CatalogRegistrationOutcome {

    /**
     * 数据库中不存在对应定义，本次完成了首次创建。
     */
    CREATED,

    /**
     * 数据库中已经存在完全一致的定义，本次注册未产生数据变更。
     */
    UNCHANGED
}
