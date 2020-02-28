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

import java.io.File;
import java.io.IOException;

/**
 * Class for retrieving information about Zowe version from Zowe's manifest.json
 * and information about API ML version from build-info.properties and git.properties
 */

@Slf4j
@Service
public class FileBasedVersions implements Versions {
    private final VersionProducer apiMlVersion;
    private final VersionProducer zoweVersion;

    private VersionInfo cachedVersion;

    @Value("${apiml.zoweManifest:#{null}}")
    private String zoweManifest;

    public FileBasedVersions() {
        this.apiMlVersion = new ApiMlVersionProducer(new BuildInfo());
        this.zoweVersion = new ZoweVersionProducer(new File(zoweManifest));
    }

    public FileBasedVersions(VersionProducer apiMlVersion, VersionProducer zoweVersion) {
        this.apiMlVersion = apiMlVersion;
        this.zoweVersion = zoweVersion;
    }

    /**
     * Return the cached VersionInfo object, if it's empty it will be filled
     * @return filled VersionInfo object
     */
    public VersionInfo getVersion() {
        if(cachedVersion == null) {
            cachedVersion = new VersionInfo(
                zoweVersion.version(),
                apiMlVersion.version()
            );
        }

        return cachedVersion;
    }
}
