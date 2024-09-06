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
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.zaas.ZaasApplication;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.utils.JWTUtils;

import static io.restassured.RestAssured.config;
import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.conn.ssl.SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER;

@SpringBootTest(classes = ZaasApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "apiml.security.auth.provider=dummy" // To simulate SAF auth provider that does not run outside of mainframe
})
@MockBean(name = "x509Mapper", classes = AuthenticationMapper.class)
class ZaasTest {

    private static final String COOKIE = "apimlAuthenticationToken";

    @Autowired
    private HttpConfig httpConfig;

    @LocalServerPort
    private int port;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Test
    void givenZosmfCookieAndDummyAuthProvider_whenZoweJwtRequest_thenUnavailable() {
        String zosmfJwt = JWTUtils.createZosmfJwtToken("user", "z/OS", "Ltpa", httpConfig.getHttpsConfig());

        //@formatter:off
        given().config(config().sslConfig(new SSLConfig().sslSocketFactory(
                new SSLSocketFactory(httpConfig.getSecureSslContextWithoutKeystore(), ALLOW_ALL_HOSTNAME_VERIFIER)))
            )
            .cookie(COOKIE, zosmfJwt)
        .when()
            .post(String.format("https://%s:%d/zaas/scheme/zoweJwt", hostname, port))
        .then()
            .statusCode(SC_SERVICE_UNAVAILABLE);
        //@formatter:on
    }

}
