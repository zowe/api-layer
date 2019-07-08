/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.service;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class BuildInfo {

    public void logBuildInfo() {
        BuildInfoDetails buildInfo = getBuildInfoDetails();
        log.info("Service {} version {} #{} on {} by {} commit {}", buildInfo.getArtifact(), buildInfo.getVersion(), buildInfo.getNumber(),
            buildInfo.getTime(), buildInfo.getMachine(), buildInfo.getCommitId());
    }

    public BuildInfoDetails getBuildInfoDetails() {
        Properties build = getProperties("META-INF/build-info.properties");
        Properties git = getProperties("META-INF/git.properties");
        return new BuildInfoDetails(build, git);
    }

    private Properties getProperties(String path) {
        // Create the Properties
        Properties props = new Properties();

        // Create the input streams
        try (InputStream input = getClass().getClassLoader().getResourceAsStream(path)) {
            if (input == null) {
                log.error("Could not read properties from: {}", path);
                return props;
            }

            props.load(input);
        } catch (IOException ioe) {
            log.error("Error reading properties from: {} Details: {}", path, ioe.toString());
        }

        return props;
    }
}
