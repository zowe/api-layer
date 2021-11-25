/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.external;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.zowe.apiml.util.categories.MetricsServiceTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasItems;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;

@TestsNotMeantForZowe
@MetricsServiceTest
public class MetricServiceTest {

    @BeforeAll
    static void setup() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void allClustersAraAvailable() {
        String jwt = gatewayToken("user", "user");
        given().header(HttpHeaders.AUTHORIZATION, "Bearer " + jwt)
            .get("https://localhost:10010/metrics-service/api/v1/clusters")
            .then()
            .body("name", hasItems("GATEWAY", "DISCOVERY"));
    }
}
