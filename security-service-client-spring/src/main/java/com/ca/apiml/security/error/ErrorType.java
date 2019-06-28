/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.error;

public enum ErrorType {
    BAD_CREDENTIALS("apiml.security.login.invalidCredentials", "Username or password are invalid."),
    TOKEN_NOT_VALID("apiml.security.query.invalidToken", "Token is not valid."),
    TOKEN_NOT_PROVIDED("apiml.security.query.tokenNotProvided", "No authorization token provided"),
    TOKEN_EXPIRED("apiml.security.expiredToken", "Token is expired"),
    AUTH_CREDENTIALS_NOT_FOUND("apiml.security.login.invalidInput", "Authorization header is missing, or request body is missing or invalid"),
    AUTH_METHOD_NOT_SUPPORTED("apiml.security.invalidMethod", "Authentication method is not supported"),
    AUTH_GENERAL("apiml.security.generic", "A failure occurred when authenticating.");

    private String errorMessageKey;
    private String defaultMessage;

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
