/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.constants;

/**
 * Generally used constants for API Mediation Layer and services
 */
public final class ApimlConstants {

    private ApimlConstants() {
        throw new IllegalStateException("APIML constant class");
    }

    //Authentication constants
    public static final String BASIC_AUTHENTICATION_PREFIX = "Basic";
    public static final String BEARER_AUTHENTICATION_PREFIX = "Bearer";
}
