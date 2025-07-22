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
import org.zowe.apiml.apicatalog.controllers.handlers.ApiCatalogControllerExceptionHandler;
import org.zowe.apiml.apicatalog.swagger.ApiDocService;
import org.zowe.apiml.apicatalog.swagger.ContainerService;

import static org.hamcrest.Matchers.contains;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
    ServicesControllerMicroservice.class,
    ApiCatalogControllerExceptionHandler.class,
    BeanConfig.class
})
@WebFluxTest(controllers = ServicesControllerMicroservice.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ServicesControllerContainerRetrievalTest {

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private ApiDocService apiDocService;

    @MockitoBean
    private ContainerService containerService;

    @BeforeAll
    void initContainerService() {
        when(containerService.getAllContainers())
            .thenThrow(new NullPointerException());
    }

    @Test
    void getContainers() {
        webTestClient.get().uri("/apicatalog/containers").exchange()
            .expectStatus().is5xxServerError()
            .expectBody().jsonPath("$.messages[?(@.messageNumber == 'ZWEAC104E')].messageContent")
                .value(contains("Could not retrieve container statuses, java.lang.NullPointerException"));
    }

}
