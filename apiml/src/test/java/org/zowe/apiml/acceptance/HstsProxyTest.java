/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.test.context.TestPropertySource;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

@AcceptanceTest
@TestPropertySource(properties = {
    "server.ssl.enabled=false",
    "server.attlsServer.enabled=false",
    "server.forward-headers-strategy=framework",
    "apiml.internal-discovery.port=40986",
    "apiml.security.domains.onlyWarn=true" // The proxy-to-application connection uses HTTP in this test.
})
class HstsProxyTest extends AcceptanceTestWithBasePath {

    @ParameterizedTest
    @CsvSource({"X-Forwarded-Proto,https", "Forwarded,proto=https"})
    void authenticationFailureIncludesHstsForForwardedHttps(String header, String value) {
        given().header(header, value)
            .when().get("http://localhost:" + port + "/gateway/api/v1/auth/query")
            .then().statusCode(401)
            .header("Strict-Transport-Security", equalTo("max-age=31536000; includeSubDomains"));
    }

    @Test
    void plaintextRequestWithoutForwardedHttpsHasNoHsts() {
        given().when().get("http://localhost:" + port + "/gateway/api/v1/auth/query")
            .then().statusCode(401)
            .header("Strict-Transport-Security", nullValue());
    }
}
