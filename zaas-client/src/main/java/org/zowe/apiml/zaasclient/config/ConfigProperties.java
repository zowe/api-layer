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

import java.util.Objects;

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

    private static final String GATEWAY_SERVICE_ID = "gateway";

    @Tolerate
    public ConfigProperties() {
        // no args constructor
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
            .build();
    }

    public void setApimlBaseUrl(String baseUrl) {
        if (baseUrl == null) { // default path
            apimlBaseUrl = "/gateway/api/v1/auth";
        }
        else if (baseUrl.contains("/") && baseUrl.contains(GATEWAY_SERVICE_ID)) {
            String[] baseUrlParts = baseUrl.split("/");
            if (Objects.equals(baseUrlParts[2], GATEWAY_SERVICE_ID)) {
                apimlBaseUrl = "/gateway/" + baseUrlParts[0] + "/" + baseUrlParts[1] + "/auth";
            }
            else if (Objects.equals(baseUrlParts[3], GATEWAY_SERVICE_ID)) {
                apimlBaseUrl = "/gateway/" + baseUrlParts[1] + "/" + baseUrlParts[2] + "/auth";
            }
            else if (!baseUrl.startsWith("/")) { // starts with gateway/..
                apimlBaseUrl = "/" + baseUrl;
            }
            else {
                apimlBaseUrl = baseUrl;
            }
        }
        else {
            apimlBaseUrl = baseUrl;
        }
    }

}
