/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.staticapi;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * Service to handle the creation of the static definition file.
 * Allows the generation and the override of the .yml.
 * Retrieves the static definition location and stores the file there.
 */
@Service
@Slf4j
public class StaticDefinitionGenerator extends StaticAPIService {

    private AtomicReference<String> fileName = new AtomicReference<>("");

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @Value("${apiml.discovery.staticApiDefinitionsDirectories:config/local/api-defs}")
    private String staticApiDefinitionsDirectories;

    public StaticDefinitionGenerator(RestTemplate restTemplate, DiscoveryConfigProperties discoveryConfigProperties) {
        super(restTemplate, discoveryConfigProperties);
    }


    public StaticAPIResponse generateFile(String file, String serviceId) throws IOException {
        if (!serviceIdIsValid(serviceId)) {
            log.debug("The service ID {} has not valid format", serviceId);
            return new StaticAPIResponse(400, "The service ID format is not valid.");
        }
        String location = retrieveStaticDefLocation();
        file = formatFile(file);
        String absoluteFilePath = String.format("./%s/%s.yml", location, serviceId);
        fileName.set(absoluteFilePath);

        checkIfFileExists(absoluteFilePath);
        String message = "The static definition file has been created by the user! Its location is: %s";
        return writeFileAndSendResponse(file, fileName, String.format(message, fileName));
    }

    public StaticAPIResponse overrideFile(String file, String serviceId) throws IOException {
        if (!serviceIdIsValid(serviceId)) {
            log.debug("The service ID {} has not valid format", serviceId);
            return new StaticAPIResponse(400, "The service ID format is not valid.");
        }
        String location = retrieveStaticDefLocation();
        file = formatFile(file);
        String absoluteFilePath = String.format("./%s/%s.yml", location, serviceId);
        fileName.set(absoluteFilePath);
        String message = "The static definition file %s has been overwritten by the user!";
        return writeFileAndSendResponse(file, fileName, String.format(message, fileName));
    }

    private String formatFile(String file) {
        file = file.replace("\\n", System.lineSeparator());
        file = file.substring(1, file.length() - 1);
        return file;
    }

    private StaticAPIResponse writeFileAndSendResponse(String file, AtomicReference<String> fileName, String message) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(fileName.get())) {
            fos.write(file.getBytes(StandardCharsets.UTF_8));

            log.debug(message);
            return new StaticAPIResponse(201, message);
        }
    }

    private void checkIfFileExists(String location) throws FileAlreadyExistsException {
        File outFile = new File(location);
        if (outFile.exists()) {
            throw new FileAlreadyExistsException(String.format("The static definition file %s already exists!", location));
        }
    }

    /**
     * Retrieve the static definition location either from the System environments or configuration. If no property is set,
     * the default value is used (local environment). The static definition is stored inside the first defined directory.
     * @return the static definition location
     */
    private String retrieveStaticDefLocation() {
        log.debug(String.format("The value of apiml.discovery.staticApiDefinitionsDirectories is: %s", staticApiDefinitionsDirectories));
        String[] directories = staticApiDefinitionsDirectories.split(";");
        return directories[0];
    }

    /**
     * Validate the service ID
     * @param serviceId the service ID
     * @return true if valid
     */
    private boolean serviceIdIsValid(String serviceId) {
        if (serviceId != null && !serviceId.isEmpty()) {
            Pattern p = Pattern.compile("^[A-Za-z][A-Za-z0-9-]*$");
            if (p.matcher(serviceId).find() && serviceId.length() < 16) {
                return true;
            }
            return false;
        }
        return false;
        }
}

