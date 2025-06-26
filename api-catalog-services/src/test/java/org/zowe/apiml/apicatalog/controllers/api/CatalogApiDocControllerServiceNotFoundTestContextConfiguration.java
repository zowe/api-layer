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

import org.springframework.context.annotation.Bean;
import org.zowe.apiml.apicatalog.controllers.handlers.CatalogApiDocControllerExceptionHandler;
import org.zowe.apiml.apicatalog.services.status.ApiDocRetrievalService;
import org.zowe.apiml.apicatalog.services.status.OpenApiCompareProducer;
import org.zowe.apiml.apicatalog.services.status.model.ServiceNotFoundException;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.mockito.Mockito.*;

class CatalogApiDocControllerServiceNotFoundTestContextConfiguration {

    @Bean
    public ApiDocRetrievalService apiServiceStatusService() {
        return mock(ApiDocRetrievalService.class);
    }

    @Bean
    public OpenApiCompareProducer openApiCompareProducer() {
        return mock(OpenApiCompareProducer.class);
    }

    @Bean
    public ApiDocController catalogApiDocController(ApiDocRetrievalService apiServiceStatusService, OpenApiCompareProducer openApiCompareProducer) {
        when(apiServiceStatusService.retrieveApiDoc("service1", "v1"))
            .thenThrow(new ServiceNotFoundException("API Documentation not retrieved, The service is running."));

        verify(apiServiceStatusService, never()).retrieveApiDoc("service1", "v1");

        return new ApiDocController(apiServiceStatusService, openApiCompareProducer);
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
