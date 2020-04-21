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
    TRUST_STORE_NOT_PROVIDED("ZWEAS500E", "There was no path to the trust store."),
    KEY_STORE_NOT_PROVIDED("ZWEAS501E","There was no path to the key store."),
    WRONG_CRYPTO_CONFIGURATION("ZWEAS502E", "The configuration provided for SSL is invalid."),
    IO_CONFIGURATION_ISSUE("ZWEAS503E", "The SSL configuration contained invalid path.");

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
