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
