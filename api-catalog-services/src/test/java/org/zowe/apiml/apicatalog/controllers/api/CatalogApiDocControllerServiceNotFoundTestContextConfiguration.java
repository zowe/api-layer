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
import org.zowe.apiml.apicatalog.controllers.handlers.CatalogApiDocControllerExceptionHandler;
import org.zowe.apiml.apicatalog.services.status.APIServiceStatusService;
import org.zowe.apiml.apicatalog.services.status.model.ServiceNotFoundException;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.mockito.Mockito.*;

class CatalogApiDocControllerServiceNotFoundTestContextConfiguration {

    @MockBean
    private APIServiceStatusService apiServiceStatusService;

    @Bean
    public CatalogApiDocController catalogApiDocController() {
        when(apiServiceStatusService.getServiceCachedApiDocInfo("service1", "v1"))
            .thenThrow(new ServiceNotFoundException("API Documentation not retrieved, The service is running."));

        verify(apiServiceStatusService, never()).getServiceCachedApiDocInfo("service1", "v1");

        return new CatalogApiDocController(apiServiceStatusService);
    }

    @Bean
    public MessageService messageService() {
        return new YamlMessageService("/apicatalog-log-messages.yml");
    }

    @Bean
    public CatalogApiDocControllerExceptionHandler catalogApiDocControllerExceptionHandler() {
        return new CatalogApiDocControllerExceptionHandler(messageService());
    }
}
