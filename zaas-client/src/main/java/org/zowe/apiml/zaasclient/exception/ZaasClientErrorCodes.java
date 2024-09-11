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

import java.util.HashMap;
import java.util.Map;

public enum ZaasClientErrorCodes {

    EXPIRED_JWT_EXCEPTION("ZWEAS100E", "Token is expired for URL", 401),
    INVALID_AUTHENTICATION("ZWEAS120E", "Invalid username or password", 401),
    EMPTY_NULL_USERNAME_PASSWORD("ZWEAS121E", "Empty or null username or password values provided", 400),
    EMPTY_NULL_AUTHORIZATION_HEADER("ZWEAS122E", "Empty or null authorization header provided", 400),
    INVALID_JWT_TOKEN("ZWEAS130E", "Invalid token provided", 400),
    GENERIC_EXCEPTION("ZWEAS170E", "An exception occurred while trying to get the token", 500),
    BAD_REQUEST("ZWEAS400E", "Unable to generate PassTicket. Verify that the secured signon (PassTicket) function " +
        "and application ID is configured properly by referring to  Using PassTickets in the guide for your security provider", 400),
    TOKEN_NOT_PROVIDED("ZWEAS401E", "Token is not provided", 401),
    SERVICE_UNAVAILABLE("ZWEAS404E", "Gateway service is unavailable", 503),
    EXPIRED_PASSWORD("ZWEAT412E", "The specified password is expired", 401),
    APPLICATION_NAME_NOT_FOUND("ZWEAS417E", "The application name wasn't found", 400);

    private final String id;
    private final String message;
    private final int returnCode;

    private static final Map<String, ZaasClientErrorCodes> errorNumberToEnum = new HashMap<>();

    static {
        for (ZaasClientErrorCodes value : values()) {
            errorNumberToEnum.put(value.id, value);
        }
    }

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

    public static ZaasClientErrorCodes byErrorNumber(String errorNumber) {
        return errorNumberToEnum.get(errorNumber);
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
