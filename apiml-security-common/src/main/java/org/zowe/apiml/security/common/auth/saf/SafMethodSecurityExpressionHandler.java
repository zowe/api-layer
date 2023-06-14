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

import lombok.RequiredArgsConstructor;
import org.aopalliance.intercept.MethodInvocation;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;

@RequiredArgsConstructor
public class SafMethodSecurityExpressionHandler extends DefaultMethodSecurityExpressionHandler {

    private static final AuthenticationTrustResolver TRUST_RESOLVER = new AuthenticationTrustResolverImpl();

    private final SafSecurityConfigurationProperties safSecurityConfigurationProperties;
    private final SafResourceAccessVerifying safResourceAccessVerifying;

    @Override
    protected MethodSecurityExpressionOperations createSecurityExpressionRoot(Authentication authentication, MethodInvocation invocation) {
        SafMethodSecurityExpressionRoot root = new SafMethodSecurityExpressionRoot(safSecurityConfigurationProperties, safResourceAccessVerifying, authentication);
        root.setPermissionEvaluator(getPermissionEvaluator());
        root.setTrustResolver(TRUST_RESOLVER);
        root.setRoleHierarchy(getRoleHierarchy());
        return root;
    }

}
