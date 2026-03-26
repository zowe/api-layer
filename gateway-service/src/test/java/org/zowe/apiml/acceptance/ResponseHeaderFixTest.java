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

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Profile;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.gateway.GatewayApplication;

import javax.servlet.http.HttpServletResponse;

import static io.restassured.RestAssured.given;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;

@SpringBootTest(
    classes = {
        GatewayApplication.class,
        ResponseHeaderFixTest.TestController.class
    },
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("ResponseHeaderFixTest")
@DirtiesContext
class ResponseHeaderFixTest {

    private static final int TEST_CONTENT_LENGTH = 101;
    private static final String CONTENT_LENGTH = "Content-Length";

    @LocalServerPort
    protected int port;

    @ParameterizedTest(name = "Test handling setting context-type using {1}")
    @CsvSource({
        "0,addHeader<String String>",
        "1,setHeader<String String>",
        "2,setIntHeader<String int>",
        "3,addIntHeader<String int>"
    })
    void givenRequest_whenSetContentLength_thenIsPropagated(int method, String description) {
        given()
            .relaxedHTTPSValidation()
        .when()
            .get(String.format("https://localhost:%d/test/%d/%s", port, method, CONTENT_LENGTH))
        .then()
            .statusCode(SC_OK)
            .header(CONTENT_LENGTH, String.valueOf(TEST_CONTENT_LENGTH))
            .header("Strict-Transport-Security", is(notNullValue()))
            .header("X-XSS-Protection", is(notNullValue()));
    }

    @ParameterizedTest(name = "Test handling headers without content-type using {1}")
    @CsvSource({
        "0,addHeader<String String>",
        "1,setHeader<String String>",
        "2,setIntHeader<String int>",
        "3,addIntHeader<String int>"
    })
    void givenRequest_whenDontSetContentLength_thenIsMissing(int method, String description) {
        given()
            .relaxedHTTPSValidation()
        .when()
            .get(String.format("https://localhost:%d/test/%d/%s", port, method, "otherHeaderName"))
        .then()
            .statusCode(SC_OK)
            .header(CONTENT_LENGTH,"0")
            .header("Strict-Transport-Security", is(notNullValue()))
            .header("X-XSS-Protection", is(notNullValue()));
    }

    @RestController
    @Profile("ResponseHeaderFixTest")
    static class TestController {

        @GetMapping(value = "/test/{method}/{headerName}")
        public void getApiDoc(@PathVariable("method") int method, @PathVariable("headerName") String headerName, HttpServletResponse response) {
            switch (method) {
                case 0:
                    response.addHeader(headerName, String.valueOf(TEST_CONTENT_LENGTH));
                    break;
                case 1:
                    response.setHeader(headerName, String.valueOf(TEST_CONTENT_LENGTH));
                    break;
                case 2:
                    response.setIntHeader(headerName, TEST_CONTENT_LENGTH);
                    break;
                case 3:
                    response.addIntHeader(headerName, TEST_CONTENT_LENGTH);
                    break;
                default:
                    throw new IllegalArgumentException("Unknown method: " + method);
            }

        }

    }

}
