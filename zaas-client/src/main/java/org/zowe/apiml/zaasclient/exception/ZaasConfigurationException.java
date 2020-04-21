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

public class ZaasConfigurationException extends Exception {

    private final String errorCode;
    private final String errorMessage;

    public ZaasConfigurationException(ZaasConfigurationErrorCodes code, String errorMessage) {
        super(errorMessage);
        this.errorCode = code.getId();
        this.errorMessage = code.getMessage();
    }

    public ZaasConfigurationException(ZaasConfigurationErrorCodes code) {
        this.errorCode = code.getId();
        this.errorMessage = code.getMessage();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getMessage() {
        return errorMessage;
    }
}
