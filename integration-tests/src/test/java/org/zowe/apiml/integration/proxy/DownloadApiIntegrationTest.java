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
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.zowe.apiml.util.requests.Endpoints.*;

@DiscoverableClientDependentTest
class DownloadApiIntegrationTest implements TestWithStartedInstances {

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Nested
    class WhenDownloadingCompressedImage {
        @Nested
        class VerifyGzippedAttachment {
            @Test
            @TestsNotMeantForZowe
            void givenValidPathAndHeaders(String url) {
                RestAssured.registerParser("image/png", Parser.JSON);
                URI uri = HttpRequestUtils.getUriFromGateway(DISCOVERABLE_GET_FILE);
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
