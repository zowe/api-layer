/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.hamcrest.core.Is.is;

@AcceptanceTest
public class RequestInstanceTest extends AcceptanceTestWithTwoServices {

    @Nested
    class WhenValidInstanceId{
        @Test
        void chooseCorrectService() throws IOException {
            mockValid200HttpResponse();
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            given().header("X-Host","serviceid1-copy").
                when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then().statusCode(is(SC_OK)).header("X-Response-Host",is(serviceWithCustomConfiguration.getId()));
        }
    }
    @Nested
    class WhenNonExistingInstanceId{
        @Test
        void cantChooseServer() throws IOException {
            mockValid200HttpResponse();
            applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
            given().header("X-Host","non-existing").
                when()
                .get(basePath + serviceWithDefaultConfiguration.getPath())
                .then()
                .statusCode(is(SC_SERVICE_UNAVAILABLE));
        }
    }
}
