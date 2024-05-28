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
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithBasePath;
import org.zowe.apiml.zaas.utils.JWTUtils;
import org.zowe.apiml.security.HttpsConfig;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;

@AcceptanceTest
@TestPropertySource(properties = {
    "apiml.security.auth.provider=dummy" // To simulate SAF auth provider that does not run outside of mainframe
})
class ZaasTest extends AcceptanceTestWithBasePath {

    private static final String COOKIE = "apimlAuthenticationToken";

    @Value("${server.ssl.keyStore}")
    private String keystore;

    @Value("${server.ssl.keyStorePassword:password}")
    private char[] keystorePassword;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Test
    void givenZosmfCookieAndDummyAuthProvider_whenZoweJwtRequest_thenUnauthorized() {
        HttpsConfig config = HttpsConfig.builder().keyAlias(keyAlias).keyPassword(keystorePassword).keyStore(keystore).build();
        String zosmfJwt = JWTUtils.createZosmfJwtToken("user", "z/OS", "Ltpa", config);

        //@formatter:off
        given()
            .cookie(COOKIE, zosmfJwt)
        .when()
            .post(basePath + "/gateway/zaas/zoweJwt")
        .then()
            .statusCode(SC_UNAUTHORIZED);
        //@formatter:on
    }

}
