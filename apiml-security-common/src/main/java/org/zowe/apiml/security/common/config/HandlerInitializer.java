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

import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;
import org.zowe.apiml.security.common.handler.BasicAuthUnauthorizedHandler;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.handler.UnauthorizedHandler;
import org.zowe.apiml.security.common.login.SuccessfulLoginHandler;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

/**
 * Convenience class that simplifies spring security configuration
 * Class contains most important handlers
 */
@Getter
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
}
