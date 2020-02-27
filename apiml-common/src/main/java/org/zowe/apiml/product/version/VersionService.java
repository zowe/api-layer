/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.version;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.util.FileUtils;

import java.io.IOException;

/**
 * Class for retrieving information about Zowe version from Zowe's manifest.json
 * and information about API ML version from build-info.properties and git.properties
 */

@Slf4j
@Service
public class VersionService {
    private static final String NO_VERSION = "Build information is not available";
    private static final VersionInfo version = new VersionInfo();

    private final BuildInfo buildInfo;

    @Value("${apiml.zoweManifest:#{null}}")
    private String zoweManifest;

    public VersionService() {
        buildInfo = new BuildInfo();
    }

    public VersionService(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    /**
     * Getting the cached VersionInfo object, if it's empty it will be filled
     * @return filled VersionInfo object
     */
    public VersionInfo getVersion() {
        if (version.getApimlVersion() == null) {
            updateVersionInfo();
        }
        return version;
    }

    /**
     * Updating the cached VersionInfo object with values from Zowe's manifest.json, API ML's build-info.properties
     * and git.properties files
     */
    public void updateVersionInfo() {
        if (StringUtils.isNotEmpty(zoweManifest)) {
            version.setZoweVersion(getZoweVersion(zoweManifest));
        }
        version.setApimlVersion(getApimlVersion());
    }

    /**
     * Retrieving the information about API ML version from build-info.properties and git.properties files
     * @return the version, build and commit numbers in one string
     */
    private String getApimlVersion() {
        BuildInfoDetails buildInfoDetails = buildInfo.getBuildInfoDetails();
        String apimlVersion = NO_VERSION;
        if (!buildInfoDetails.getVersion().equalsIgnoreCase("unknown")) {
            apimlVersion = String.format("%s build #%s (%s)", buildInfoDetails.getVersion(), buildInfoDetails.getNumber(), buildInfoDetails.getCommitId());
        }
        return apimlVersion;
    }

    /**
     * Retrieving the information about Zowe version from manifest.json file
     * @param manifestJsonFile the path to Zowe's manifest.json file
     * @return the version and build numbers in one string
     */
    private String getZoweVersion(String manifestJsonFile) {
        try {
            String manifestJson = FileUtils.readFile(manifestJsonFile);
            if (manifestJson != null) {
                return retrieveZoweVersionFromJson(manifestJson);
            } else {
                log.debug("File have not found in provided location: {}", manifestJsonFile);
            }
        } catch (IOException e) {
            log.debug("Error in reading the file {}: {}", manifestJsonFile, e.getMessage());
        }
        return NO_VERSION;
    }

    private String retrieveZoweVersionFromJson(String manifestJson) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            ObjectNode manifestNode = mapper.readValue(manifestJson, ObjectNode.class);
            JsonNode versionNode = manifestNode.get("version");
            if (versionNode != null && !versionNode.asText().isEmpty()) {
                return versionNode.asText() +
                    " build #" +
                    retrieveZoweBuildNumber(manifestNode);
            }
        } catch (JsonProcessingException e) {
            log.debug("Error in parsing Zowe build manifest.json file: {}", e.getMessage());
        }
        return NO_VERSION;
    }

    private String retrieveZoweBuildNumber(ObjectNode manifestNode) {
        String buildNumber = "n/a";
        JsonNode buildNode = manifestNode.get("build");
        if (buildNode != null) {
            JsonNode buildNumberNode = buildNode.get("number");
            if (buildNumberNode != null && StringUtils.isNotEmpty(buildNumberNode.asText())) {
                buildNumber = buildNumberNode.asText();
            }
        }
        return buildNumber;
    }

    public void clearVersionInfo() {
        version.setApimlVersion(null);
        version.setZoweVersion(null);
    }
}
