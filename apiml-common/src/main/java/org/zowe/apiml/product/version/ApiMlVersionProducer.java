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

public class ApiMlVersionProducer implements VersionProducer {
    public BuildInfo buildInfo;

    public ApiMlVersionProducer(BuildInfo buildInfo) {
        this.buildInfo = buildInfo;
    }

    @Override
    public Version version() {
        BuildInfoDetails buildInfoDetails = buildInfo.getBuildInfoDetails();
        if (buildInfoDetails.getVersion().equalsIgnoreCase("unknown")) {
            return null;
        }

        return new Version(buildInfoDetails.getVersion(), buildInfoDetails.getNumber(), buildInfoDetails.getCommitId());
    }
}
