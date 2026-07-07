/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance.corsTests;

import com.google.common.net.HttpHeaders;
import com.sun.net.httpserver.Headers;
import com.sun.net.httpserver.HttpExchange;
import io.restassured.http.Header;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService.MockServiceBuilder;
import org.zowe.apiml.gateway.MockService.Scope;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTestWithMockServices;
import org.zowe.apiml.gateway.acceptance.common.MicroservicesAcceptanceTest;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

@MicroservicesAcceptanceTest
@ActiveProfiles({"CorsPerServiceTestWithDefaults", "test"})
@TestPropertySource(properties = {
    "apiml.service.corsEnabled=true",
    "apiml.service.corsDefaultAllowedOrigins="
})
class CorsPerServiceTestWithDefaults extends AcceptanceTestWithMockServices {

    private MockServiceBuilder mockCorsService(String serviceId, Headers responseHeaders, Map<String, String> metadata, Collection<Consumer<HttpExchange>> assertions) {
        var builder = mockService(serviceId);
        var endpointBuilder = builder.addEndpoint("/" + serviceId + "/fullheaders");

        if (responseHeaders != null) {
            endpointBuilder.headers(responseHeaders);
        }
        if (metadata != null) {
            builder.additionalMetadata(metadata);
        }
        if (assertions != null) {
            endpointBuilder.assertions(assertions);
        }

        return endpointBuilder
            .and()
        .scope(Scope.TEST);
    }

    @Test
    void givenCorsIsDelegatedToGatewayButServiceDoesntAllowCors_whenSimpleCorsRequestArrives_thenReject() throws Exception {
        var headers = new Headers();
        var called = new AtomicBoolean(false);
        List<Consumer<HttpExchange>> assertions = List.of(
                httpExchange -> {
                    assertNull(httpExchange.getRequestHeaders().get(HttpHeaders.ORIGIN));
                    called.set(true);
                }
            );
        mockCorsService("servicecors5", headers, Map.of("apiml.corsEnabled", "false"), assertions).start();

        given()
            .header(new Header(HttpHeaders.ORIGIN, "https://foo.bar.org"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
            .header(new Header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "origin, x-requested-with"))
            .log().all()
        .when()
            .post(basePath + "/servicecors5/api/v1/fullheaders")
        .then()
        .log().all()
            .statusCode(is(SC_FORBIDDEN))
            .header(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, is(nullValue()));

        assertFalse(called.get());
    }

}
