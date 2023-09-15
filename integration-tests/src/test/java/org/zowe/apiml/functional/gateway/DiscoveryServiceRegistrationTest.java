/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.core.Is.is;
import static org.zowe.apiml.util.service.DiscoveryUtils.getAdditionalDiscoveryUrl;

@Tag("MultipleRegistrationsTest")
public class DiscoveryServiceRegistrationTest {

    @BeforeAll
    public static void init() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
    }

    @Test
    void givenGatewayIsRegisteredInAdditionalDiscovery_thenReturnInfoFromDiscoveryService() {
        given().config(SslContext.clientCertValid)
            .when()
            .get(getAdditionalDiscoveryUrl() + "/eureka/apps/gateway")
            .then()
            .statusCode(is(SC_OK));
    }
}
