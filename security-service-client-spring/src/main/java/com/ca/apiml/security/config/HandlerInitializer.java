/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.apiml.security.config;

import com.ca.apiml.security.error.ResourceAccessExceptionHandler;
import com.ca.apiml.security.handler.BasicAuthUnauthorizedHandler;
import com.ca.apiml.security.handler.FailedAuthenticationHandler;
import com.ca.apiml.security.handler.UnauthorizedHandler;
import com.ca.apiml.security.login.SuccessfulLoginHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class HandlerInitializer {

    private final SuccessfulLoginHandler successfulLoginHandler;
    private final UnauthorizedHandler unAuthorizedHandler;
    private final BasicAuthUnauthorizedHandler basicAuthUnauthorizedHandler;
    private final FailedAuthenticationHandler authenticationFailureHandler;
    private final ResourceAccessExceptionHandler resourceAccessExceptionHandler;

    public HandlerInitializer(SuccessfulLoginHandler successfulLoginHandler,
                              @Qualifier("plainAuth")
                                  UnauthorizedHandler unAuthorizedHandler,
                              BasicAuthUnauthorizedHandler basicAuthUnauthorizedHandler,
                              FailedAuthenticationHandler authenticationFailureHandler,
                              ResourceAccessExceptionHandler resourceAccessExceptionHandler) {
        this.successfulLoginHandler = successfulLoginHandler;
        this.unAuthorizedHandler = unAuthorizedHandler;
        this.basicAuthUnauthorizedHandler = basicAuthUnauthorizedHandler;
        this.authenticationFailureHandler = authenticationFailureHandler;
        this.resourceAccessExceptionHandler = resourceAccessExceptionHandler;
    }

    public SuccessfulLoginHandler getSuccessfulLoginHandler() {
        return successfulLoginHandler;
    }

    public UnauthorizedHandler getUnAuthorizedHandler() {
        return unAuthorizedHandler;
    }

    public BasicAuthUnauthorizedHandler getBasicAuthUnauthorizedHandler() {
        return basicAuthUnauthorizedHandler;
    }

    public FailedAuthenticationHandler getAuthenticationFailureHandler() {
        return authenticationFailureHandler;
    }

    public ResourceAccessExceptionHandler getResourceAccessExceptionHandler() {
        return resourceAccessExceptionHandler;
    }
}
