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
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.config.CloudGatewayConfiguration;
import org.zowe.apiml.util.config.ConfigReader;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_GREET;

@Tag("CloudGatewayProxyTest")
class CloudGatewayProxyTest {
    private static final int SECOND = 1000;
    private static final int DEFAULT_TIMEOUT = 2 * SECOND;

    static CloudGatewayConfiguration conf = ConfigReader.environmentConfiguration().getCloudGatewayConfiguration();

    @Test
    void givenRequestHeader_thenRouteToProvidedHost() throws URISyntaxException {
        RestAssured.useRelaxedHTTPSValidation();

        String scgUrl = String.format("%s://%s:%s/%s", conf.getScheme(), conf.getHost(), conf.getPort(), "gateway/version");
        given().header("X-Request-Id", "apiml1")
            .get(new URI(scgUrl)).then().statusCode(200);
        given().header("X-Request-Id", "apiml1")
            .get(new URI(scgUrl)).then().statusCode(200);
    }

    @Test
    void givenRequestTimeoutIsReached_thenDropConnection() {
        RestAssured.useRelaxedHTTPSValidation();
        String scgUrl = String.format("%s://%s:%s%s?%s=%d", conf.getScheme(), conf.getHost(), conf.getPort(), DISCOVERABLE_GREET, "delayMs", DEFAULT_TIMEOUT + SECOND);
        assertTimeout(Duration.ofMillis(DEFAULT_TIMEOUT * 3), () -> {
            given()
                .header("X-Request-Id", "discoverableclient")
            .when()
                .get(scgUrl)
            .then()
                .statusCode(HttpStatus.SC_GATEWAY_TIMEOUT);
        });
    }
}
