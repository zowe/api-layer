/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.config;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.zaas.ZaasApplication;
import org.zowe.apiml.zaas.security.mapping.AuthenticationMapper;

import javax.net.ssl.SSLException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.fail;
import static org.zowe.apiml.security.SecurityUtils.COOKIE_AUTH_NAME;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    /**
     * Simple Spring Context test to verify AT-TLS filter chain setup is in place with the right properties being sent
     */
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
    @ActiveProfiles({ "attlsServer", "attlsClient" })
    @DirtiesContext
    @Nested
    class GivenAttlsModeEnabled {

        @MockitoBean(name = "x509Mapper")
        private AuthenticationMapper x509Mapper;

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;

        @Test
        void requestFailsWithHttps() {
            assertThrows(SSLException.class, () -> {
                given()
                    .log().all()
                    .cookie(COOKIE_AUTH_NAME, "jwttoken")
                .when()
                    .get(String.format("https://%s:%d", hostname, port))
                .then()
                    .log().all();
                fail("Expected SSL failure");
            });
        }

        @Test
        void requestFailsWithAttlsContextReasonWithHttp() {
            given()
                .log().all()
                .cookie(COOKIE_AUTH_NAME, "jwttoken")
            .when()
                .get(String.format("http://%s:%d", hostname, port))
            .then()
                .log().all()
                .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .body(containsString("Connection is not secure."))
                .body(containsString("AttlsContext.getStatConn"));
        }

    }

    /**
     * This test intends to verify ICSF workaround (no keyring load)
     */
    @Nested
    @TestPropertySource(
        properties = {
            "server.ssl.keyStoreType=",
            "server.ssl.keyStorePassword=",
            "server.ssl.keyPassword=",
            "server.ssl.keyAlias=",
            "server.ssl.keyStore=",
            "apiml.security.auth.provider=zosmf"
        }
    )
    @ActiveProfiles({ "attlsServer", "attlsClient" })
    @DirtiesContext
    @SpringBootTest(
        classes = ZaasApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenSslDisabled {

        @MockitoBean(name = "x509Mapper")
        private AuthenticationMapper x509Mapper;

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;

        @Test
        void whenNoKeystore_thenStartupSuccess() {
            given()
                .log().all()
                .cookie(COOKIE_AUTH_NAME, "jwttoken")
            .when()
                .get(String.format("http://%s:%d", hostname, port))
            .then()
                .log().all()
                .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .body(containsString("Connection is not secure."))
                .body(containsString("AttlsContext.getStatConn"));
        }

    }

}
