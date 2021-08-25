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

import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import java.io.IOException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;

@AcceptanceTest
public class CompressPerServiceTest extends AcceptanceTestWithTwoServices {

    @Test
    void givenServiceAcceptsCompression_thenResponseIsCompressed() throws IOException {
        mockValid200HttpResponse();
        applicationRegistry.setCurrentApplication(serviceWithCustomConfiguration.getId());
        discoveryClient.createRefreshCacheEvent();
        given()
            .when()
            .get(basePath + serviceWithCustomConfiguration.getPath())
            .then()
            .statusCode(is(SC_OK))
            .header("Content-Encoding", is("gzip"));
    }

    @Test
    void givenServiceDoesntAcceptsCompression_thenResponseIsNotCompressed() throws IOException {
        mockValid200HttpResponse();
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        discoveryClient.createRefreshCacheEvent();
        given()
            .when()
            .get(basePath + serviceWithCustomConfiguration.getPath())
            .then()
            .statusCode(is(SC_OK))
            .header("Content-Encoding", Matchers.nullValue());
    }
}
