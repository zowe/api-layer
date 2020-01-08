/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.api;

import com.ca.mfaas.client.configuration.ApplicationConfiguration;
import com.ca.mfaas.client.configuration.SpringComponentsConfiguration;
import com.ca.mfaas.client.model.RedirectLocation;
import com.ca.mfaas.product.registry.EurekaClientWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {PageRedirectionController.class}, secure = false)
@Import(value = {SpringComponentsConfiguration.class, ApplicationConfiguration.class, EurekaClientWrapper.class})
public class PageRedirectionControllerTest {
    private final ObjectMapper mapper = new ObjectMapper();
    @Autowired
    private MockMvc mockMvc;

    @Test
    public void redirectToLocation() throws Exception {
        RedirectLocation redirectLocation = new RedirectLocation("https://hostA:8080/some/path");
        String payload = mapper.writeValueAsString(redirectLocation);

        this.mockMvc.perform(
            post("/api/v1/redirect")
                .contentType(MediaType.APPLICATION_JSON_UTF8)
                .content(payload))
            .andExpect(status().isTemporaryRedirect())
            .andExpect(redirectedUrl(redirectLocation.getLocation()));
    }

}
