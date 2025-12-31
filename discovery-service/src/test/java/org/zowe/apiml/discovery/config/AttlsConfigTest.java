/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.discovery.functional.DiscoveryFunctionalTest;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    private String protocol = "http";

    @ActiveProfiles({ "attlsServer", "attlsClient" })
    @Nested
    class GivenAttlsModeEnabled extends DiscoveryFunctionalTest {

        @Override
        protected String getProtocol() {
            return protocol;
        }

        @Test
        void whenContextLoads_requestFailsWithHttps() {
            protocol = "https";
            assertThrows(IOException.class, () -> {
                given()
                    .log().all()
                .when()
                    .get(getDiscoveryUriWithPath("/application/info"))
                .then()
                    .log().all();
            });
        }

        /**
         * This test verifies the call attempted to use AT-TLS filters
         */
        @Test
        void whenContextLoads_RequestFailsWithAttlsContextReason() {
            protocol = "http";
            given()
                .log().all()
            .when()
                .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
                .log().all()
                .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .body(containsString("Connection is not secure."))
                .body(containsString("AttlsContextImpl.getStatConn"));
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
            "server.ssl.keyStore="
        }
    )
    @ActiveProfiles({ "attlsServer", "attlsClient" })
    class GivenSslDisabled extends DiscoveryFunctionalTest {

        @Test
        void whenNoKeystore_thenStartupSuccess() {
            protocol = "http";
            given()
                .log().all()
            .when()
                .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
                .log().all()
                .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .body(containsString("Connection is not secure."))
                .body(containsString("AttlsContextImpl.getStatConn"));
        }

    }

}
