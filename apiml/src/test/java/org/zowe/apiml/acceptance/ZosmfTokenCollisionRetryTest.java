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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.sun.net.httpserver.Headers;
import io.restassured.http.Cookie;
import io.restassured.response.ValidatableResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.security.common.util.JWTTestUtils;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_NO_CONTENT;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * z/OSMF issues JWTs with only 1-second (iat) granularity, so two independent logins for the same principal
 * landing in the same second can receive byte-identical tokens. If one of those logins is followed by a logout
 * before the other's isInvalidated() check runs, the second (perfectly legitimate) login sees its own fresh
 * token flagged as already invalidated.
 * <p>
 * Test will fail if {@code @EnableRetry} is not in Spring context, for example, if it's removed from
 * {@code ModulithConfig}.
 */
@TestPropertySource(properties = "apiml.security.auth.zosmf.jwtAutoconfiguration=jwt")
class ZosmfTokenCollisionRetryTest extends AcceptanceTestWithMockServices {

    private static final String AUTH_COOKIE = "apimlAuthenticationToken";
    private static final String LOGIN_ENDPOINT = "/gateway/api/v1/auth/login";
    private static final String LOGOUT_ENDPOINT = "/gateway/api/v1/auth/logout";
    private static final String LOGIN_BASIC_AUTH = "Basic dXNlcjpwYXNz";

    private static final String USERNAME = "user";
    private static final String JWT_COLLIDING = JWTTestUtils.createDummyZOSMFToken(USERNAME);
    private static final String JWT_AFTER_RETRY = JWTTestUtils.createDummyZOSMFToken(USERNAME);

    @Autowired
    @Qualifier("cacheManager")
    private CacheManager cacheManager;

    /**
     * Spring contexts in one test JVM share a single cache instance therefore clean up cache before
     * and after the test.
     */
    @BeforeEach
    @AfterEach
    void clearZosmfEndpointExistsCache() {
        var cache = cacheManager.getCache("zosmfAuthenticationEndpoint");
        if (cache != null) {
            cache.clear();
        }
    }

    @Test
    void givenSecondLoginCollidesWithJustLoggedOutToken_whenRetryIsEnabled_thenItRecoversOnRetry() throws JsonProcessingException {
        assertNotEquals(JWT_COLLIDING, JWT_AFTER_RETRY);
        var loginCallCount = new AtomicInteger();
        mockZosmfAuthenticateEndpoint(List.of(JWT_COLLIDING, JWT_COLLIDING, JWT_AFTER_RETRY), loginCallCount);

        String firstToken = login().statusCode(SC_NO_CONTENT).extract().cookie(AUTH_COOKIE);
        assertEquals(JWT_COLLIDING, firstToken);

        logout(firstToken).statusCode(SC_NO_CONTENT);

        String secondToken = login().statusCode(SC_NO_CONTENT).extract().cookie(AUTH_COOKIE);
        assertEquals(JWT_AFTER_RETRY, secondToken,
            "expected the retry to have landed on the non-colliding token from the next second z/OSMF call");

        assertEquals(3, loginCallCount.get(),
            "expected 3 authenticate() calls to z/OSMF: first login, second login's failed attempt, second login's retry");
    }

    private ValidatableResponse login() {
        return given()
            .auth().preemptive().basic("user", "pass")
        .when()
            .post(String.format("%s%s", basePath, LOGIN_ENDPOINT))
        .then();
    }

    private ValidatableResponse logout(String token) {
        return given()
            .cookie(new Cookie.Builder(AUTH_COOKIE, token).build())
        .when()
            .post(String.format("%s%s", basePath, LOGOUT_ENDPOINT))
        .then();
    }

    /**
     * Registers a "zosmf" mock exposing "/zosmf/services/authenticate" endpoint (POST for login, DELETE
     * for logout).
     * <p>
     * DELETE requests always succeed. POST requests carrying the empty-credentials "Basic Og==" header are the
     * {@code loginEndpointExists()} probe and always answer 401, which is interpreted as "the endpoint exists".
     * Logins returns one z/OSMF jwtToken at a time from {@code jwtPerLoginCall}, repeating the last entry once
     * exhausted. Each such call increments {@code loginCallCount}.
     * <p>
     * "/zosmf/info" is mocked only to satisfy the availability probe in the background; it deliberately returns
     * no token cookies, so that if {@code authenticate()} ever took the "/zosmf/info" fallback branch the login
     * would fail outright rather than quietly authenticating with a token.
     */
    private void mockZosmfAuthenticateEndpoint(List<String> jwtPerLoginCall, AtomicInteger loginCallCount) throws JsonProcessingException {
        mockService("zosmf").scope(MockService.Scope.TEST)
            .addEndpoint("/zosmf/info")
                .responseCode(SC_OK)
                .contentType("application/json")
                .bodyJson("{\"zosmf_version\":\"29\",\"zosmf_saf_realm\":\"SAFRealm\",\"zosmf_full_version\":\"29.0\"}")
            .and()
            .addEndpoint("/zosmf/services/authenticate")
                .responseProvider(he -> {
                    if ("DELETE".equals(he.getRequestMethod())) {
                        return MockService.Endpoint.Response.builder()
                            .responseCode(SC_OK)
                            .build();
                    }

                    String authorization = he.getRequestHeaders().getFirst("Authorization");
                    if (!LOGIN_BASIC_AUTH.equals(authorization)) {
                        return MockService.Endpoint.Response.builder()
                            .responseCode(SC_UNAUTHORIZED)
                            .build();
                    }

                    int callNumber = loginCallCount.getAndIncrement();
                    int index = Math.min(callNumber, jwtPerLoginCall.size() - 1);
                    String jwt = jwtPerLoginCall.get(index);

                    var responseHeaders = new Headers();
                    responseHeaders.add("Set-Cookie", "jwtToken=" + jwt);
                    return MockService.Endpoint.Response.builder()
                        .responseCode(SC_OK)
                        .headers(responseHeaders)
                        .build();
                })
            .and()
                .start();
    }

}
