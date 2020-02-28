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


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

public class ApiMlVersionProducerTest {
    private BuildInfo buildInfo;
    private ApiMlVersionProducer underTest;

    @BeforeEach
    public void prepareTestedInstance() {
        buildInfo = Mockito.mock(BuildInfo.class);

        underTest = new ApiMlVersionProducer(buildInfo);
    }

    @Test
    public void givenValidBuildInfo_whenApiMlVersionIsRequested_thenValidVersionIsProvided() {
        Properties buildProperties = new Properties();
        buildProperties.setProperty("build.version", "1.3.0");
        buildProperties.setProperty("build.number", "123");

        Properties gitProperties = new Properties();
        gitProperties.setProperty("git.commit.id.abbrev", "1a3b5c7");

        when(buildInfo.getBuildInfoDetails()).thenReturn(new BuildInfoDetails(buildProperties, gitProperties));

        Version valid = underTest.version();
        assertThat(valid, is(new Version("1.3.0", "123", "1a3b5c7")));
    }
}
