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

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;

public class ZoweVersionProducer implements VersionProducer {
    private File zoweManifest;

    public ZoweVersionProducer(File zoweManifest) {
        this.zoweManifest = zoweManifest;
    }

    @Override
    public Version version() {
        ObjectMapper mapper = new ObjectMapper();
        try {
            ZoweManifestBuildInfo info = mapper.readValue(zoweManifest, ZoweManifestBuildInfo.class);
            Build buildInformation = info.getBuild();
            return new Version(info.getVersion(), buildInformation.getNumber(), buildInformation.getCommitHash());
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
