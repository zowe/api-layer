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

import lombok.Getter;
import lombok.Setter;
import org.springframework.security.access.expression.SecurityExpressionRoot;
import org.springframework.security.access.expression.method.MethodSecurityExpressionOperations;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;

public class SafMethodSecurityExpressionRoot extends SecurityExpressionRoot
    implements MethodSecurityExpressionOperations {

    private final SafSecurityConfigurationProperties safSecurityConfigurationProperties;
    private final SafResourceAccessVerifying safResourceAccessVerifying;

    @Getter
    @Setter
    private Object filterObject;

    @Getter
    @Setter
    private Object returnObject;

    public SafMethodSecurityExpressionRoot(SafSecurityConfigurationProperties safSecurityConfigurationProperties, SafResourceAccessVerifying safResourceAccessVerifying, Authentication authentication) {
        super(authentication);
        this.safSecurityConfigurationProperties = safSecurityConfigurationProperties;
        this.safResourceAccessVerifying = safResourceAccessVerifying;
    }

    @Override
    public Object getThis() {
        return this;
    }

    public boolean hasSafResourceAccess(String resourceClass, String resourceName, String accessLevel) {
        return safResourceAccessVerifying.hasSafResourceAccess(authentication, resourceClass, resourceName, accessLevel);
    }

    public boolean hasSafServiceResourceAccess(String resourceNameSuffix, String accessLevel) {
        return hasSafResourceAccess(safSecurityConfigurationProperties.getServiceResourceClass(), safSecurityConfigurationProperties.getServiceResourceNamePrefix() + resourceNameSuffix, accessLevel);
    }

}
