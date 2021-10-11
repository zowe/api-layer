/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.security.config.annotation.web.WebSecurityConfigurer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;
import org.zowe.apiml.apicatalog.services.status.model.ServiceNotFoundException;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {StaticAPIRefreshController.class},
    excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebSecurityConfigurer.class) },
    excludeAutoConfiguration = { SecurityAutoConfiguration.class}
)
@ContextConfiguration(classes = StaticApiContextConfiguration.class)
class StaticAPIRefreshControllerTest {

    private static final String API_REFRESH_ENDPOINT = "/static-api/refresh";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StaticAPIService staticAPIService;

    @Test
    void givenServiceNotFoundException_whenCallRefreshAPI_thenResponseShouldBe503WithSpecificMessage() throws Exception {
        when(staticAPIService.refresh()).thenThrow(
            new ServiceNotFoundException("Exception")
        );

        mockMvc.perform(post(API_REFRESH_ENDPOINT))
            .andExpect(jsonPath("$.messages", hasSize(1)))
            .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
            .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAC706E"))
            .andExpect(jsonPath("$.messages[0].messageContent").value("Service not located, discovery"))
            .andExpect(jsonPath("$.messages[0].messageKey").value("org.zowe.apiml.apicatalog.serviceNotFound"))
            .andExpect(status().isServiceUnavailable());
    }

    @Test
    void givenRestClientException_whenCallRefreshAPI_thenResponseShouldBe500WithSpecificMessage() throws Exception {
        when(staticAPIService.refresh()).thenThrow(
            new RestClientException("Exception")
        );

        mockMvc.perform(post(API_REFRESH_ENDPOINT))
            .andExpect(jsonPath("$.messages", hasSize(1)))
            .andExpect(jsonPath("$.messages[0].messageType").value("ERROR"))
            .andExpect(jsonPath("$.messages[0].messageNumber").value("ZWEAC707E"))
            .andExpect(jsonPath("$.messages[0].messageContent").value("Static API refresh failed, caused by exception: org.springframework.web.client.RestClientException: Exception"))
            .andExpect(jsonPath("$.messages[0].messageKey").value("org.zowe.apiml.apicatalog.StaticApiRefreshFailed"))
            .andExpect(status().isInternalServerError());
    }

    @Test
    void givenSuccessStaticResponse_whenCallRefreshAPI_thenResponseCodeShouldBe200() throws Exception {
        when(staticAPIService.refresh()).thenReturn(
            new StaticAPIResponse(200, "This is body")
        );

        mockMvc.perform(post(API_REFRESH_ENDPOINT))
            .andExpect(status().isOk());
    }

}
