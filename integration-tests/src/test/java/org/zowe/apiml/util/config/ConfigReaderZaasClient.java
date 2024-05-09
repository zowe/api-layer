/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.config;

import org.zowe.apiml.zaasclient.config.ConfigProperties;

import static org.zowe.apiml.util.config.ConfigReader.environmentConfiguration;
import static org.zowe.apiml.util.requests.Endpoints.ROUTED_AUTH;

public class ConfigReaderZaasClient {

        public static ConfigProperties getConfigProperties() {
            return ConfigProperties.builder()
                .apimlHost(environmentConfiguration().getGatewayServiceConfiguration().getHost())
                .apimlPort(environmentConfiguration().getGatewayServiceConfiguration().getPort() + "")
                .apimlBaseUrl(ROUTED_AUTH)
                .keyStorePath(environmentConfiguration().getTlsConfiguration().getKeyStore())
                .keyStorePassword(environmentConfiguration().getTlsConfiguration().getKeyStorePassword())
                .keyStoreType(environmentConfiguration().getTlsConfiguration().getKeyStoreType())
                .trustStorePath(environmentConfiguration().getTlsConfiguration().getTrustStore())
                .trustStorePassword(environmentConfiguration().getTlsConfiguration().getTrustStorePassword())
                .trustStoreType(environmentConfiguration().getTlsConfiguration().getTrustStoreType())
                .nonStrictVerifySslCertificatesOfServices(environmentConfiguration().getTlsConfiguration().isNonStrictVerifySslCertificatesOfServices())
                .build();
        }

}

