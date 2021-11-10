/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.File;
import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.zowe.apiml.util.requests.Endpoints.*;

@DiscoverableClientDependentTest
class MultipartPutIntegrationTest implements TestWithStartedInstances {
    private final String configFileName = "example.txt";
    private final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    @BeforeAll
    static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    protected static URI[] discoverableClientUrls() {
        return new URI[]{
            HttpRequestUtils.getUriFromGateway(DISCOVERABLE_MULTIPART)
        };
    }

    @Nested
    class WhenSendingMultipartData {
        @Nested
        class VerifyBodyMatches {
            @ParameterizedTest(name = "givenPutRequest {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.proxy.MultipartPutIntegrationTest#discoverableClientUrls")
            void givenPutRequest(URI url) {
                RestAssured.registerParser("text/plain", Parser.JSON);

                given().
                    contentType("multipart/form-data").
                    multiPart(new File(classLoader.getResource(configFileName).getFile())).
                expect().
                    statusCode(200).
                    body("fileName", equalTo("example.txt")).
                    body("fileType", equalTo("application/octet-stream")).
                when().
                    put(url);
            }

            @ParameterizedTest(name = "givenPostRequest {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.proxy.MultipartPutIntegrationTest#discoverableClientUrls")
            void givenPostRequest(URI url) {
                RestAssured.registerParser("text/plain", Parser.JSON);

                given().
                    contentType("multipart/form-data").
                    multiPart(new File(classLoader.getResource(configFileName).getFile())).
                expect().
                    statusCode(200).
                    body("fileName", equalTo("example.txt")).
                    body("fileType", equalTo("application/octet-stream")).
                when().
                    post(url);
            }
        }
    }
}
