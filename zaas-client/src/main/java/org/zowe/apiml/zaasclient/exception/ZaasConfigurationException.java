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
    private final ZaasConfigurationErrorCodes errorCode;

    public ZaasConfigurationException(ZaasConfigurationErrorCodes code) {
        super(code.toString());

        errorCode = code;
    }

    public ZaasConfigurationException(ZaasConfigurationErrorCodes code, Throwable e) {
        super(code.toString(), e);

        errorCode = code;
    }

    public ZaasConfigurationException(ZaasConfigurationErrorCodes code, String downstreamMessage) {
        super(downstreamMessage + " \n " + code.toString());

        errorCode = code;
    }

    public ZaasConfigurationErrorCodes getErrorCode() {
        return errorCode;
    }
}
