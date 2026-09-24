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

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.autoconfigure.web.reactive.ReactiveWebSecurityAutoConfiguration;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.client.RestClientException;
import org.zowe.apiml.apicatalog.config.BeanConfig;
import org.zowe.apiml.apicatalog.controllers.handlers.StaticAPIRefreshControllerExceptionHandler;
import org.zowe.apiml.apicatalog.controllers.handlers.StaticDefinitionControllerExceptionHandler;
import org.zowe.apiml.apicatalog.exceptions.ServiceNotFoundException;
import org.zowe.apiml.apicatalog.staticapi.StaticAPIResponse;
import org.zowe.apiml.apicatalog.staticapi.StaticRegistrationServiceRest;
import org.zowe.apiml.apicatalog.staticapi.StaticDefinitionGenerator;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {
    StaticAPIRefreshControllerMicroservice.class,
    StaticDefinitionControllerMicroservice.class,
    StaticAPIRefreshControllerExceptionHandler.class,
    StaticDefinitionControllerExceptionHandler.class,
    BeanConfig.class
})
@WebFluxTest(controllers = {StaticAPIRefreshControllerMicroservice.class, StaticDefinitionControllerMicroservice.class}, excludeAutoConfiguration = ReactiveWebSecurityAutoConfiguration.class)
class StaticAPIRefreshControllerTest {

    private static final String API_REFRESH_ENDPOINT = "/apicatalog/static-api/refresh";

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private StaticRegistrationServiceRest staticServiceRest;

    @MockitoBean
    private StaticDefinitionGenerator staticDefinitionGenerator;

    @Test
    void givenServiceNotFoundException_whenCallRefreshAPI_thenResponseShouldBe503WithSpecificMessage() {
        when(staticServiceRest.refresh()).thenThrow(
            new ServiceNotFoundException("Exception")
        );

        webTestClient.post().uri(API_REFRESH_ENDPOINT).exchange()
            .expectStatus().isEqualTo(HttpStatus.SC_SERVICE_UNAVAILABLE)
            .expectBody()
                .jsonPath("$.messages").value(List.class, messages -> assertThat(messages).hasSize(1))
                .jsonPath("$.messages[0].messageType").isEqualTo("ERROR")
                .jsonPath("$.messages[0].messageNumber").isEqualTo("ZWEAC706E")
                .jsonPath("$.messages[0].messageContent").isEqualTo("Service not located, discovery")
                .jsonPath("$.messages[0].messageKey").isEqualTo("org.zowe.apiml.apicatalog.serviceNotFound");
    }

    @Test
    void givenRestClientException_whenCallRefreshAPI_thenResponseShouldBe500WithSpecificMessage() {
        when(staticServiceRest.refresh()).thenThrow(
            new RestClientException("Exception")
        );

        webTestClient.post().uri(API_REFRESH_ENDPOINT).exchange()
            .expectStatus().isEqualTo(HttpStatus.SC_INTERNAL_SERVER_ERROR)
            .expectBody()
                .jsonPath("$.messages").value(List.class, messages -> assertThat(messages).hasSize(1))
                .jsonPath("$.messages[0].messageType").isEqualTo("ERROR")
                .jsonPath("$.messages[0].messageNumber").isEqualTo("ZWEAC707E")
                .jsonPath("$.messages[0].messageContent").isEqualTo("Static API refresh failed, caused by exception: org.springframework.web.client.RestClientException: Exception")
                .jsonPath("$.messages[0].messageKey").isEqualTo("org.zowe.apiml.apicatalog.StaticApiRefreshFailed");
    }

    @Test
    void givenSuccessStaticResponse_whenCallRefreshAPI_thenResponseCodeShouldBe200() {
        when(staticServiceRest.refresh()).thenReturn(
            Mono.just(new StaticAPIResponse(200, "This is body"))
        );

        webTestClient.post().uri(API_REFRESH_ENDPOINT).exchange()
            .expectStatus().isOk();
    }

}
