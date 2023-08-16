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

            ConfigProperties configProperties = new ConfigProperties();


            configProperties.setApimlHost(environmentConfiguration().getGatewayServiceConfiguration().getHost());
            configProperties.setApimlPort(environmentConfiguration().getGatewayServiceConfiguration().getPort() + "");
            configProperties.setApimlBaseUrl(ROUTED_AUTH);
            configProperties.setKeyStorePath(environmentConfiguration().getTlsConfiguration().getKeyStore());
            configProperties.setKeyStorePassword(environmentConfiguration().getTlsConfiguration().getKeyStorePassword());
            configProperties.setKeyStoreType(environmentConfiguration().getTlsConfiguration().getKeyStoreType());
            configProperties.setTrustStorePath(environmentConfiguration().getTlsConfiguration().getTrustStore());
            configProperties.setTrustStorePassword(environmentConfiguration().getTlsConfiguration().getTrustStorePassword());
            configProperties.setTrustStoreType(environmentConfiguration().getTlsConfiguration().getTrustStoreType());
            configProperties.setNonStrictVerifySslCertificatesOfServices(environmentConfiguration().getTlsConfiguration().isNonStrictVerifySslCertificatesOfServices());
            return configProperties;
        }
}

