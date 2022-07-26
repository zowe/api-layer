/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import org.aopalliance.intercept.MethodInvocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.access.hierarchicalroles.RoleHierarchy;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class SafMethodSecurityExpressionHandlerTest {

    @Mock
    private Authentication authentication;

    @Mock
    private SafSecurityConfigurationProperties safSecurityConfigurationProperties;

    @Mock
    private SafResourceAccessVerifying safResourceAccessVerifying;

    @Mock
    private MethodInvocation invocation;

    @Mock
    private PermissionEvaluator permissionEvaluator;

    @Mock
    private RoleHierarchy roleHierarchy;

    private SafMethodSecurityExpressionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new SafMethodSecurityExpressionHandler(
            safSecurityConfigurationProperties, safResourceAccessVerifying
        );
        ReflectionTestUtils.setField(handler, "permissionEvaluator", permissionEvaluator);
        ReflectionTestUtils.setField(handler, "roleHierarchy", roleHierarchy);
    }

    @Test
    void testCreateSecurityExpressionRoot_whenInit_thenProperlySet() {
        MethodSecurityExpressionOperations mseo = handler.createSecurityExpressionRoot(authentication, invocation);
        assertSame(authentication, mseo.getAuthentication());
        assertTrue(ReflectionTestUtils.getField(mseo, "trustResolver") instanceof AuthenticationTrustResolverImpl);
        assertSame(permissionEvaluator, ReflectionTestUtils.getField(mseo, "permissionEvaluator"));
        assertSame(roleHierarchy, ReflectionTestUtils.getField(mseo, "roleHierarchy"));
    }

}
