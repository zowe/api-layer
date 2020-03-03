/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.product.version.VersionInfo;
import org.zowe.apiml.product.version.VersionInfoDetails;
import org.zowe.apiml.product.version.VersionService;

import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class VersionControllerTest {
    @Mock
    private VersionService versionService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        VersionController versionController = new VersionController(versionService);
        mockMvc = MockMvcBuilders.standaloneSetup(versionController).build();
    }

    @Test
    public void givenSpecificVersions_whenVersionEndpointCalled_thenVersionInfoShouldBeGivenInSuccessfulResponse() throws Exception {
        Mockito.when(versionService.getVersion()).thenReturn(getDummyVersionInfo());
        this.mockMvc.perform(get("/version"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zowe.version", is("0.0.0")))
            .andExpect(jsonPath("$.zowe.buildNumber", is("000")))
            .andExpect(jsonPath("$.zowe.commitHash", is("1a3b5c7")))

            .andExpect(jsonPath("$.apiml.version", is("0.0.0")))
            .andExpect(jsonPath("$.apiml.buildNumber", is("000")))
            .andExpect(jsonPath("$.apiml.commitHash", is("1a3b5c7")));
    }

    private VersionInfo getDummyVersionInfo() {
        VersionInfo versionInfo = new VersionInfo();
        VersionInfoDetails versionInfoDetails = new VersionInfoDetails("0.0.0", "000", "1a3b5c7");
        versionInfo.setZowe(versionInfoDetails);
        versionInfo.setApiml(versionInfoDetails);
        return versionInfo;
    }
}
