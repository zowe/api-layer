/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.authentication.pat;

import com.nimbusds.jwt.JWTParser;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import org.apache.groovy.util.Arrays;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;
import java.text.ParseException;
import java.util.Collections;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.personalAccessToken;
import static org.zowe.apiml.util.requests.Endpoints.*;

class PATWithAllSchemesTest {

    static Stream<Arguments> authentication() {
        return Stream.of(
            Arguments.of("PAT header", (BiFunction<RequestSpecification, String, RequestSpecification>) (rs, token) -> rs.header(ApimlConstants.PAT_HEADER_NAME, token)),
            Arguments.of("authorization header", (BiFunction<RequestSpecification, String, RequestSpecification>) (rs, token) -> rs.header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token)),
            Arguments.of("PAT cookie", (BiFunction<RequestSpecification, String, RequestSpecification>) (rs, token) -> rs.cookie(ApimlConstants.PAT_COOKIE_AUTH_NAME, token)),
            Arguments.of("APIML cookie", (BiFunction<RequestSpecification, String, RequestSpecification>) (rs, token) -> rs.cookie(COOKIE_NAME, token))
        );
    }

    static Stream<Arguments> schemas() {
        return Stream.of(
            Arguments.of("zowejwt", HttpRequestUtils.getUriFromGateway(ZOWE_JWT_REQUEST), (Consumer<Response>) r -> {
                assertEquals(HttpStatus.SC_OK, r.getStatusCode());
                assertNull(r.getBody().path("headers.authorization"));
                assertThat(r.getBody().path("headers.cookie"), containsString(COOKIE_NAME));
                String jwt = r.getBody().path("headers.cookie").toString();
                try {
                    String issuer = JWTParser.parse(jwt.substring(COOKIE_NAME.length()).trim()).getJWTClaimsSet().toJSONObject().get("iss").toString();
                    assertEquals("APIML", issuer);
                } catch (ParseException e) {
                    fail(e);
                }
            }),
            Arguments.of("dcpassticket", HttpRequestUtils.getUriFromGateway(REQUEST_INFO_ENDPOINT), (Consumer<Response>) r -> {
                assertEquals(HttpStatus.SC_OK, r.getStatusCode());
                assertThat(r.getBody().path("headers.authorization"), startsWith("Basic "));
                assertThat(r.getBody().path("cookies"), not(hasKey(COOKIE_NAME)));
            }),
            Arguments.of("dcsafidt", HttpRequestUtils.getUriFromGateway(SAF_IDT_REQUEST), (Consumer<Response>) r -> {
                assertEquals(HttpStatus.SC_OK, r.getStatusCode());
                assertThat(r.getBody().path("headers"), hasKey("x-saf-token"));
            })
        );
    }

    static Stream<Arguments> authSchemas() {
        return authentication().flatMap(a -> schemas().map(s ->
            Arguments.of(Arrays.concat(a.get(), s.get()))
        ));
    }

    @BeforeAll
    static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @InfinispanStorageTest
    @ParameterizedTest(name = "Test service with {0} schema with the credentials stored in {2}")
    @MethodSource("org.zowe.apiml.integration.authentication.pat.PATWithAllSchemesTest#authSchemas")
    void requestWithPAT(
        String credentialContainer, BiFunction<RequestSpecification, String, RequestSpecification> authenticationAction,
        String name, URI urlSpecification, Consumer<Response> validation) {
        String pat = personalAccessToken(Collections.singleton(name));

        validation.accept(authenticationAction.apply(given(), pat)
            .config(SslContext.tlsWithoutCert)
            .when()
            .get(urlSpecification)
        );
    }
}
