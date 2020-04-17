/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.exception;

public enum ZaasClientErrorCodes {

    INVALID_AUTHENTICATION("ZWEAZC120E", "Invalid username or password", 401),
    EMPTY_NULL_USERNAME_PASSWORD("ZWEAZC121E", "Empty or null username or password values provided", 400),
    EMPTY_NULL_AUTHORIZATION_HEADER("ZWEAZC122E", "Empty or null authorization header provided", 400),
    SERVICE_UNAVAILABLE("ZWEAZC404E", "Gateway service is unavailable", 404),
    GENERIC_EXCEPTION("ZWEAZC170E", "An exception occurred while trying to get the token", 400),
    APPLICATION_NAME_NOT_FOUND("ZWEAZC417E", "The token provided is invalid", 404),
    TOKEN_NOT_PROVIDED("ZWEAZC401E", "Token is not provided", 401),
    EXPIRED_JWT_EXCEPTION("ZWEAT100E", "Token is expired for URL", 401);

    private final String id;
    private final String message;
    private final int returnCode;

    ZaasClientErrorCodes(String id, String message, int returnCode) {
        this.id = id;
        this.message = message;
        this.returnCode = returnCode;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    public int getReturnCode() {
        return returnCode;
    }

    @Override
    public String toString() {
        return "ZaasClientErrorCodes{" +
            "id='" + id + '\'' +
            ", message='" + message + '\'' +
            ", returnCode=" + returnCode +
            '}';
    }
}
