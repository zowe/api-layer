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

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.apicatalog.controllers.handlers.CatalogApiDocControllerExceptionHandler;
import org.zowe.apiml.apicatalog.exceptions.ServiceNotFoundException;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ApiDocController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
    ApiDocController.class,
    CatalogApiDocControllerExceptionHandler.class,
    ApiDocControllerServiceNotFoundTest.Context.class
})
@TestInstance(TestInstance.Lifecycle. PER_CLASS)
class ApiDocControllerServiceNotFoundTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApiDocService apiDocService;

    @BeforeAll
    void initApiDocRetrievalService() {
        when(apiDocService.retrieveApiDoc("service1", "v1"))
            .thenThrow(new ServiceNotFoundException("API Documentation not retrieved, The service is running."));
    }

    @Test
    void getApiDocForServiceDown() throws Exception {
        webTestClient.get().uri("/apicatalog/apidoc/service1/v1").exchange()
            .expectStatus().isNotFound()
            .expectBody().jsonPath("$.messages[?(@.messageNumber == 'ZWEAC706E')].messageContent")
                .value(contains("Service not located, API Documentation not retrieved, The service is running."));
    }

    static class Context {

        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/apicatalog-log-messages.yml");
        }

    }

}
