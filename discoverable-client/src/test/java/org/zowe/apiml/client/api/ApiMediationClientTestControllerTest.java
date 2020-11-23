/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.api;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;
import org.zowe.apiml.client.service.ApiMediationClientService;

import static junit.framework.TestCase.assertFalse;
import static org.hamcrest.core.Is.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {ApiMediationClientTestController.class})
public class ApiMediationClientTestControllerTest {
    private static final String MEDIATION_CLIENT_URI = "/api/v1/apiMediationClient";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ApiMediationClientService apiMediationClientService;

    @Test
    public void registrationTest_successful() throws Exception {
        this.mockMvc.perform(
            post(MEDIATION_CLIENT_URI))
            .andExpect(status().isOk());
    }

    @Test
    public void unregisterTest_successful() throws Exception {
        apiMediationClientService.register();
        this.mockMvc.perform(
            delete(MEDIATION_CLIENT_URI))
            .andExpect(status().isOk());
    }

    @Test
    public void isRegisteredTest_notRegistered() throws Exception {
        this.mockMvc.perform(
            get(MEDIATION_CLIENT_URI))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.isRegistered", is(false)));
    }

    @Test
    public void isRegisteredTestService_notRegistered() {
        assertFalse(apiMediationClientService.isRegistered());
    }
}
