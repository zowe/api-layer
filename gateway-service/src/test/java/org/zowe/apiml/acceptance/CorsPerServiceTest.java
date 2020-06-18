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

import io.restassured.http.Header;
import org.apache.http.client.methods.HttpUriRequest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.zowe.apiml.acceptance.common.AcceptanceTest;
import org.zowe.apiml.acceptance.common.AcceptanceTestWithTwoServices;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.*;

/*
 * Verify default state
 *    - With pre-flight request
 *       The No Access Control Allow Origin header
 *    - Without pre-flight request
 *       The header for Access Control Allow Origin isn't present
 *  Verify changed state
 *    - With pre-flight request
 *      - Verify that the downstream headers for CORS are ignored
 *    - Without pre-flight request
 *      - Verify that the downstream headers for CORS are ignored
 *
 *  The case for downstream headers will be?
 *     With disabled by default not much.
 *     Is there case when disabled cors would mean that we will have to change it?
 *     When allowed the headers are also irelevant as the headers adds no behavior
 *  What can the headers do?
 *  If the pre-flight request comes and we
 */
@AcceptanceTest
public class CorsPerServiceTest extends AcceptanceTestWithTwoServices {
    @Test
    // Verify the header to allow CORS isn't set
    // Verify there was no call to southbound service
    void givenCorsIsDelegatedToGatewayButServiceDoesntAllowCors_whenPreflightRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockValid200HttpResponse();
        discoveryClient.createRefreshCacheEvent();

        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .options(basePath + serviceWithDefaultConfiguration.getPath())
        .then()
            .statusCode(is(SC_FORBIDDEN))
            .header("Access-Control-Allow-Origin", is(nullValue()));

        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

    @Test
    // Verify the header to allow CORS isn't set
    // Verify there was no call to southbound service
    void givenCorsIsDelegatedToGatewayButServiceDoesntAllowCors_whenSimpleCorsRequestArrives_thenNoAccessControlAllowOriginIsSet() throws Exception {
        applicationRegistry.setCurrentApplication(serviceWithDefaultConfiguration.getId());
        mockValid200HttpResponse();
        discoveryClient.createRefreshCacheEvent();

        given()
            .header(new Header("Origin", "https://foo.bar.org"))
            .header(new Header("Access-Control-Request-Method", "POST"))
            .header(new Header("Access-Control-Request-Headers", "origin, x-requested-with"))
        .when()
            .get(basePath + serviceWithDefaultConfiguration.getPath())
        .then()
            .statusCode(is(SC_FORBIDDEN))
            .header("Access-Control-Allow-Origin", is(nullValue()));

        verify(mockClient, never()).execute(ArgumentMatchers.any(HttpUriRequest.class));
    }

}
