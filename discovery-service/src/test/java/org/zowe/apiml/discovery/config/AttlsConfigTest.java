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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.discovery.functional.DiscoveryFunctionalTest;

import java.net.ConnectException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

@TestPropertySource(
    properties = {
        "server.attls.enabled=true",
        "server.ssl.enabled=false"
    }
)
@TestInstance(Lifecycle.PER_CLASS)
@ActiveProfiles("attls")
class AttlsConfigTest extends DiscoveryFunctionalTest {

    private String protocol = "http";

    @Override
    protected String getProtocol() {
        return protocol;
    }

    @Nested
    class GivenAttlsModeEnabledAndHttps {

        @BeforeEach
        void setUp() {
            protocol = "https";
        }

        @Test
        void whenContextLoads_RequestFailsWithHttps() {
            try {
                given()
                    .log().all()
                .when()
                    .get(getDiscoveryUriWithPath("/application/info"))
                .then()
                    .log().all()
                    .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR));
                fail("Expected SSL failure");
            } catch (Exception e) {
                assertInstanceOf(ConnectException.class, e);
            }
        }
    }

    @Nested
    class GivenAttlsModeEnabledAndHttp {

        @BeforeEach
        void setUp() {
            protocol = "http";
        }

        @Test
        void whenContextLoads_RequestFailsWithAttlsContextReason() {
            given()
                .log().all()
            .when()
                .get(getDiscoveryUriWithPath("/eureka/apps"))
            .then()
                .log().all()
                .statusCode(is(HttpStatus.SC_INTERNAL_SERVER_ERROR))
                .body(containsString("Connection is not secure. org/zowe/commons/attls/AttlsContext.getStatConn"));
        }
    }
}
