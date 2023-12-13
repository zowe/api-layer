/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.caching.CachingService;
import org.zowe.apiml.util.config.SslContext;

import javax.net.ssl.SSLException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

@SpringBootTest(
    classes = CachingService.class,
    webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT
)
@ActiveProfiles("AttlsConfigTestCachingService")
@TestPropertySource(
    properties = {
        "server.attls.enabled=true",
        "server.ssl.enabled=false"
    }
)
@TestInstance(Lifecycle.PER_CLASS)
public class AttlsConfigTest {

    @Value("${apiml.service.hostname:localhost}")
    String hostname;
    @LocalServerPort
    int port;


    @Nested
    class GivenAttlsModeEnabled {

        private String getUri(String scheme, String endpoint) {
            return String.format("%s://%s:%d/%s/%s", scheme, hostname, port, "api/v1", endpoint);
        }

        @Nested
        class WhenContextLoads {

            @Test
            void requestFailsWithHttps() {
                try {
                    given()
                        .config(SslContext.clientCertUnknownUser)
                        .header("Content-type", "application/json")
                        .get(getUri("https", "cache"))
                    .then()
                        .statusCode(HttpStatus.FORBIDDEN.value());
                    fail("");
                } catch (Exception e) {
                    assertInstanceOf(SSLException.class, e);
                }
            }

            @Test
            void requestFailsWithAttlsReasonWithHttp() {
                given()
                    .config(SslContext.clientCertUnknownUser)
                    .header("Content-type", "application/json")
                    .get(getUri("http", "cache"))
                .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("Connection is not secure."))
                    .body(containsString("AttlsContext.getStatConn"));
            }
        }
    }
}
