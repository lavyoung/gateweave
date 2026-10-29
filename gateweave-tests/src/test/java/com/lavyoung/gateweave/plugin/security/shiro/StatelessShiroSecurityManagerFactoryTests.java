package com.lavyoung.gateweave.plugin.security.shiro;

import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.DefaultSessionStorageEvaluator;
import org.apache.shiro.mgt.DefaultSubjectDAO;
import org.apache.shiro.realm.Realm;
import org.apache.shiro.session.mgt.DefaultSessionManager;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

class StatelessShiroSecurityManagerFactoryTests {

    @Test
    void shouldRegisterRealmAndDisableSessionInfrastructure() {
        // Given
        Realm realm = mock(Realm.class);

        // When
        DefaultSecurityManager securityManager = StatelessShiroSecurityManagerFactory.create(realm);

        // Then
        assertThat(securityManager.getRealms()).containsExactly(realm);

        DefaultSubjectDAO subjectDAO = (DefaultSubjectDAO) securityManager.getSubjectDAO();
        DefaultSessionStorageEvaluator storageEvaluator =
                (DefaultSessionStorageEvaluator) subjectDAO.getSessionStorageEvaluator();
        assertThat(storageEvaluator.isSessionStorageEnabled()).isFalse();

        DefaultSessionManager sessionManager = (DefaultSessionManager) securityManager.getSessionManager();
        assertThat(sessionManager.isSessionValidationSchedulerEnabled()).isFalse();
    }

    @Test
    void shouldRejectNullRealm() {
        // When & Then
        assertThatThrownBy(() -> StatelessShiroSecurityManagerFactory.create(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("realm must not be null");
    }
}
