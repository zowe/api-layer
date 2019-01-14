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

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.Matchers.is;
import java.io.File;

import static io.restassured.RestAssured.given;

public class MultipartPutIntegrationTest {
    @Autowired
    @Test
    public void shouldDoPutRequestWithMultipart() {
        given().
            contentType("multipart/mixed").
            multiPart(new File("example.txt")).
        expect().
            statusCode(200).
            body("fileUploadResult", is("OK")).
        when().
            put("/api/v1/discoverableclient/multipart");
    }
//
//    @Test
//    public void shouldDoPutRequestWithMultipart() {
//        given().
//            contentType(MediaType.MULTIPART_FORM_DATA_VALUE).
//            multiPart(new File("example.txt")).
//            when().
//            put("/api/v1/discoverableclient/multipart").
//            then().
//            statusCode(200);
//    }
}
