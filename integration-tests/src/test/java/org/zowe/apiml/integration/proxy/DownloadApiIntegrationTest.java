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
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;

@DiscoverableClientDependentTest
class DownloadApiIntegrationTest implements TestWithStartedInstances {

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    protected static String[] discoverableClientSource() {
        return new String[]{
            "/discoverableclient/api/v1/get-file",
            "/api/v1/discoverableclient/get-file"
        };
    }

    @Nested
    class WhenDownloadingCompressedImage {
        @Nested
        class VerifyGzippedAttachment {
            @ParameterizedTest(name = "givenValidPathAndHeaders {index} {0} ")
            @MethodSource("org.zowe.apiml.integration.proxy.DownloadApiIntegrationTest#discoverableClientSource")
            @TestsNotMeantForZowe
            void givenValidPathAndHeaders(String url) {
                RestAssured.registerParser("image/png", Parser.JSON);
                URI uri = HttpRequestUtils.getUriFromGateway(url);
                given().
                    contentType("application/octet-stream").
                    accept("image/png").
                when().
                    get(uri).
                then().
                    statusCode(200).
                    header("Content-Disposition", "attachment;filename=api-catalog.png").
                    header("Content-Encoding", "gzip").
                    contentType("image/png");
            }
        }
    }
}
