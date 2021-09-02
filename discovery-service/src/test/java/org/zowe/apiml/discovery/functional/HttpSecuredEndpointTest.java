/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.functional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;

import static io.restassured.RestAssured.given;

@ActiveProfiles("http")
class HttpSecuredEndpointTest extends DiscoveryFunctionalTest {

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Test
    void UiIsSecuredWithConfiguredBasicAuth() {
        given()
            .get(getDiscoveryUriWithPath("/"))
            .then()
            .statusCode(HttpStatus.UNAUTHORIZED.value());

        given().auth().basic(eurekaUserid, eurekaPassword)
            .get(getDiscoveryUriWithPath("/"))
            .then()
            .statusCode(HttpStatus.OK.value());
    }
}
