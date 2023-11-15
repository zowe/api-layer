/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

public final class Constants {

    /**
     * Attribute to control HttpClient to sign request by client certificate.
     *
     * ServerWebExchange exchange;
     * exchange.getAttributes().put(HTTP_CLIENT_USE_CLIENT_CERTIFICATE, Boolean.TRUE);
     *
     * If the attribute is set to Boolean.TRUE request will be signed, otherwise not.
     */
    public static final String HTTP_CLIENT_USE_CLIENT_CERTIFICATE = "apiml.useClientCert";

    private Constants() {}

}
