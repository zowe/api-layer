/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaasclient.config;

import lombok.Builder;
import lombok.Data;
import lombok.experimental.Tolerate;

@Data
@Builder
public class ConfigProperties {

    private String apimlHost;
    private String apimlPort;
    private String apimlBaseUrl;
    private String keyStoreType;
    private String keyStorePath;
    private char[] keyStorePassword;
    private String trustStoreType;
    private String trustStorePath;
    private char[] trustStorePassword;
    private boolean httpOnly;
    private boolean nonStrictVerifySslCertificatesOfServices;

    @SuppressWarnings("squid:S1075")
    private static final String OLD_PATH_FORMAT = "/api/v1/gateway";
    @SuppressWarnings("squid:S1075")
    private static final String NEW_PATH_FORMAT = "/gateway/api/v1";

    @Builder.Default
    private String tokenPrefix = "apimlAuthenticationToken";

    @Tolerate
    public ConfigProperties() {
        // lombok Builder.Default bug workaround
        this.tokenPrefix = "apimlAuthenticationToken";
    }

    public ConfigProperties withoutKeyStore() {
        return ConfigProperties.builder()
            .apimlHost(apimlHost)
            .apimlPort(apimlPort)
            .apimlBaseUrl(apimlBaseUrl)
            .trustStoreType(trustStoreType)
            .trustStorePath(trustStorePath)
            .trustStorePassword(trustStorePassword)
            .httpOnly(httpOnly)
            .nonStrictVerifySslCertificatesOfServices(nonStrictVerifySslCertificatesOfServices)
            .tokenPrefix(tokenPrefix)
            .build();
    }

    public void setApimlBaseUrl(String baseUrl) {
        // set default path if it is missing
        if (baseUrl == null) {
            baseUrl = "/gateway/api/v1/auth";
        }

        // if path does not start with / add it
        if (!baseUrl.startsWith("/")) {
            baseUrl = "/" + baseUrl;
        }

        // replace old path format with the new one
        if (baseUrl.startsWith(OLD_PATH_FORMAT)) {
            baseUrl = NEW_PATH_FORMAT + baseUrl.substring(OLD_PATH_FORMAT.length());
        }

        apimlBaseUrl = baseUrl;
    }

}
