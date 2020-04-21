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

public enum ZaasConfigurationErrorCodes {
    TRUST_STORE_NOT_PROVIDED("ZWEACZ500E", ""),
    KEY_STORE_NOT_PROVIDED("ZWEACZ501E",""),
    WRONG_CRYPTO_CONFIGURATION("ZWEACZ502E", ""),
    IO_CONFIGURATION_ISSUE("ZWEACZ503E", "");

    private final String id;
    private final String message;

    ZaasConfigurationErrorCodes(String id, String message) {
        this.id = id;
        this.message = message;
    }

    public String getId() {
        return id;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ZaasClientErrorCodes{" +
            "id='" + id + '\'' +
            ", message='" + message +
            '}';
    }
}
