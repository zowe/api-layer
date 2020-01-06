/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discoverableclient;

import com.ca.mfaas.util.http.HttpRequestUtils;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.BeforeClass;
import org.junit.Test;

import java.net.URI;

import static io.restassured.RestAssured.given;

public class DownloadApiIntegrationTest {

    @BeforeClass
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void shouldDoGetRequestAndDownloadZipFile() {
        RestAssured.registerParser("application/zip", Parser.JSON);
        URI uri = HttpRequestUtils.getUriFromGateway("/api/v1/discoverableclient/get-file");
        given().
            contentType("application/octet-stream").
            accept("application/zip").
            expect().
            statusCode(200).
            header("Content-Disposition", "attachment;filename=sample-text.zip").
            header("Content-Encoding", "gzip").
            when().
            get(uri)
            .then().contentType("application/zip")
            ;
    }
}
