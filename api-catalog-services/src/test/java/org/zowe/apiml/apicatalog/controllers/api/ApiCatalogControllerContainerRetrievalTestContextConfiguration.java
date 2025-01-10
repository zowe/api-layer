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

import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.zowe.apiml.apicatalog.controllers.handlers.ApiCatalogControllerExceptionHandler;
import org.zowe.apiml.apicatalog.services.cached.CachedProductFamilyService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.mockito.Mockito.*;

class ApiCatalogControllerContainerRetrievalTestContextConfiguration {

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
