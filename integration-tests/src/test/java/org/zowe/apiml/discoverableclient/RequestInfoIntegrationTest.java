/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discoverableclient;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.service.DiscoveryUtils;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

/**
 *
 */
public class RequestInfoIntegrationTest {

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    private static String getUrl() {
        return
            DiscoveryUtils.getInstances("dcbypass").get(0).getUrl() +
            "/discoverableclient/api/v1/request";
    }

    private static String getGatewayUrl() {
        return HttpRequestUtils.getUriFromGateway("/api/v1/dcbypass/request").toString();
    }

    public static Stream<Arguments> getInputs() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        return Stream.of(
            Arguments.of("call DiscoverableClient with sign", getUrl(), Boolean.TRUE),
            Arguments.of("call DiscoverableClient without sign", getUrl(), Boolean.FALSE),
            Arguments.of("call DiscoverableClient through Gateway with sign", getGatewayUrl(), Boolean.TRUE),
            Arguments.of("call DiscoverableClient through Gateway without sign", getGatewayUrl(), Boolean.FALSE)
        );
    }

    @ParameterizedTest(name = "call endpoint {1} to receive json with signed = {2} : {0}")
    @MethodSource("getInputs")
    public void testRequestInfo(String description, String url, Boolean signed) {
        if (signed) {
            RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());
        }

        given()
        .when()
            .get(url)
        .then()
            .statusCode(SC_OK)
            .body("signed", equalTo(signed));
    }

}
