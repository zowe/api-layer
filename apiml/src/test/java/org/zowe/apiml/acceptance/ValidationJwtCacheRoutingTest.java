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

import lombok.SneakyThrows;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.util.JWTTestUtils;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.junit.jupiter.api.Assertions.*;

@AcceptanceTest
class ValidationJwtCacheRoutingTest extends AcceptanceTestWithMockServices {

    private static final String COOKIE = "apimlAuthenticationToken";

    @Autowired
    CacheManager cacheManager;

    @LocalServerPort
    private int port;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    private String expiredJwtToken;

    private Cache validationJwtTokenCache;

    @Value("${server.ssl.keyPassword}")
    char[] password;
    @Value("${server.ssl.keyStore}")
    String client_cert_keystore;
    @Value("${server.ssl.keyStore}")
    String keystore;

    @BeforeAll
    void initMockServices() {
        getSchemes().forEach(scheme ->
            mockService("%s-service".formatted(scheme.toLowerCase())).scope(MockService.Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.fromString(scheme))
                .applid("dummy")
                .addEndpoint("/%s-service/foo".formatted(scheme.toLowerCase()))
                .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("Authorization")))
                .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-SAF-Token")))
                .assertion(exchange -> assertTrue(exchange.getRequestHeaders().containsKey("X-zowe-auth-failure")))
                .responseCode(200)
                .and()
                .start()
        );
    }

    @BeforeEach
    @SneakyThrows
    void setUp() {
        mockZosmfSuccess();
        SslContextConfigurer configurer = new SslContextConfigurer(password, client_cert_keystore, keystore);
        SslContext.prepareSslAuthentication(configurer);
        expiredJwtToken = JWTTestUtils.createExpiredZoweJwtToken("user", "z/OS", "Ltpa", httpConfig.getHttpsConfig());
        validationJwtTokenCache = cacheManager.getCache("validationJwtToken");
        validationJwtTokenCache.put(expiredJwtToken, new TokenAuthentication(expiredJwtToken));
    }

    @AfterAll
    void stop() {
        SslContext.reset();
    }

    Stream<String> getSchemes() {
        return Stream.of("httpBasicPassTicket", "zosmf", "zoweJwt", "safIdt");
    }

    @ParameterizedTest
    @MethodSource("getSchemes")
    void whenExpiredTokenInValidationCacheRouting_thenAuthorizationFails(String scheme) {
        //@formatter:off
        var response = given()
            .cookie(COOKIE, expiredJwtToken)
            .when()
            .get("https://%s:%d/%s-service/api/v1/foo".formatted(hostname, port, scheme.toLowerCase()));
        //@formatter:on

        assertEquals(SC_OK, response.getStatusCode());
        assertNotNull(response.getHeader("X-zowe-auth-failure"));
    }

    @Test
    void whenExpiredTokenInValidationCacheQuery_thenAuthorizationFails() {
        //@formatter:off
        given()
            .cookie(COOKIE, expiredJwtToken)
            .when()
            .get("https://%s:%d/gateway/api/v1/auth/query".formatted(hostname, port))
            .then()
            .statusCode(SC_UNAUTHORIZED);
        //@formatter:on
    }
}
