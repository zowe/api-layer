/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.zowe.apiml.gateway.GatewayServiceApplication;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTest;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MockService;
import reactor.core.publisher.Mono;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.atomic.AtomicReference;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;

@AcceptanceTest
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("gatewayExceptionHandlerTest")
@SpringBootTest(classes = GatewayServiceApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"management.server.port=10987"})
class GatewayExceptionHandlerTest extends AcceptanceTestWithMockServices {

    private static final AtomicReference<Exception> mockException = new AtomicReference<>();

    @BeforeAll
    void createAllZaasServices() {
        mockService("serv1ce").scope(MockService.Scope.CLASS).start();
    }

    @ParameterizedTest
    @CsvSource({
        "400,org.zowe.apiml.common.badRequest",
        "401,org.zowe.apiml.common.unauthorized",
        "403,org.zowe.apiml.security.forbidden",
        "404,org.zowe.apiml.common.notFound",
        "405,org.zowe.apiml.common.methodNotAllowed",
        "415,org.zowe.apiml.common.unsupportedMediaType",
        "500,org.zowe.apiml.common.internalServerError",
        "503,org.zowe.apiml.common.serviceUnavailable",
    })
    void givenErrorResponse_whenCallGateway_thenDecorateIt(int code, String messageKey) throws MalformedURLException {
        mockException.set(WebClientResponseException.create(code, "msg", null, null, null));

        given().when()
            .get(new URL(basePath + "/serv1ce/api/v1/test"))
            .then()
            .statusCode(code)
            .body("messages[0].messageKey", containsString(messageKey));
    }

    @Configuration
    @Profile("gatewayExceptionHandlerTest")
    static class Config {

        @Bean
        GlobalFilter exceptionFilter() {
            return (exchange, chain) -> Mono.error(mockException.get());
        }

    }

}
