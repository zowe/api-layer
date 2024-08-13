/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Nested;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.gateway.acceptance.common.AcceptanceTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;


@AcceptanceTest
@Disabled           //it has an issue when the property is false;No SecurityContext found in WebSession
@TestPropertySource(
    properties = {
        "apiml.health.protected=false"
    }
)
public class ProtectedHealthEndpointTest {

    @Value("${apiml.service.hostname:localhost}")
    protected String hostname;

    @LocalServerPort
    protected int port;

    protected String getGatewayUriWithPath(String path) {
        return getGatewayUriWithPath("https", path);
    }

    protected String getGatewayUriWithPath(String scheme, String path) {
        return String.format("%s://%s:%d/%s", scheme, hostname, port, path);
    }
    @Nested
    class GivenHealthEndPointProtectionEnabled {
        @Test
        void requestSuccessWith200() {

            given()
                .when()
                .get(getGatewayUriWithPath("application/health"))
                .then()
                .statusCode(is(HttpStatus.SC_OK));
        }
    }
}
