/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.staticdef;

import com.netflix.appinfo.InstanceInfo;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Base64;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest
public class StaticApiRestControllerTest {
    private static final String CREDENTIALS = "eureka:password";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StaticServicesRegistrationService registrationService;

    @Test
    public void listDefinitions() throws Exception {
        String serviceName = "service";
        String basicToken = "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes());

        List<InstanceInfo> instancesInfo = Arrays.asList(
            InstanceInfo.Builder.newBuilder()
                .setAppName(serviceName)
                .build()
        );
        when(registrationService.getStaticInstances()).thenReturn(instancesInfo);


        this.mockMvc.perform(get("/discovery/api/v1/staticApi").header("Authorization", basicToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].app", hasItem(serviceName.toUpperCase())));

        verify(registrationService, times(1)).getStaticInstances();
    }

    @Test
    public void reloadDefinitions() throws Exception {
        String serviceName = "service";
        String basicToken = "Basic " + Base64.getEncoder().encodeToString(CREDENTIALS.getBytes());

        List<InstanceInfo> instancesInfo = Arrays.asList(
            InstanceInfo.Builder.newBuilder()
                .setAppName(serviceName)
                .build()
        );

        when(registrationService.getStaticInstances()).thenReturn(instancesInfo);

        this.mockMvc.perform(post("/discovery/api/v1/staticApi").header("Authorization", basicToken))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].app", hasItem(serviceName.toUpperCase())));

        verify(registrationService, times(1)).reloadServices();
        verify(registrationService, times(1)).getStaticInstances();
    }
}
