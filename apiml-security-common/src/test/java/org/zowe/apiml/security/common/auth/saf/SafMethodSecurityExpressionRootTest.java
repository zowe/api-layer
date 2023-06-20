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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.zowe.apiml.security.common.config.SafSecurityConfigurationProperties;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

@ExtendWith(MockitoExtension.class)
class SafMethodSecurityExpressionRootTest {

    @Mock
    private SafSecurityConfigurationProperties safSecurityConfigurationProperties;

    @Mock
    private SafResourceAccessVerifying safResourceAccessVerifying;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private SafMethodSecurityExpressionRoot safMethodSecurityExpressionRoot;

    @Test
    void testFilterObject_whenSetValue_thenIsSet() {
        assertNull(safMethodSecurityExpressionRoot.getFilterObject());

        Object filterObject = new Object();
        safMethodSecurityExpressionRoot.setFilterObject(filterObject);
        assertSame(filterObject, safMethodSecurityExpressionRoot.getFilterObject());
    }

    @Test
    void testReturnObject_whenSetValue_thenIsSet() {
        assertNull(safMethodSecurityExpressionRoot.getReturnObject());

        Object returnObject = new Object();
        safMethodSecurityExpressionRoot.setReturnObject(returnObject);
        assertSame(returnObject, safMethodSecurityExpressionRoot.getReturnObject());
    }

    @Test
    void testGetThis_whenPrepared_thenReturnSameInstance() {
        assertSame(safMethodSecurityExpressionRoot, safMethodSecurityExpressionRoot.getThis());
    }

    @Test
    void testGetAuthentication_whenPrepared_thenReturnInstanceFromConstructor() {
        assertSame(authentication, safMethodSecurityExpressionRoot.getAuthentication());
    }

}
