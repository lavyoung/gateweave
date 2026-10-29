package com.lavyoung.gateweave.engine.configuration;

import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;
import com.lavyoung.gateweave.engine.application.security.BearerTokenResolver;
import com.lavyoung.gateweave.engine.application.security.GatewaySessionAuthenticationService;
import com.lavyoung.gateweave.plugin.security.jwt.JwtTokenVerifier;
import com.lavyoung.gateweave.plugin.security.jwt.JwtVerificationPolicy;
import com.lavyoung.gateweave.plugin.security.shiro.GatewayAuthenticationRealm;
import com.lavyoung.gateweave.plugin.security.shiro.ShiroGatewayAuthenticator;
import com.lavyoung.gateweave.plugin.security.shiro.StatelessShiroSecurityManagerFactory;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Base64;

/**
 * 装配网关数据面的 JWT 与 Shiro 无状态认证组件。
 *
 * <p>配置类是外部安全配置与认证插件之间的装配边界。JJWT、Shiro 及密钥转换细节
 * 不进入应用层和领域层，业务调用方只依赖 GatewayAuthenticator 契约。</p>
 *
 * @author <a href="mailto:lavyoung1325@outlook.com">lavyoung</a>
 * @version 1.0.0
 * @date 2026/10/29
 */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(GatewayJwtProperties.class)
public class GatewaySecurityConfiguration {

    /**
     * 创建负责 JWT 验签并恢复可信主体的 Shiro Realm。
     *
     * @param properties JWT 安全配置
     * @return 网关认证 Realm
     * @throws IllegalArgumentException Base64 密钥格式错误或密钥强度不符合 HS256 要求时抛出
     */
    @Bean
    public GatewayAuthenticationRealm gatewayAuthenticationRealm(GatewayJwtProperties properties) {
        byte[] verificationKeyBytes = Base64.getDecoder().decode(properties.verificationKeyBase64());
        JwtVerificationPolicy policy = new JwtVerificationPolicy(
                verificationKeyBytes, properties.issuer(), properties.audience(), properties.clockSkew(),
                properties.maximumTokenLength());
        return new GatewayAuthenticationRealm(new JwtTokenVerifier(policy));
    }

    /**
     * 创建关闭所有 Session 能力的 Shiro SecurityManager。
     *
     * @param realm 网关认证 Realm
     * @return 无状态 Shiro SecurityManager
     */
    @Bean(destroyMethod = "destroy")
    public DefaultSecurityManager gatewaySecurityManager(GatewayAuthenticationRealm realm) {
        return StatelessShiroSecurityManagerFactory.create(realm);
    }

    /**
     * 创建供网关应用层使用的统一认证扩展。
     *
     * @param securityManager 无状态 Shiro SecurityManager
     * @return 网关认证扩展
     */
    @Bean
    public GatewayAuthenticator gatewayAuthenticator(DefaultSecurityManager securityManager) {
        return new ShiroGatewayAuthenticator(securityManager);
    }

    /**
     * 创建标准 Bearer Token 解析器。
     *
     * @return Bearer Token 解析器
     */
    @Bean
    public BearerTokenResolver bearerTokenResolver() {
        return new BearerTokenResolver();
    }

    /**
     * 创建负责执行路由鉴权策略的应用服务。
     *
     * @param tokenResolver Bearer Token 解析器
     * @param authenticator 网关认证扩展
     * @return 会话鉴权应用服务
     */
    @Bean
    public GatewaySessionAuthenticationService gatewaySessionAuthenticationService(
            BearerTokenResolver tokenResolver, GatewayAuthenticator authenticator) {
        return new GatewaySessionAuthenticationService(tokenResolver, authenticator);
    }
}
