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
    BAD_CREDENTIALS("org.zowe.apiml.security.login.invalidCredentials", "Invalid Credentials", "Provide a valid username and password."),
    TOKEN_NOT_VALID("org.zowe.apiml.security.query.invalidToken", "Token is not valid.", "Provide a valid token."),
    BAD_ACCESS_TOKEN_BODY("org.zowe.apiml.security.query.invalidAccessTokenBody", "Personal Access Token body in the request is not valid.", "Use a valid body in the request. Format of a message: {validity: int , scopes: [string]}."),
    BAD_REVOKE_REQUEST_BODY("org.zowe.apiml.security.query.invalidRevokeRequestBody", "Body in the revoke request is not valid.", "Use a valid body in the request. Format of a message: {userId: string, (optional)timestamp: long} or {serviceId: string, (optional)timestamp: long}."),
    ACCESS_TOKEN_BODY_MISSING_SCOPES("org.zowe.apiml.security.query.accessTokenBodyMissingScopes", "Body in the HTTP request for Personal Access Token does not contain scopes.", "Provide a list of services for which this token will be valid."),
    TOKEN_NOT_PROVIDED("org.zowe.apiml.security.query.tokenNotProvided", "No authorization token provided.", "Provide a valid authorization token."),
    TOKEN_EXPIRED("org.zowe.apiml.security.expiredToken", "Token is expired.", "Obtain a new token by performing an authentication request."),
    AUTH_CREDENTIALS_NOT_FOUND("org.zowe.apiml.security.login.invalidInput", "Authorization header is missing, or request body is missing or invalid.", "Provide valid authentication."),
    AUTH_METHOD_NOT_SUPPORTED("org.zowe.apiml.security.invalidMethod", "Authentication method is not supported.", "Use the correct HTTP request method that is supported for the URL."),
    AUTH_REQUIRED("org.zowe.apiml.security.authRequired", "Authentication is required.", "Provide valid authentication."),
    AUTH_GENERAL("org.zowe.apiml.security.generic", "A failure occurred when authenticating.", "Refer to the specific authentication exception details for troubleshooting."),
    SERVICE_UNAVAILABLE("org.zowe.apiml.security.serviceUnavailable", "Authentication service not available.", "Make sure that the Authentication service is running and is accessible by the URL provided in the message."),
    GATEWAY_NOT_AVAILABLE("org.zowe.apiml.security.gatewayNotAvailable", "API Gateway Service not available.", "Check that both the service and Gateway are correctly registered in the Discovery service. Allow some time after the services are discovered for the information to propagate to individual services."),
    INVALID_TOKEN_TYPE("org.zowe.apiml.security.login.invalidTokenType", "Invalid token type in response from Authentication service.", "Review your APIML authentication provider configuration and ensure your Authentication service is working."),
    USER_SUSPENDED("org.zowe.apiml.security.platform.errno.EMVSSAFEXTRERR","Account Suspended", "Contact your security administrator to unsuspend your account."),
    NEW_PASSWORD_INVALID("org.zowe.apiml.security.platform.errno.EMVSPASSWORD", "The new password is not valid", "Provide valid password."),
    PASSWORD_EXPIRED("org.zowe.apiml.security.platform.errno.EMVSEXPIRE", "Password has expired", "Contact your security administrator to reset your password.");

    private final String errorMessageKey;
    private final String defaultMessage;
    private final String defaultAction;

    ErrorType(String errorMessageKey, String defaultMessage, String defaultAction) {
        this.errorMessageKey = errorMessageKey;
        this.defaultMessage = defaultMessage;
        this.defaultAction = defaultAction;
    }

    public String getErrorMessageKey() {
        return errorMessageKey;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getDefaultAction() {
        return defaultAction;
    }

    public static ErrorType fromMessageKey(String messageKey) {
        for (ErrorType errorType : ErrorType.values()) {
            if (errorType.errorMessageKey.equals(messageKey)) {
                return errorType;
            }
        }
        throw new IllegalArgumentException("Message key '" + messageKey + "' is invalid");
    }
}
