/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.functional;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.TestPropertySource;

import javax.net.ssl.SSLException;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;

@TestPropertySource(
    properties = {
        "server.attls.enabled=true",
        "server.ssl.enabled=false"
    }
)
public class AttlsConfigTest extends ApiCatalogFunctionalTest {

    @Nested
    class GivenAttlsModeEnabled {

        @Nested
        class WhenContextLoads {

            @Test
            void requestFailsWithHttps() {
                try {
                    given()
                        .log().all()
                    .when()
                        .get(getCatalogUriWithPath("containers"))
                    .then()
                        .log().all()
                        .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                    fail("Expected an SSL failure");
                } catch (Exception e) {
                    assertInstanceOf(SSLException.class, e);
                }
            }

            @Test
            void requestFailsWithAttlsContextReasonWithHttp() {
                given()
                    .log().all()
                .when()
                    .get(getCatalogUriWithPath("http", "apicatalog/api/v1/containers"))
                .then()
                    .log().all()
                    .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .body(containsString("Connection is not secure. org/zowe/commons/attls/AttlsContext.getStatConn"));
            }
        }
    }
}
