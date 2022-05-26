/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import org.apache.http.HttpStatus;
import org.junit.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.zowe.apiml.acceptance.common.AcceptanceTest;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

import java.io.IOException;

@AcceptanceTest
public class ValidateAPIControllerTest {
    
    private String validatePath;

    @BeforeEach
    void setup() throws IOException {
        validatePath = "https://localhost:10011/eureka/validate";
    }

    @Nested
    class GivenControllerServiceID {

        @Test
        void whenServiceId_validate() throws Exception {
            given()
                .param("serviceID","valid")
                .when()
                .post(validatePath)
                .then()
                .assertThat()
                .body("statusCode", equalTo("200"))
                .statusCode(HttpStatus.SC_OK);
        }

        @Test
        void whenServiceId_InvalidateUpper() throws Exception {
            given()
                .param("serviceID", "Invalid")
                .when()
                .post(validatePath)
                .then()
                .assertThat()
                .body("statusCode", equalTo("400"))
                .statusCode(HttpStatus.SC_OK);
        }

        @Test
        void whenServiceId_InvalidateSymbol() throws Exception {
            given()
                .param("serviceID", "invalid@")
                .when()
                .post(validatePath)
                .then()
                .assertThat()
                .body("statusCode", equalTo("400"))
                .statusCode(HttpStatus.SC_OK);
        }
        
    }
}