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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Service to handle the creation of the static definition file
 * Allows the generation and the override of the .yml. Retrieves the static definition location from the Discovery env
 * variables and stores the file there.
 */
@Service
@Slf4j
public class StaticDefinitionGenerator extends StaticAPIService {

    private static final String ENV_ENDPOINT = "application/env";

    private AtomicReference<String> fileName = new AtomicReference<>("");

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public StaticDefinitionGenerator(RestTemplate restTemplate, DiscoveryConfigProperties discoveryConfigProperties) {
        super(restTemplate, discoveryConfigProperties);
    }


    public StaticAPIResponse generateFile(String file) throws IOException {
        String location = retrieveStaticDefLocation();
        String serviceId = StringUtils.substringBetween(file, "serviceId: ", "\\n");
        file = formatFile(file);
        String absoluteFilePath = String.format("./%s/%s.yml", location, serviceId);
        fileName.set(absoluteFilePath);

        checkIfFileExists(absoluteFilePath);
        String message = "The static definition file has been created by the user! Its location is: %s";
        return writeFileAndSendResponse(file, fileName, String.format(message, fileName));
    }

    public StaticAPIResponse overrideFile(String file) throws IOException {
        String location = retrieveStaticDefLocation();
        String serviceId = StringUtils.substringBetween(file, "serviceId: ", "\\n");
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

    private String retrieveStaticDefLocation() {
        List<String> discoveryServiceUrls = getDiscoveryServiceUrls(ENV_ENDPOINT);
        for (int i = 0; i < discoveryServiceUrls.size(); i++) {

            String discoveryServiceUrl = discoveryServiceUrls.get(i);
            HttpEntity<?> entity = getHttpEntity(discoveryServiceUrl, true);
            try {
                ResponseEntity<String> response = super.getRestTemplate().exchange(discoveryServiceUrl, HttpMethod.GET, entity, String.class);

                // Return response if successful response or if none have been successful and this is the last URL to try
                if (response.getStatusCode().is2xxSuccessful() || i == discoveryServiceUrls.size() - 1) {
                    ObjectMapper mapper = new ObjectMapper();
                    JsonNode jsonNode;
                    jsonNode = mapper.readTree(response.getBody());
                    String location = jsonNode.findValue("apiml.discovery.staticApiDefinitionsDirectories").findValue("value").toString();
                    if (location == null || location.isEmpty()) {
                        log.debug("apiml.discovery.staticApiDefinitionsDirectories parameter is not defined");
                        throw new IOException("No location defined for the static definition.");
                    }
                    return location.replace("\"", "");
                }

            } catch (Exception e) {
                apimlLog.log("org.zowe.apiml.apicatalog.StaticDefinitionGenerationFailed", e.getMessage());
                return null;
            }
        }
        return null;
    }

}
