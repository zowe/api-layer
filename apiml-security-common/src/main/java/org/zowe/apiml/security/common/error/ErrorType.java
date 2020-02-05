/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security.common.error;

/**
 * Enum of error types
 * binding error keys and default error messages
 */
public enum ErrorType {
    BAD_CREDENTIALS("org.zowe.apiml.security.login.invalidCredentials", "Username or password are invalid."),
    TOKEN_NOT_VALID("org.zowe.apiml.security.query.invalidToken", "Token is not valid."),
    TOKEN_NOT_PROVIDED("org.zowe.apiml.security.query.tokenNotProvided", "No authorization token provided."),
    TOKEN_EXPIRED("org.zowe.apiml.security.expiredToken", "Token is expired."),
    AUTH_CREDENTIALS_NOT_FOUND("org.zowe.apiml.security.login.invalidInput", "Authorization header is missing, or request body is missing or invalid."),
    AUTH_METHOD_NOT_SUPPORTED("org.zowe.apiml.security.invalidMethod", "Authentication method is not supported."),
    AUTH_REQUIRED("org.zowe.apiml.security.authRequired", "Authentication is required."),
    AUTH_GENERAL("org.zowe.apiml.security.generic", "A failure occurred when authenticating."),
    SERVICE_UNAVAILABLE("org.zowe.apiml.security.serviceUnavailable", "Authentication service not available."),
    GATEWAY_NOT_AVAILABLE("org.zowe.apiml.security.gatewayNotAvailable", "API Gateway Service not available.");

    private final String errorMessageKey;
    private final String defaultMessage;

    ErrorType(String errorMessageKey, String defaultMessage) {
        this.errorMessageKey = errorMessageKey;
        this.defaultMessage = defaultMessage;
    }

    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }
}
