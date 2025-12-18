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
import io.restassured.config.HttpClientConfig;
import io.restassured.config.RestAssuredConfig;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Random;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_MULTIPART;

@DiscoverableClientDependentTest
class MultipartPutIntegrationTest implements TestWithStartedInstances {
    private final String configFileName = "example.txt";
    private final ClassLoader classLoader = ClassLoader.getSystemClassLoader();
    private URI url = HttpRequestUtils.getUriFromGateway(DISCOVERABLE_MULTIPART);

    @BeforeAll
    static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
        RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();
    }

    @Nested
    class WhenSendingMultipartData {
        @Nested
        class VerifyBodyMatches {
            @Test
            void givenPutRequest() {
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

            @Test
            void givenPostRequest() {
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

        @Test
        void givenLargeFileUpload() {
            int payloadSize = 750 * 1024 * 1024; //750MB
            RestAssuredConfig config = RestAssured.config()
                .httpClient(HttpClientConfig.httpClientConfig()
                    .setParam("http.connection.timeout", 300000)
                    .setParam("http.socket.timeout", 300000)
                    .setParam("http.method.retry-handler", (org.apache.http.client.HttpRequestRetryHandler)
                        (exception, executionCount, context) -> false));

            given()
                .config(config)
                .multiPart(
                    "file",
                    "largefile.dat",
                    new RandomDataInputStream(payloadSize),
                    "application/octet-stream"
                )
                .when()
                .post(url)
                .then()
                .statusCode(200)
                .body("fileName", equalTo("largefile.dat"))
                .body("fileType", equalTo("application/octet-stream"))
                .body("size", equalTo(payloadSize));
        }

        static class RandomDataInputStream extends InputStream {
            private final long targetSize;
            private long count = 0;
            private final Random random = new Random();

            RandomDataInputStream(long targetSize) {
                this.targetSize = targetSize;
            }

            @Override
            public int read() throws IOException {
                if (count >= targetSize) {
                    return -1;
                }
                count++;
                return random.nextInt(256);
            }

            @Override
            public int read(byte[] b) throws IOException {
                return read(b, 0, b.length);
            }

            @Override
            public int read(byte[] b, int off, int len) {
                if (count >= targetSize) return -1;
                long remaining = targetSize - count;
                int toRead = (int) Math.min(len, remaining);
                random.nextBytes(b);
                count += toRead;
                return toRead;
            }
        }
    }
}
