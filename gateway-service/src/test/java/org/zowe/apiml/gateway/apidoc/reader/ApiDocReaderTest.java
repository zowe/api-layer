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

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.parser.OpenAPIV3Parser;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.*;

public class ApiDocReaderTest {

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Test
    public void givenFileLocationAsANull_whenLoadIsCalled_thenThrowApiDocReaderException() {
        expectedException.expectMessage("API doc location can't be null or empty");
        expectedException.expect(ApiDocReaderException.class);

        ApiDocReader apiDocReader = new ApiDocReader();
        apiDocReader.load(null);
    }


    @Test
    public void givenEmptyFileLocation_whenLoadIsCalled_thenThrowApiDocReaderException() {
        expectedException.expectMessage("API doc location can't be null or empty");
        expectedException.expect(ApiDocReaderException.class);

        ApiDocReader apiDocReader = new ApiDocReader();
        apiDocReader.load("");
    }


    @Test
    public void givenFileLocation_whenFileIsNotExist_thenThrowApiDocReaderException() {
        expectedException.expectMessage("OpenAPI file does not exist");
        expectedException.expect(ApiDocReaderException.class);

        ApiDocReader apiDocReader = new ApiDocReader();
        apiDocReader.load("classpath:invalid-path.json");
    }


    @Test
    public void givenFileLocationWithoutClassPathPrefix_whenFileIsNotExist_thenThrowApiDocReaderException() {
        expectedException.expectMessage("OpenAPI file does not exist");
        expectedException.expect(ApiDocReaderException.class);

        ApiDocReader apiDocReader = new ApiDocReader();
        apiDocReader.load("invalid-path.json");
    }


    @Test
    public void givenFileLocationWithInvalidJsonContent_whenLoadIsCalled_thenThrowApiDocReaderException() {
        expectedException.expectMessage("OpenAPI content is not valid");
        expectedException.expect(ApiDocReaderException.class);

        ApiDocReader apiDocReader = new ApiDocReader();
        apiDocReader.load("api-doc-invalid-content.json");
    }

    @Test
    public void givenFileLocationWithValidJsonContent_whenLoadIsCalled_thenOpenApiShouldMatchWithJsonContent() {
        ApiDocReader apiDocReader = new ApiDocReader();
        OpenAPI actualOpenApi = apiDocReader.load("api-doc.json");
        OpenAPI expectedOpenApi = new OpenAPIV3Parser().read("api-doc.json");

        assertNotNull("Open api object is null", actualOpenApi);
        assertEquals("Open api object is not equal with expected", expectedOpenApi, actualOpenApi);
    }

    @Test
    public void givenFileLocationNameWithSpaces_whenFileExists_thenOpenApiShouldMatchWithJsonContent() {
        ApiDocReader apiDocReader = new ApiDocReader();
        OpenAPI actualOpenApi = apiDocReader.load(" api-doc.json ");
        OpenAPI expectedOpenApi = new OpenAPIV3Parser().read("api-doc.json");

        assertNotNull("Open api object is null", actualOpenApi);
        assertEquals("Open api object is not equal with expected", expectedOpenApi, actualOpenApi);
    }
}
