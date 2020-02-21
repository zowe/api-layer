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
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.util.FileUtils;

import java.io.IOException;

@Slf4j
@Service
public class VersionService {
    private static final String NO_VERSION = "Build information is not available";

    @Value("${apiml.zoweManifest:#{null}}")
    private String zoweManifest;

    private static final VersionInfo version = new VersionInfo();

    public VersionInfo getVersion() {
        if (version.getApimlVersion() == null) {
            updateVersionInfo();
        }
        return version;
    }

    public void updateVersionInfo() {
        if (StringUtils.isNotEmpty(zoweManifest)) {
            version.setZoweVersion(getZoweVersion(zoweManifest));
        }
        version.setApimlVersion(getApimlVersion());
    }

    private String getApimlVersion() {
        BuildInfoDetails buildInfo = new BuildInfo().getBuildInfoDetails();
        String apimlVersion = NO_VERSION;
        if (!buildInfo.getVersion().equalsIgnoreCase("unknown")) {
            apimlVersion = String.format("%s build #%s (%s)", buildInfo.getVersion(), buildInfo.getNumber(), buildInfo.getCommitId());
        }
        return apimlVersion;
    }

    private String getZoweVersion(String manifestJsonFile) {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        try {
            String manifestJson = FileUtils.readFile(manifestJsonFile);
            ObjectNode objectNode = mapper.readValue(manifestJson, ObjectNode.class);
            JsonNode versionNode = objectNode.get("version");
            if (versionNode != null && StringUtils.isNotEmpty(versionNode.asText())) {
                StringBuilder zoweVersion = new StringBuilder();
                zoweVersion.append(versionNode.asText());
                zoweVersion.append(" build #");
                JsonNode buildNode = objectNode.get("build");
                if (buildNode != null) {
                    JsonNode buildNumberNode = buildNode.get("number");
                    if (buildNumberNode != null && StringUtils.isNotEmpty(buildNumberNode.asText())) {
                        zoweVersion.append(buildNumberNode.asText());
                    } else {
                        zoweVersion.append("n/a");
                    }
                }
                return zoweVersion.toString();
            }
        } catch (IOException e) {
            log.debug("Error in reading the file {}: {}", manifestJsonFile, e.getMessage());
        }
        return NO_VERSION;
    }
}
