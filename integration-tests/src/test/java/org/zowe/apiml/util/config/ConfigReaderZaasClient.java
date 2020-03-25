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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.zaasclient.config.ConfigProperties;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Objects;
import java.util.Properties;

@Slf4j
public class ConfigReaderZaasClient {

        static final String configFileName = "configFile-properties.yml";
        ClassLoader classLoader = ClassLoader.getSystemClassLoader();
        File configFile = new File(Objects.requireNonNull(classLoader.getResource(configFileName)).getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        EnvironmentConfiguration configuration;


        public static ConfigProperties getConfigProperties () {
            String absoluteFilePath = new File(configFileName).getAbsolutePath();
            ConfigProperties configProperties = new ConfigProperties();
            Properties configProp = new Properties();
            try {
                if (Paths.get(absoluteFilePath).toFile().exists()) {
                    configProp.load(new FileReader(absoluteFilePath));

                    configProperties.setApimlHost(configProp.getProperty("APIML_HOST"));
                    configProperties.setApimlPort(configProp.getProperty("APIML_PORT"));
                    configProperties.setApimlBaseUrl(configProp.getProperty("APIML_BASE_URL"));
                    configProperties.setKeyStorePath(configProp.getProperty("KEYSTOREPATH"));
                    configProperties.setKeyStorePassword(configProp.getProperty("KEYSTOREPASSWORD"));
                    configProperties.setKeyStoreType(configProp.getProperty("KEYSTORETYPE"));
                    configProperties.setTrustStorePath(configProp.getProperty("TRUSTSTOREPATH"));
                    configProperties.setTrustStorePassword(configProp.getProperty("TRUSTSTOREPASSWORD"));
                    configProperties.setTrustStoreType(configProp.getProperty("TRUSTSTORETYPE"));
                }
            } catch (IOException e) {
                System.out.println(e.getMessage());
            }
            return configProperties;
        }


}

