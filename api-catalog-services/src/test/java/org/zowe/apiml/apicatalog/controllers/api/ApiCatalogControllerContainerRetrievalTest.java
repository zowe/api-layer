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

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = {ApiCatalogController.class},
    excludeFilters = { @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, value = WebSecurityConfigurer.class) },
    excludeAutoConfiguration = { SecurityAutoConfiguration.class}
)
@ContextConfiguration(classes = ApiCatalogControllerContainerRetrievalTestContextConfiguration.class)
class ApiCatalogControllerContainerRetrievalTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getContainers() throws Exception {
        this.mockMvc.perform(get("/containers"))
            .andExpect(status().is5xxServerError())
            .andExpect(jsonPath("$.messages[?(@.messageNumber == 'ZWEAC104E')].messageContent",
                hasItem("Could not retrieve container statuses, java.lang.NullPointerException")));
    }


}
