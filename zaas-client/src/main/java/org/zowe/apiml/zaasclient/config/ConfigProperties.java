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

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;

@Getter
@AllArgsConstructor
public class ConfigProperties {

    private static final String CONFIG_FILE_NAME = "configFile.properties";

    private String apimlHost;
    private String apimlPort;
    private String apimlBaseUrl;
    private String keyStoreType;
    private String keyStorePath;
    private String keyStorePassword;
    private String trustStoreType;
    private String trustStorePath;
    private String trustStorePassword;

    public ConfigProperties() throws IOException {
        String configFile = System.getProperty("CONFIG_FILE");
        Properties configProperties= new Properties();
        if(configFile!=null && !configFile.isEmpty()) {
            configProperties.load(new FileReader(configFile));
        } else if(Paths.get(CONFIG_FILE_NAME).toFile().exists()) {
            configProperties.load(new FileReader(CONFIG_FILE_NAME));
        } else {
            InputStream inputStream = getClass().getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);
            configProperties.load(inputStream);
        }
        this.keyStorePath = configProperties.getProperty("KEYSTOREPATH");
        this.keyStorePassword = configProperties.getProperty("KEYSTOREPASSWORD");
        this.keyStoreType = configProperties.getProperty("KEYSTORETYPE");
        this.trustStorePath = configProperties.getProperty("TRUSTSTOREPATH");
        this.trustStorePassword = configProperties.getProperty("TRUSTSTOREPASSWORD");
        this.trustStoreType = configProperties.getProperty("TRUSTSTORETYPE");
        this.apimlHost = configProperties.getProperty("APIML_HOST");
        this.apimlPort = configProperties.getProperty("APIML_PORT");
        this.apimlBaseUrl = configProperties.getProperty("APIML_BASE_URL");
    }

    @Override
    public String toString() {
        return "ConfigProperties{" +
            "apimlHost='" + apimlHost + '\'' +
            ", apimlPort='" + apimlPort + '\'' +
            ", apimlBaseUrl='" + apimlBaseUrl + '\'' +
            ", keyStoreType='" + keyStoreType + '\'' +
            ", keyStorePath='" + keyStorePath + '\'' +
            ", keyStorePassword='" + "" + '\'' +
            ", trustStoreType='" + trustStoreType + '\'' +
            ", trustStorePath='" + trustStorePath + '\'' +
            ", trustStorePassword='" + "" + '\'' +
            '}';
    }
}
