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

import org.zowe.apiml.util.http.HttpRequestUtils;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.net.URI;

import static io.restassured.RestAssured.given;

public class MultipartPutIntegrationTest {
    private static final String MULTIPART_PATH = "/api/v1/discoverableclient/multipart";
    private final String configFileName = "example.txt";
    private final ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    @BeforeClass
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    //@formatter:off
    @Test
    public void shouldDoPutRequestAndMatchReturnBody() {
        RestAssured.registerParser("text/plain", Parser.JSON);
        URI uri = HttpRequestUtils.getUriFromGateway(MULTIPART_PATH);
        given().
            contentType("multipart/form-data").
            multiPart(new File(classLoader.getResource(configFileName).getFile())).
        expect().
            statusCode(200).
            body("fileName", equalTo("example.txt")).
            body("fileType", equalTo("application/octet-stream")).
        when().
            put(uri);
    }

    @Test
    public void shouldDoPostRequestAndMatchReturnBody() {
        RestAssured.registerParser("text/plain", Parser.JSON);
        URI uri = HttpRequestUtils.getUriFromGateway(MULTIPART_PATH);
        given().
            contentType("multipart/form-data").
            multiPart(new File(classLoader.getResource(configFileName).getFile())).
        expect().
            statusCode(200).
            body("fileName", equalTo("example.txt")).
            body("fileType", equalTo("application/octet-stream")).
        when().
            post(uri);
    }

    //@formatter:on
}
