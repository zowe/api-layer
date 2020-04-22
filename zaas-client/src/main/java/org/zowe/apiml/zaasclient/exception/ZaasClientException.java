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
    private final ZaasClientErrorCodes errorCode;

    public ZaasClientException(ZaasClientErrorCodes code) {
        super(code.toString());

        errorCode = code;
    }

    public ZaasClientException(ZaasClientErrorCodes code, Throwable e) {
        super(code.toString(), e);

        errorCode = code;
    }

    public ZaasClientException(ZaasClientErrorCodes code, String downstreamMessage) {
        super(downstreamMessage + " \n " + code.toString());

        errorCode = code;
    }

    public ZaasClientErrorCodes getErrorCode() {
        return errorCode;
    }
}
