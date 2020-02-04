/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.controllers.api;

import org.zowe.apiml.apicatalog.controllers.handlers.ApiCatalogControllerExceptionHandler;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasItem;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@WebMvcTest(controllers = {ApiCatalogController.class}, secure = false)
public class ApiCatalogControllerContainerRetrievalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void getContainers() throws Exception {
        this.mockMvc.perform(get("/containers"))
            .andExpect(status().is5xxServerError())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'ZWEAC104E')].messageContent",
                hasItem("Could not retrieve container statuses, java.lang.NullPointerException")));
    }

    @Configuration
    static class ContextConfiguration {

        @MockBean
        private CachedProductFamilyService cachedProductFamilyService;

        @Bean
        public ApiCatalogController apiCatalogController() {
            when(cachedProductFamilyService.getAllContainers())
                .thenThrow(new NullPointerException());

            verify(cachedProductFamilyService, never()).getAllContainers();

            return new ApiCatalogController(cachedProductFamilyService, null);
        }

        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/apicatalog-log-messages.yml");
        }

        @Bean
        public ApiCatalogControllerExceptionHandler apiCatalogControllerExceptionHandler() {
            return new ApiCatalogControllerExceptionHandler(messageService());
        }
    }
}
