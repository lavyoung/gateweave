package com.lavyoung.gateweave.engine.configuration;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.WeakKeyException;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.lavyoung.gateweave.contract.extension.security.AuthenticatedPrincipal;
import com.lavyoung.gateweave.contract.extension.security.GatewayAuthenticator;
import com.lavyoung.gateweave.engine.application.security.BearerTokenResolver;
import com.lavyoung.gateweave.engine.application.security.GatewaySessionAuthenticationService;
import com.lavyoung.gateweave.plugin.security.shiro.GatewayAuthenticationRealm;

import static org.assertj.core.api.Assertions.assertThat;

class GatewaySecurityConfigurationTests {

    private static final String ISSUER = "gateweave-issuer";
    private static final String AUDIENCE = "gateweave-engine";
    private static final byte[] VERIFICATION_KEY_BYTES =
            "01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8);
    private static final String VERIFICATION_KEY_BASE64 = Base64.getEncoder().encodeToString(VERIFICATION_KEY_BYTES);

    @Test
    void shouldCreateSingleGatewayAuthenticatorAndStatelessShiroBeans() {
        // Given
        ApplicationContextRunner contextRunner = contextRunner(VERIFICATION_KEY_BASE64);

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBeansOfType(GatewayAuthenticator.class)).containsOnlyKeys("gatewayAuthenticator");
            assertThat(context).hasSingleBean(GatewayAuthenticationRealm.class);
            assertThat(context).hasSingleBean(DefaultSecurityManager.class);
            assertThat(context).hasSingleBean(BearerTokenResolver.class);
            assertThat(context).hasSingleBean(GatewaySessionAuthenticationService.class);

            AbstractBeanDefinition beanDefinition = (AbstractBeanDefinition) context.getBeanFactory()
                    .getBeanDefinition("gatewaySecurityManager");
            assertThat(beanDefinition.getDestroyMethodNames()).containsExactly("destroy");
        });
    }

    @Test
    void shouldAuthenticateValidJwtThroughSpringManagedChain() {
        // Given
        ApplicationContextRunner contextRunner = contextRunner(VERIFICATION_KEY_BASE64);
        SecretKey key = new SecretKeySpec(VERIFICATION_KEY_BYTES, "HmacSHA256");
        String token = Jwts.builder()
                .issuer(ISSUER)
                .audience()
                .add(AUDIENCE)
                .and()
                .subject("user-10001")
                .expiration(Date.from(Instant.now().plusSeconds(300)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // When & Then
        contextRunner.run(context -> {
            GatewayAuthenticator authenticator = context.getBean(GatewayAuthenticator.class);
            assertThat(authenticator.authenticate(token)).contains(new AuthenticatedPrincipal("user-10001"));
        });
    }

    @Test
    void shouldFailContextForInvalidBase64VerificationKey() {
        // Given
        ApplicationContextRunner contextRunner = contextRunner("not-base64!");

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class);
        });
    }

    @Test
    void shouldFailContextForWeakVerificationKey() {
        // Given
        String weakKey = Base64.getEncoder().encodeToString("weak-key".getBytes(StandardCharsets.UTF_8));
        ApplicationContextRunner contextRunner = contextRunner(weakKey);

        // When & Then
        contextRunner.run(context -> {
            assertThat(context).hasFailed();
            assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(WeakKeyException.class);
        });
    }

    private ApplicationContextRunner contextRunner(String verificationKeyBase64) {
        return new ApplicationContextRunner()
                .withUserConfiguration(GatewaySecurityConfiguration.class)
                .withPropertyValues(
                        "gateweave.security.jwt.issuer=" + ISSUER,
                        "gateweave.security.jwt.audience=" + AUDIENCE,
                        "gateweave.security.jwt.verification-key-base64=" + verificationKeyBase64,
                        "gateweave.security.jwt.clock-skew=0s",
                        "gateweave.security.jwt.maximum-token-length=4096");
    }
}
