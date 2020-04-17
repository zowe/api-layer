/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discoverableclient;

import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.http.HttpRequestUtils;

import java.net.URI;

import static io.restassured.RestAssured.given;

public class DownloadApiIntegrationTest {

    @BeforeAll
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    @TestsNotMeantForZowe
    public void shouldSendGetRequestAndDownloadCompressedImage() {
        RestAssured.registerParser("image/png", Parser.JSON);
        URI uri = HttpRequestUtils.getUriFromGateway("/api/v1/discoverableclient/get-file");
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
