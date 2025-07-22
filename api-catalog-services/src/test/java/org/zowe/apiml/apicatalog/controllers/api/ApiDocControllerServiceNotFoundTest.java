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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.apicatalog.config.BeanConfig;
import org.zowe.apiml.apicatalog.controllers.handlers.CatalogApiDocControllerExceptionHandler;
import org.zowe.apiml.apicatalog.exceptions.ServiceNotFoundException;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;

@WebFluxTest(controllers = ApiDocControllerMicroservice.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ContextConfiguration(classes = {
    ApiDocControllerMicroservice.class,
    CatalogApiDocControllerExceptionHandler.class,
    BeanConfig.class
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
    void getApiDocForServiceDown() {
        webTestClient.get().uri("/apicatalog/apidoc/service1/v1").exchange()
            .expectStatus().isNotFound()
            .expectBody().jsonPath("$.messages[?(@.messageNumber == 'ZWEAC706E')].messageContent")
                .value(contains("Service not located, API Documentation not retrieved, The service is running."));
    }

}
