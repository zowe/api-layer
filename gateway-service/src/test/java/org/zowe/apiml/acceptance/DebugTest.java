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

import org.junit.jupiter.api.Test;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

//TODO Delete this test, it's just to show how the fix works

@AcceptanceTest
public class DebugTest extends AcceptanceTestWithTwoServices {

    @Test
    void test() throws Exception{
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockValid200HttpResponse();
        discoveryClient.createRefreshCacheEvent();

        given()
        .when()
        .options(basePath + "/serviceid2/test")
        .then().statusCode(is(SC_OK));
    }

}
