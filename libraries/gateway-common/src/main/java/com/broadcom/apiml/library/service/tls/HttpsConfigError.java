/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.tls;

enum ErrorCode {
    UNKNOWN_ERROR, HTTP_CLIENT_INITIALIZATION_FAILED, KEYSTORE_NOT_DEFINED, KEYSTORE_PASSWORD_NOT_DEFINED,
    TRUSTSTORE_PASSWORD_NOT_DEFINED, SSL_CONTEXT_INITIALIZATION_FAILED, TRUSTSTORE_NOT_DEFINED, WRONG_KEY_ALIAS;
}

public class HttpsConfigError extends RuntimeException {
    private final ErrorCode code;
    private final HttpsConfig config;

    public HttpsConfigError(ErrorCode code, HttpsConfig config) {
        this.code = code;
        this.config = config;
    }

    public HttpsConfigError(String message, Throwable cause, ErrorCode code, HttpsConfig config) {
        super(message, cause);
        this.code = code;
        this.config = config;
    }

    public HttpsConfigError(String message, ErrorCode code, HttpsConfig config) {
        super(message);
        this.code = code;
        this.config = config;
    }

    public HttpsConfigError(Throwable cause, ErrorCode code, HttpsConfig config) {
        super(cause);
        this.code = code;
        this.config = config;
    }

    public ErrorCode getCode() {
        return this.code;
    }

    public HttpsConfig getConfig() {
        return this.config;
    }

}
