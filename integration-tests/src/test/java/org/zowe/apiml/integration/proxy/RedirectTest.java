/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import io.restassured.RestAssured;
import io.restassured.http.ContentType;
import lombok.Data;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;

@DiscoverableClientDependentTest
public class RedirectTest {

    GatewayServiceConfiguration gwConf = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

    @BeforeAll
    static void init() throws Exception {
        RestAssured.useRelaxedHTTPSValidation();
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());

    }

    static Stream<Arguments> headerValues() {
        return Stream.of(
            Arguments.of("absolut URL with encoding doesn't match service route", "%2Fapi%2Frequest", "%2Fapi%2Frequest"),
            Arguments.of("absolut URL doesn't match service route", "/api/request", "/api/request"),
            Arguments.of("relative URL", "api/request", "api/request"),
            Arguments.of("absolut URL containing encoded characters matches service route", "/discoverableclient/api/v1/login?returnUrl=%2Fapi%2Frequest", "https://localhost:10010/discoverableclient/api/v1/login?returnUrl=%2Fapi%2Frequest"),
            Arguments.of("relative URL that contains service ID", "discoverableclient/api/v1/login?returnUrl=%2Fapi%2Frequest", "discoverableclient/api/v1/login?returnUrl=%2Fapi%2Frequest"),
            Arguments.of("absolut URL matches service route", "/discoverableclient/api/v1/request", "https://localhost:10010/discoverableclient/api/v1/request"),
            Arguments.of("Full URL contains service host and port", "https://localhost:10012/discoverableclient/api/v1/request", "https://localhost:10010/discoverableclient/api/v1/request"),
            Arguments.of("Full URL contains gateway host and port", "https://localhost:10010/discoverableclient/api/v3/request", "https://localhost:10010/discoverableclient/api/v3/request"),
            Arguments.of("scheme-relative URL contains service host and port", "//localhost:10012/discoverableclient/api/v1/request", "//localhost:10010/discoverableclient/api/v1/request"),
            Arguments.of("scheme-relative URL contains gateway host and port", "//localhost:10010/discoverableclient/api/v1/request", "//localhost:10010/discoverableclient/api/v1/request")
        );
    }

    @ParameterizedTest(name = "given {0} then Location header value {1} was transform to {2}")
    @MethodSource("headerValues")
    void giveLocationHeaderFromService(String msg, String original, String translated) {
        var baseUrl = String.format("%s://%s:%d", gwConf.getScheme(), gwConf.getHost(), gwConf.getPort());
        var targetUrl = baseUrl + "/discoverableclient/api/v1/redirect";
        given()
            .body(new LocationReq(original))
            .contentType(ContentType.JSON)
            .post(targetUrl)
            .then().log().ifValidationFails()
            .header("Location", translated)
            .statusCode(307);
    }

    @Data
    static class LocationReq {
        final String location;
    }
}
