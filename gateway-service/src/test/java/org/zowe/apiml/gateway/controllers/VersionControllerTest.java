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
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.product.version.FileBasedVersions;
import org.zowe.apiml.product.version.Version;
import org.zowe.apiml.product.version.VersionInfo;

import java.util.Collections;

import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
public class VersionControllerTest {
    @Mock
    private FileBasedVersions versionService;

    private MockMvc mockMvc;

    @Before
    public void setUp() {
        VersionController versionController = new VersionController(versionService);
        mockMvc = MockMvcBuilders.standaloneSetup(versionController).build();
    }

    /**
     * The Information is available
         {
             zowe: {
                 version: 1.9.0,
                 buildNumber: 123
             },
             apiMl: {
                 version: 1.3.0,
                 buildNumber: 123,
                 commitHash: '1a3b5c7'
             }
         }
     */
    @Test
    public void givenSpecificVersions_whenVersionEndpointCalled_thenVersionInfoShouldBeGivenInSuccessfulResponse() throws Exception {
        when(versionService.getVersion()).thenReturn(new VersionInfo(
            new Version("1.9.0", "124", null),
            new Version("1.3.0", "2060", "1a3b6c7")
        ));

        this.mockMvc.perform(get("/version"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.zowe.version", is("1.9.0")))
            .andExpect(jsonPath("$.zowe.buildNumber", is("124")))

            .andExpect(jsonPath("$.apiMl.version", is("1.3.0")))
            .andExpect(jsonPath("$.apiMl.buildNumber", is("2060")))
            .andExpect(jsonPath("$.apiMl.commitHash", is("1a3b6c7")));
    }

    /**
    No version information is available
    {}
     */
    @Test
    public void givenNoVersionsAreAvailable_whenTheVersionIsRequested_thenThisInformationNeedsToBeReturned() throws Exception {
        when(versionService.getVersion()).thenReturn(new VersionInfo(null, null));
        mockMvc.perform(get("/version"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", is(Collections.EMPTY_MAP)));

    }
}
