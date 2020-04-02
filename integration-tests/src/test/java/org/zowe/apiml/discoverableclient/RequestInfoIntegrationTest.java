/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discoverableclient;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.service.DiscoveryUtils;

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.zowe.apiml.gatewayservice.SecurityUtils.getConfiguredSslConfig;

/**
 *
 */
public class RequestInfoIntegrationTest {

    @BeforeEach
    public void setUp() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    private String getUrl() {
        return
            DiscoveryUtils.getInstances("discoverableclient").get(0).getUrl() +
            "/discoverableclient/api/v1/request";
    }

    @Test
    public void givenSignedRequest_thenReturnCertificateInfo_whenCallRequestInfo() {
        RestAssured.config = RestAssured.config().sslConfig(getConfiguredSslConfig());

        given()
        .when()
            .get(getUrl())
        .then()
            .statusCode(SC_OK)
            .body("signed", equalTo(Boolean.TRUE));
    }

}
