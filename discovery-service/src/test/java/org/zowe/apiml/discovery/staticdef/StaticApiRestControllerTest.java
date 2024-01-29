/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.staticdef;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Base64;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class StaticApiRestControllerTest {

    private static final String CREDENTIALS = "eureka:password";

    List<InstanceInfo> instancesInfo;
    private MockMvc mockMvc;
    String serviceName = "service";

    @BeforeEach
    public void setup() {

        registrationService = mock(StaticServicesRegistrationService.class);
        instancesInfo = Collections.singletonList(
            InstanceInfo.Builder.newBuilder()
                .setAppName(serviceName)
                .build()
        );
        mockMvc = standaloneSetup(new StaticApiRestController(registrationService)).build();
    }


    private StaticServicesRegistrationService registrationService;

    @Test
    void listDefinitions() throws Exception {
        String serviceName = "service";
        String basicToken = "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes());

        when(registrationService.getStaticInstances()).thenReturn(instancesInfo);
        this.mockMvc.perform(get("/discovery/api/v1/staticApi").header("Authorization", basicToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].app", hasItem(serviceName.toUpperCase())));

        verify(registrationService, times(1)).getStaticInstances();
    }

    @Test
    void reloadDefinitions() throws Exception {

        String basicToken = "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes());

        StaticRegistrationResult result = new StaticRegistrationResult();

        result.getInstances().addAll(instancesInfo);

        when(registrationService.reloadServices()).thenReturn(result);

        this.mockMvc.perform(post("/discovery/api/v1/staticApi").header("Authorization", basicToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.instances[*].app", hasItem(serviceName.toUpperCase())));

        verify(registrationService, times(1)).reloadServices();
    }
}
