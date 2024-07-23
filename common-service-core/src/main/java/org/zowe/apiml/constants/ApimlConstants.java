/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.constants;

/**
 * Generally used constants for API Mediation Layer and services
 */
public final class ApimlConstants {

    private ApimlConstants() {
        throw new IllegalStateException("APIML constant class");
    }

    public static final String BASIC_AUTHENTICATION_PREFIX = "Basic";
    public static final String BEARER_AUTHENTICATION_PREFIX = "Bearer";
    public static final String PAT_COOKIE_AUTH_NAME = "personalAccessToken";
    public static final String PAT_HEADER_NAME = "PRIVATE-TOKEN";
    public static final String AUTH_FAIL_HEADER = "X-Zowe-Auth-Failure";
    public static final String HTTP_CLIENT_USE_CLIENT_CERTIFICATE = "apiml.useClientCert";
    public static final String SAF_TOKEN_HEADER = "X-SAF-Token";
    public static final String HEADER_OIDC_TOKEN = "OIDC-token";
    public static final String X_INSTANCEID = "X-InstanceId";

}
