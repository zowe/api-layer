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

import io.restassured.config.SSLConfig;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.util.JWTTestUtils;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;
import org.zowe.apiml.zaas.ZaasApplication;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
    classes = ZaasApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("test")
class JwtValidationCacheTest {

    private static final String COOKIE = "apimlAuthenticationToken";

    @Autowired
    private HttpConfig httpConfig;

    @Autowired
    private CacheManager cacheManager;

    @LocalServerPort
    private int port;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    private String expiredJwtToken;

    private Cache validationJwtTokenCache;

    @Value("${server.ssl.keyPassword}")
    private char[] password;

    @Value("${server.ssl.keyStore}")
    private String clientCertKeystore;

    @Value("${server.ssl.keyStore}")
    private String keystore;

    @BeforeEach
    void setUp() throws Exception {
        SslContextConfigurer configurer = new SslContextConfigurer(password, clientCertKeystore, keystore);
        SslContext.prepareSslAuthentication(configurer);
        expiredJwtToken = JWTTestUtils.createExpiredZoweJwtToken("user", "z/OS", "Ltpa", httpConfig.getHttpsConfig());
        validationJwtTokenCache = cacheManager.getCache("validatedJwtTokens");
        validationJwtTokenCache.put(expiredJwtToken, new TokenAuthentication(expiredJwtToken));
    }

    @Nested
    class ExpiredJwtTokenInValidationCache {
        @ParameterizedTest
        @ValueSource(strings = {"ticket", "zosmf","zoweJwt","safIdt"})
        void whenZoweJwtSchemeCalled_thenUnauthorized(String scheme) {
            //@formatter:off
            given().config(config().sslConfig(new SSLConfig().sslSocketFactory(
                    new SSLSocketFactory(httpConfig.getSecureSslContextWithoutKeystore(), ALLOW_ALL_HOSTNAME_VERIFIER)))
                )
                .cookie(COOKIE, expiredJwtToken)
                .when()
                .post(String.format("https://%s:%d/zaas/scheme/%s", hostname, port, scheme))
                .then()
                .statusCode(SC_UNAUTHORIZED);
            //@formatter:on
        }
    }

    @Test
    void whenQueryCalledWithExpiredJwtToken_thenUnauthorized() {
        //@formatter:off
        var response = given().config(config().sslConfig(new SSLConfig().sslSocketFactory(
                new SSLSocketFactory(httpConfig.getSecureSslContextWithoutKeystore(), ALLOW_ALL_HOSTNAME_VERIFIER)))
            )
            .cookie(COOKIE, expiredJwtToken)
            .when()
            .get(String.format("https://%s:%d/zaas/api/v1/auth/query", hostname, port))
            .then()
            .statusCode(SC_UNAUTHORIZED)
            .extract().body().asString();
        //@formatter:on

        assertTrue(response.contains("The validity of the token is expired."));
    }

}
