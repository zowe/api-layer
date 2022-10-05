/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.handler.BasicAuthUnauthorizedHandler;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.handler.UnauthorizedHandler;
import org.zowe.apiml.security.common.login.SuccessfulLoginHandler;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandlerInitializerTest {

    @Test
    void givenInputs_thenMapToCorrectFields() {
        SuccessfulLoginHandler loginHandler = new SuccessfulLoginHandler(null);
        UnauthorizedHandler unauthorizedHandler = new UnauthorizedHandler(null);
        BasicAuthUnauthorizedHandler basicAuthUnauthorizedHandler = new BasicAuthUnauthorizedHandler(null);
        FailedAuthenticationHandler failedAuthenticationHandler = new FailedAuthenticationHandler(null);
        ResourceAccessExceptionHandler resourceAccessExceptionHandler = new ResourceAccessExceptionHandler(null, null);
        HandlerInitializer handlerInitializer = new HandlerInitializer(loginHandler, unauthorizedHandler, basicAuthUnauthorizedHandler, failedAuthenticationHandler, resourceAccessExceptionHandler);
        assertEquals(loginHandler, handlerInitializer.getSuccessfulLoginHandler());
        assertEquals(unauthorizedHandler, handlerInitializer.getUnAuthorizedHandler());
        assertEquals(basicAuthUnauthorizedHandler, handlerInitializer.getBasicAuthUnauthorizedHandler());
        assertEquals(failedAuthenticationHandler, handlerInitializer.getAuthenticationFailureHandler());
        assertEquals(resourceAccessExceptionHandler, handlerInitializer.getResourceAccessExceptionHandler());
    }
}
