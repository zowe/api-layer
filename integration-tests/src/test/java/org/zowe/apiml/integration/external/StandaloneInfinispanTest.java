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
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.InfinispanStorageTest;
import org.zowe.apiml.util.config.ItSslConfigFactory;
import org.zowe.apiml.util.config.SslContext;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;
import static org.zowe.apiml.util.requests.Endpoints.HEALTH;

@InfinispanStorageTest
@EnabledIfSystemProperty(named = "environment.infinispan.standalone", matches = "true")
class StandaloneInfinispanTest implements TestWithStartedInstances {

    @BeforeAll
    static void setupSsl() throws Exception {
        SslContext.prepareSslAuthentication(ItSslConfigFactory.integrationTests());
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    void reportsHealthyStandaloneCachesWithoutCluster() {
        given().config(SslContext.clientCertApiml)
            .when().get(getUriFromGateway(HEALTH))
            .then().log().ifValidationFails()
            .statusCode(200)
            .body("components.infinispan.status", is("UP"))
            .body("components.infinispan.details.infinispan.mode", is("standalone"))
            .body("components.infinispan.details.infinispan", not(hasKey("cluster")))
            .body("components.infinispan.details.infinispan.caches.zoweCache", is("RUNNING"))
            .body("components.infinispan.details.infinispan.caches.zoweInvalidatedTokenItemCache", is("RUNNING"))
            .body("components.infinispan.details.infinispan.caches.invalidatedJwtTokens", is("RUNNING"));
    }

}
