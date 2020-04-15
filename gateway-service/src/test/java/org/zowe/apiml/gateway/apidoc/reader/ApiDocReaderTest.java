/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.apidoc.reader;

import org.junit.jupiter.api.Test;
import org.zowe.apiml.gateway.utils.JsonReaderUtil;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ApiDocReaderTest {

    private final ApiDocReader apiDocReader = new ApiDocReader();


    @Test
    public void givenFileLocationAsANull_whenLoadIsCalled_thenThrowApiDocReaderException() {
        Exception exception = assertThrows(ApiDocReaderException.class,
            () -> apiDocReader.load(null),
            "Expected exception is not ApiDocReaderException");

        assertEquals("API doc location can't be null or empty", exception.getMessage());
    }


    @Test
    public void givenEmptyFileLocation_whenLoadIsCalled_thenThrowApiDocReaderException() {
        Exception exception = assertThrows(ApiDocReaderException.class,
            () -> apiDocReader.load(""),
            "Expected exception is not ApiDocReaderException");

        assertEquals("API doc location can't be null or empty", exception.getMessage());
    }


    @Test
    public void givenFileLocation_whenFileIsNotExist_thenThrowApiDocReaderException() {
        ApiDocReader apiDocReader = new ApiDocReader();
        Exception exception = assertThrows(ApiDocReaderException.class,
            () -> apiDocReader.load("invalid-path.json"),
            "Expected exception is not ApiDocReaderException");

        assertEquals("OpenAPI file does not exist: invalid-path.json", exception.getMessage());
    }

    @Test
    public void givenFileLocationWithInvalidJsonContent_whenLoadIsCalled_thenThrowApiDocReaderException() {
        Exception exception = assertThrows(ApiDocReaderException.class,
            () -> apiDocReader.load("api-doc-invalid-content.json"),
            "Expected exception is not ApiDocReaderException");

        assertEquals("OpenAPI content is not valid", exception.getMessage());
    }

    @Test
    public void givenFileLocationWithValidJsonContent_whenLoadIsCalled_thenOpenApiShouldMatchWithJsonContent()
        throws IOException {

        String actualOpenApi = apiDocReader.load("api-doc.json");
        String expectedOpenApi = JsonReaderUtil.getJsonStringFromResource("api-doc.json");

        assertNotNull(actualOpenApi, "Open api object is null");
        assertEquals(expectedOpenApi, actualOpenApi, "Open api object is not equal with expected");
    }

    @Test
    public void givenFileLocationNameWithSpaces_whenFileExists_thenOpenApiShouldMatchWithJsonContent()
        throws IOException {

        String actualOpenApi = apiDocReader.load(" api-doc.json ");
        String expectedOpenApi = JsonReaderUtil.getJsonStringFromResource("api-doc.json");

        assertNotNull(actualOpenApi, "Open api object is null");
        assertEquals(expectedOpenApi, actualOpenApi, "Open api object is not equal with expected");
    }
}
