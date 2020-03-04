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
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

/**
 * Class for retrieving information about Zowe version from Zowe's manifest.json
 * and information about API ML version from build-info.properties and git.properties
 */

@Slf4j
@Service
public class VersionService {
    private final VersionInfo version;
    private final BuildInfo buildInfo;

    @Value("${apiml.zoweManifest:#{null}}")
    private String zoweManifest;
    @Value("${apiml.zoweManifestEncoding:IBM1047}")
    private String zoweManifestEncoding;

    public VersionService() {
        this.buildInfo = new BuildInfo();
        this.version = new VersionInfo();
    }

    public VersionService(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
        this.version = new VersionInfo();
    }

    /**
     * Getting the cached VersionInfo object, if it's empty it will be filled
     * @return filled VersionInfo object
     */
    public VersionInfo getVersion() {
        if (version.getApiml() == null) {
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
            version.setZowe(getZoweVersion(zoweManifest));
        }
        version.setApiml(getApimlVersion());
    }

    /**
     * Retrieving the information about API ML version from build-info.properties and git.properties files
     * @return the version, build and commit numbers in one string
     */
    private VersionInfoDetails getApimlVersion() {
        BuildInfoDetails buildInfoDetails = buildInfo.getBuildInfoDetails();
        return new VersionInfoDetails(buildInfoDetails.getVersion(), buildInfoDetails.getNumber(), buildInfoDetails.getCommitId());
    }

    /**
     * Retrieving the information about Zowe version from manifest.json file
     * @param manifestJsonFile the path to Zowe's manifest.json file
     * @return the version and build numbers in one string
     */
    private VersionInfoDetails getZoweVersion(String manifestJsonFile) {
        try {
            File file = ResourceUtils.getFile(manifestJsonFile);
            return readManifestJson(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            log.debug("Error in reading the file {}: {}", manifestJsonFile, e.getMessage());
        }
        return null;
    }

    private VersionInfoDetails readManifestJson(byte[] jsonInBytes) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        String manifestJson = new String(jsonInBytes);
        try {
            return retrieveZoweVersion(manifestJson, mapper);
        } catch (JsonProcessingException e) {
            try {
                //try mainframe encoding
                manifestJson = new String(jsonInBytes, Charset.forName(zoweManifestEncoding));
                return retrieveZoweVersion(manifestJson, mapper);
            } catch (JsonProcessingException ex) {
                log.debug("Error in parsing Zowe build manifest.json file: {}", e.getMessage());
            }
        }
        return null;
    }

    private VersionInfoDetails retrieveZoweVersion(String manifestJson, ObjectMapper mapper) throws JsonProcessingException {
        VersionInfoDetails zoweVersion = new VersionInfoDetails();
        ObjectNode manifestNode = mapper.readValue(manifestJson, ObjectNode.class);
        JsonNode versionNode = manifestNode.get("version");
        if (versionNode != null && !versionNode.asText().isEmpty()) {
            zoweVersion.setVersion(versionNode.asText());
        } else {
            zoweVersion.setVersion("Unknown");
        }
        retrieveZoweBuildNumber(manifestNode, zoweVersion);
        return zoweVersion;
    }

    private void retrieveZoweBuildNumber(ObjectNode manifestNode, VersionInfoDetails zoweVersion) {
        String buildNumber = "null";
        String commitHash = "Unknown";
        JsonNode buildNode = manifestNode.get("build");
        if (buildNode != null) {
            JsonNode buildNumberNode = buildNode.get("number");
            if (buildNumberNode != null && StringUtils.isNotEmpty(buildNumberNode.asText())) {
                buildNumber = buildNumberNode.asText();
            }
            JsonNode commitIdNode = buildNode.get("commitHash");
            if (commitIdNode != null && StringUtils.isNotEmpty(commitIdNode.asText())) {
                commitHash = commitIdNode.asText();
            }
        }
        zoweVersion.setBuildNumber(buildNumber);
        zoweVersion.setCommitHash(commitHash);
    }
}
