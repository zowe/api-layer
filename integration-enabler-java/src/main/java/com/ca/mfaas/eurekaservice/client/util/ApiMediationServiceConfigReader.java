/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice.client.util;

import com.ca.mfaas.eurekaservice.client.config.ApiMediationServiceConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.net.URL;

public class ApiMediationServiceConfigReader {
    private final String fileName;

    public ApiMediationServiceConfigReader(final String fileName) {
        this.fileName = fileName;
    }

    public ApiMediationServiceConfig readConfiguration() {
        URL fileUrl = getClass().getResource(this.fileName);
        if (fileUrl == null) {
            throw new ApiMediationServiceConfigReaderException(String.format("File [%s] is not exist", this.fileName));
        }
        File configFile = new File(fileUrl.getFile());
        ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
        ApiMediationServiceConfig configuration;
        try {
            configuration = mapper.readValue(configFile, ApiMediationServiceConfig.class);
        } catch (IOException e) {
            throw new ApiMediationServiceConfigReaderException(String.format("File [%s] can't be parsed as ApiMediationServiceConfig", this.fileName));
        }

        return configuration;
    }
}
