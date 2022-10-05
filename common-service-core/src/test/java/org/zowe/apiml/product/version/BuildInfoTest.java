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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BuildInfoTest {

    @Test
    void givenProperties_thenReturnBuildInfoDetails() {
        BuildInfo buildInfo = new BuildInfo();
        BuildInfoDetails details = buildInfo.getBuildInfoDetails();
        assertEquals("service-name", details.getArtifact());
    }
    @Test
    void givenMissingProperties_thenReturnEmptyBuildInfoDetails() {
        BuildInfo buildInfo = new BuildInfo("missing/build","missing/git");
        BuildInfoDetails details = buildInfo.getBuildInfoDetails();
        assertEquals("Unknown",details.getArtifact());
    }
}
