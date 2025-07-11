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
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
    ApiDocController.class,
    CatalogApiDocControllerExceptionHandler.class,
    ApiDocControllerApiDocNotFoundTest.Context.class
})
@WebFluxTest(controllers = ApiDocController.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@TestInstance(TestInstance.Lifecycle. PER_CLASS)
class ApiDocControllerApiDocNotFoundTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApiDocService apiDocService;

    @BeforeAll
    void initApiDocRetrievalService() {
        when(apiDocService.retrieveApiDoc("service2", "v1"))
            .thenThrow(new ApiDocNotFoundException("Really bad stuff happened"));

        when(apiDocService.retrieveApiDoc("service2", null))
            .thenThrow(new ApiDocNotFoundException("Really bad stuff happened"));
    }

    @Test
    void getApiDocAndFailThenThrowApiDocNotFoundException() {
        webTestClient.get().uri("/apicatalog/apidoc/service2/v1").exchange()
            .expectStatus().isNotFound()
            .expectBody().jsonPath("$.messages[?(@.messageNumber == 'ZWEAC103E')].messageContent")
                .value(contains("API Documentation not retrieved, Really bad stuff happened"));
    }

    static class Context {

        @Bean
        public MessageService messageService() {
            return new YamlMessageService("/apicatalog-log-messages.yml");
        }

    }

}
