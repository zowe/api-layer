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
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import static io.restassured.RestAssured.given;

@TestPropertySource( properties = {"apiml.health.protected=false"} )
@DirtiesContext
public class ApiCatalogProtectedEndpointTest  extends  ApiCatalogFunctionalTest {
    @Test
    void requestSuccessWith200() {
        given()
            .when()
            .get(getCatalogUriWithPath("apicatalog/application/health"))
            .then()
            .statusCode(HttpStatus.SC_SERVICE_UNAVAILABLE);
    }
}
