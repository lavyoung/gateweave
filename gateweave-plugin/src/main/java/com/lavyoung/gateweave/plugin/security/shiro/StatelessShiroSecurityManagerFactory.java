package com.lavyoung.gateweave.plugin.security.shiro;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.DefaultSessionManager;

import java.util.Objects;

/**
 * 负责创建适用于网关请求的无状态 Shiro SecurityManager。
 *
 * <p>工厂统一关闭 Subject 的 Session 状态存储以及 Session 校验调度器，
 * 避免调用方遗漏配置，也避免无状态网关启动无意义的后台会话任务。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
public final class StatelessShiroSecurityManagerFactory {

    private StatelessShiroSecurityManagerFactory() {
    }

    /**
     * 使用指定 Realm 创建无状态 SecurityManager。
     *
     * @param realm Shiro 认证 Realm
     * @return 已关闭 Session 存储和校验调度器的 SecurityManager
     * @throws NullPointerException realm 为 null 时抛出
     */
    public static DefaultSecurityManager create(Realm realm) {
        Objects.requireNonNull(realm, "realm must not be null");
        DefaultSessionStorageEvaluator storageEvaluator = new DefaultSessionStorageEvaluator();
        storageEvaluator.setSessionStorageEnabled(false);

        DefaultSubjectDAO subjectDAO = new DefaultSubjectDAO();
        subjectDAO.setSessionStorageEvaluator(storageEvaluator);

        DefaultSessionManager sessionManager = new DefaultSessionManager();
        sessionManager.setSessionValidationSchedulerEnabled(false);

        DefaultSecurityManager securityManager = new DefaultSecurityManager(realm);
        securityManager.setSubjectDAO(subjectDAO);
        securityManager.setSessionManager(sessionManager);
        return securityManager;
    }
}
