/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.broadcom.apiml.test.integration.discoverableclient;

import com.broadcom.apiml.test.integration.utils.categories.LocalDeploymentTest;
import com.broadcom.apiml.test.integration.utils.http.HttpRequestUtils;
import io.restassured.RestAssured;
import io.restassured.parsing.Parser;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.experimental.categories.Category;

import java.io.File;
import java.net.URI;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@Category(LocalDeploymentTest.class)
public class MultipartPutIntegrationTest {
    private static final String MULTIPART_PATH = "/api/v1/discoverableclient/multipart";
    private String configFileName = "example.txt";
    private ClassLoader classLoader = ClassLoader.getSystemClassLoader();

    @BeforeClass
    public static void beforeClass() {
        RestAssured.useRelaxedHTTPSValidation();
    }

    @Test
    public void shouldDoPutRequestAndMatchReturnBody() {
        RestAssured.registerParser("text/plain", Parser.JSON);
        URI uri = HttpRequestUtils.getUriFromGateway(MULTIPART_PATH);
        given().
            contentType("multipart/form-data").
            multiPart(new File(classLoader.getResource(configFileName).getFile())).
            expect().
            statusCode(200).
            body(is("fileUploadView")).
            when().
            put(uri);
    }

    @Test
    public void shouldDoPutRequestAndReturnOk() {
        URI uri = HttpRequestUtils.getUriFromGateway(MULTIPART_PATH);
        given().
            contentType("multipart/form-data").
            multiPart(new File(classLoader.getResource(configFileName).getFile())).
            when().
            put(uri).
            then().
            statusCode(200);
    }
}
