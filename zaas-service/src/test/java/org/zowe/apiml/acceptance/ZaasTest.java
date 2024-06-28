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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.zaas.ZaasApplication;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;
import org.zowe.apiml.zaas.utils.JWTUtils;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_SERVICE_UNAVAILABLE;

@SpringBootTest(classes = ZaasApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "apiml.security.auth.provider=dummy" // To simulate SAF auth provider that does not run outside of mainframe
})
@MockBean(name = "x509Mapper", classes = AuthenticationMapper.class)
class ZaasTest {

    private static final String COOKIE = "apimlAuthenticationToken";

    @Value("${server.ssl.keyStore}")
    private String keystore;

    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @LocalServerPort
    private int port;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Test
    void givenZosmfCookieAndDummyAuthProvider_whenZoweJwtRequest_thenUnavailable() {
        HttpsConfig config = HttpsConfig.builder().keyAlias(keyAlias).keyPassword(keystorePassword).keyStore(keystore).build();
        String zosmfJwt = JWTUtils.createZosmfJwtToken("user", "z/OS", "Ltpa", config);

        //@formatter:off
        given()
            .cookie(COOKIE, zosmfJwt)
        .when()
            .post(String.format("https://%s:%d/zaas/scheme/zoweJwt", hostname, port))
        .then()
            .statusCode(SC_SERVICE_UNAVAILABLE);
        //@formatter:on
    }

}
