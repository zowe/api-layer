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

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.zaasclient.config.ConfigProperties;

import java.io.IOException;

@Slf4j
public class ConfigReaderZaasClient {

    public final static int PORT = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort();

    public static ConfigProperties getConfigProperties() {

        ConfigProperties configProperties = new ConfigProperties();


        configProperties.setApimlHost(ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getHost());
        configProperties.setApimlPort(String.valueOf(ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getPort()));
        configProperties.setApimlBaseUrl("/api/v1/gateway/auth");
        configProperties.setKeyStorePath(ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStore());
        configProperties.setKeyStorePassword(ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStorePassword());
        configProperties.setKeyStoreType(ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStoreType());
        configProperties.setTrustStorePath(ConfigReader.environmentConfiguration().getTlsConfiguration().getTrustStore());
        configProperties.setTrustStorePassword(ConfigReader.environmentConfiguration().getTlsConfiguration().getTrustStorePassword());
        configProperties.setTrustStoreType(ConfigReader.environmentConfiguration().getTlsConfiguration().getTrustStoreType());

        return configProperties;
    }
}

