/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.health;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.zowe.apiml.discovery.DiscoveryServiceApplication;
import org.zowe.apiml.discovery.config.EurekaConfig;
import org.zowe.apiml.discovery.functional.DiscoveryFunctionalTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.core.Is.is;

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "apiml.health.protected=false"
    },
    classes = {DiscoveryServiceApplication.class, EurekaConfig.class}
)
public class ProtectedHealthEndpointTest extends DiscoveryFunctionalTest {
    @Nested
    @ActiveProfiles("http")
    class GivenProtectedHealthEndpointWithHttp {
    @Test
    void applicationHealthEndpointsWhenProtected() {
        given()
            .when()
            .get(getDiscoveryUriWithPath("/application/health"))
            .then()
            .statusCode(is(HttpStatus.SC_OK));
      }
    }

    @Nested
    @ActiveProfiles("https")
    class GivenProtectedHealthEndpointWithHttps {
        @Test
        void applicationHealthEndpointsWhenProtected() {
            given()
                .when()
                .get(getDiscoveryUriWithPath("/application/health"))
                .then()
                .statusCode(is(HttpStatus.SC_OK));
        }
    }
}
