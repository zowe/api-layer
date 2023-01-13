/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Applications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;
import org.zowe.apiml.apicatalog.services.cached.CachedApiDocService;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;

@Slf4j
@Service
@ConditionalOnProperty(
    value = "apiml.catalog.standalone.enabled",
    havingValue = "true")
@RequiredArgsConstructor
public class StandaloneLoaderService {

    @Value("${apiml.catalog.standalone.servicesDirectory:services}")
    private String servicesDirectory;

    private final ObjectMapper objectMapper;
    private final InstanceInitializeService instanceInitializeService;
    private final CachedApiDocService cachedApiDocService;
    private final StandaloneAPIDocRetrievalService standaloneAPIDocRetrievalService;

    public void initializeCache() {
        loadApplicationCache();
        loadOpenAPICache();
    }

    private void loadApplicationCache() {
        File[] appFiles = getFiles(servicesDirectory + "/apps");
        if (appFiles.length == 0) {
            log.error("No service definition files found.");
            return;
        }

        for (File file : appFiles) {
            createContainerFromFile(file);
        }
    }

    private void loadOpenAPICache() {
        File[] openAPIFiles = getFiles(servicesDirectory + "/apiDocs");
        if (openAPIFiles.length == 0) {
            log.error("No apiDocs files found.");
            return;
        }

        for (File openAPIFile : openAPIFiles) {
            loadApiDocCache(openAPIFile);
        }
    }

    private void createContainerFromFile(File file) {
        log.info("Initialising services from '{}' file.", file.getName());

        try {
            Applications apps = objectMapper.readValue(file, Applications.class);
            apps.getRegisteredApplications().forEach(app -> {
                instanceInitializeService.createContainers(app);
                // Uses metadata from the first instance like {@link InstanceRetrievalService#getInstanceInfo}
                loadApiDocVersionCache(app.getInstances().get(0));
            });
        } catch (IOException e) {
            log.error("Unable to parse service definition '{}' because {}", file.getName(), e.getMessage());
        }
    }

    private void loadApiDocCache(File file) {
        try {
            String apiDoc = IOUtils.toString(Files.newInputStream(file.toPath()), StandardCharsets.UTF_8);
            String[] name = FilenameUtils.removeExtension(file.getName()).split("_");

            if (name.length < 2 || name.length > 3) {
                log.warn("ApiDoc file has incorrect format '{}'. The correct format is '{serviceId}_{version}(_default)'.", apiDoc);
                return;
            }

            if (name.length > 2 && name[2].equals("default")) {
                cachedApiDocService.updateApiDocForService(name[0], CachedApiDocService.DEFAULT_API_KEY, apiDoc);
            }

            cachedApiDocService.updateApiDocForService(name[0], name[1], apiDoc);
        } catch (IOException e) {
            log.error("Cannot read '{}' because {}", file.getName(), e.getMessage());
        }
    }

    private void loadApiDocVersionCache(InstanceInfo instanceInfo) {
        List<String> apiVersions = standaloneAPIDocRetrievalService.retrieveApiVersions(instanceInfo.getMetadata());
        cachedApiDocService.updateApiVersionsForService(instanceInfo.getAppName(), apiVersions);

        String defaultApiVersion = standaloneAPIDocRetrievalService.retrieveDefaultApiVersion(instanceInfo.getMetadata());
        cachedApiDocService.updateDefaultApiVersionForService(instanceInfo.getAppName(), defaultApiVersion);
    }

    private File[] getFiles(String directory) {
        File dir = new File(directory);
        if (!dir.isDirectory()) {
            log.error("Directory '{}' does not exists.", directory);
            return new File[0];
        }

        return dir.listFiles((d, name) -> name.endsWith(".json"));
    }

}
