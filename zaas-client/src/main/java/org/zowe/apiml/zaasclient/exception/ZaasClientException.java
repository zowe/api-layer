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

public class ZaasClientException extends Exception {

    private String errorCode;
    private String errorMessage;
    private int httpResponseCode;

    public ZaasClientException(ZaasClientErrorCodes code) {
        this.errorCode = code.getId();
        this.errorMessage = code.getMessage();
        this.httpResponseCode = code.getReturnCode();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public int getHttpResponseCode() {
        return httpResponseCode;
    }
}
