/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.apache.http.HttpHeaders;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.core.Is.is;

@ActiveProfiles("http")
class HttpSecuredEndpointTest extends DiscoveryFunctionalTest {

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Test
    void uiIsSecuredWithConfiguredBasicAuth() {
        given()
            .get(getDiscoveryUriWithPath("/"))
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());

        given().auth().basic(eurekaUserid, eurekaPassword)
            .get(getDiscoveryUriWithPath("/"))
            .then()
            .statusCode(HttpStatus.OK.value());
    }

    @Nested
    class GivenApplicationEndpoints {
        @Test
        void applicationInfoEndpointsWhenProvidedNothing() {
            given()
                .when()
                .get(getDiscoveryUriWithPath("/application/info"))
                .then()
                .statusCode(is(org.apache.http.HttpStatus.SC_OK));
        }

        @Test
        void applicationHealthEndpointsWhenProvidedNothing() {
            given()
                .when()
                .get(getDiscoveryUriWithPath("/application/health"))
                .then()
                .statusCode(is(org.apache.http.HttpStatus.SC_OK));
        }
    }

    @ParameterizedTest(name = "givenATTLS_testApplicationBeansEndpoints_Get {index} {0} ")
    @ValueSource(strings = {"/application/beans", "/"})
    void givenATTLS_testApplicationBeansEndpoints_Get(String path) {
        given()
            .when()
            .get(getDiscoveryUriWithPath(path))
            .then()
            .statusCode(is(org.apache.http.HttpStatus.SC_UNAUTHORIZED))
            .header(HttpHeaders.WWW_AUTHENTICATE, containsString(DISCOVERY_REALM));
    }

    @Nested
    class GivenHttpHeaders {
        @Test
        void verifyHttpHeadersOnUi() {
            Map<String, String> expectedHeaders = new HashMap<>();
            expectedHeaders.put("X-Content-Type-Options", "nosniff");
            expectedHeaders.put("X-XSS-Protection", "1; mode=block");
            expectedHeaders.put("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            expectedHeaders.put("Pragma", "no-cache");
            expectedHeaders.put("Content-Type", "text/html;charset=UTF-8");
            expectedHeaders.put("Transfer-Encoding", "chunked");
            expectedHeaders.put("X-Frame-Options", "DENY");

            List<String> forbiddenHeaders = new ArrayList<>();
            forbiddenHeaders.add("Strict-Transport-Security");
            Response response = RestAssured
                .given()
                .auth().basic(eurekaUserid, eurekaPassword)
                .get(getDiscoveryUriWithPath("/"));
            Map<String, String> responseHeaders = new HashMap<>();

            response.getHeaders().forEach(h -> responseHeaders.put(h.getName(), h.getValue()));

            expectedHeaders.forEach((key, value) -> assertThat(responseHeaders, hasEntry(key, value)));
            forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));
        }

        @Test
        void verifyHttpHeadersOnApi() {
            Map<String, String> expectedHeaders = new HashMap<>();
            expectedHeaders.put("X-Content-Type-Options", "nosniff");
            expectedHeaders.put("X-XSS-Protection", "1; mode=block");
            expectedHeaders.put("Cache-Control", "no-cache, no-store, max-age=0, must-revalidate");
            expectedHeaders.put("Pragma", "no-cache");
            expectedHeaders.put("X-Frame-Options", "DENY");

            List<String> forbiddenHeaders = new ArrayList<>();
            forbiddenHeaders.add("Strict-Transport-Security");

            Response response = RestAssured
                .given()
                .auth().basic(eurekaUserid, eurekaPassword)
                .get(getDiscoveryUriWithPath("/application"));
            Map<String, String> responseHeaders = new HashMap<>();
            response.getHeaders().forEach(h -> responseHeaders.put(h.getName(), h.getValue()));

            expectedHeaders.forEach((key, value) -> assertThat(responseHeaders, hasEntry(key, value)));
            forbiddenHeaders.forEach(h -> assertThat(responseHeaders, not(hasKey(h))));
        }

    }

}
