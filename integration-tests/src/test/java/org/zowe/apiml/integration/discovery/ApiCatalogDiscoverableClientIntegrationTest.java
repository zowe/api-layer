/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.integration.discovery;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.CatalogTest;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.http.HttpSecurityUtils;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verify integration of the API Catalog with Discoverable client to make sure that the discovery service and gateways
 * work properly together.
 */
@CatalogTest
@DiscoverableClientDependentTest
class ApiCatalogDiscoverableClientIntegrationTest implements TestWithStartedInstances  {
    private static final String GET_DISCOVERABLE_CLIENT_CONTAINER_ENDPOINT = "/apicatalog/api/v1/containers/cademoapps";
    private static final String GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/v1";
    private static final String GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT_V2 = "/apicatalog/api/v1/apidoc/discoverableclient/v2";

    private static final String GET_API_SERVICE_VERSION_DIFF_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/v1/v2";
    private static final String GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_VERSION = "/apicatalog/api/v1/apidoc/discoverableclient/v1/v3";
    private static final String GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_SERVICE = "/apicatalog/api/v1/apidoc/invalidService/v1/v2";


    @Nested
    class WhenGettingApiDoc {
        @Nested
        class ReturnRelevantApiDoc {
            @Test
            void givenV1ApiDocPath() throws Exception {
                final HttpResponse response = getResponse(GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT, HttpStatus.SC_OK);
                String jsonResponse = EntityUtils.toString(response.getEntity());
                DocumentContext jsonContext = JsonPath.parse(jsonResponse);

                validateDiscoverableClientApiV1(jsonResponse, jsonContext);
            }

            @Test
            void givenV2ApiDocPath() throws IOException {
                final HttpResponse response = getResponse(GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT_V2, HttpStatus.SC_OK);
                final String jsonResponse = EntityUtils.toString(response.getEntity());

                String apiCatalogSwagger = "\n**************************\n" +
                    "Integration Test: Discoverable Client Swagger" +
                    "\n**************************\n" +
                    jsonResponse +
                    "\n**************************\n";
                DocumentContext jsonContext = JsonPath.parse(jsonResponse);

                LinkedHashMap swaggerInfo = jsonContext.read("$.info");
                String swaggerBasePath = jsonContext.read("$.basePath");
                LinkedHashMap paths = jsonContext.read("$.paths");
                LinkedHashMap definitions = jsonContext.read("$.definitions");
                LinkedHashMap externalDoc = jsonContext.read("$.externalDocs");

                assertTrue(swaggerInfo.get("description").toString().contains("API"), apiCatalogSwagger);
                assertEquals("/discoverableclient/api/v2", swaggerBasePath, apiCatalogSwagger);
                assertEquals("External documentation", externalDoc.get("description"), apiCatalogSwagger);

                assertFalse(paths.isEmpty(), apiCatalogSwagger);
                assertNotNull(paths.get("/greeting"), apiCatalogSwagger);

                assertFalse(definitions.isEmpty(), apiCatalogSwagger);
                assertNotNull(definitions.get("Greeting"), apiCatalogSwagger);
            }

            // Verify that by default v1 swagger is returned.
            @Test
            void givenUrlForContainer() throws IOException {
                HttpResponse response = getResponse(GET_DISCOVERABLE_CLIENT_CONTAINER_ENDPOINT, HttpStatus.SC_OK);
                String containerJsonResponse = EntityUtils.toString(response.getEntity());
                DocumentContext containerJsonContext = JsonPath.parse(containerJsonResponse);

                // Validate container
                assertEquals("cademoapps", containerJsonContext.read("$[0].id"));
                assertEquals("Sample API Mediation Layer Applications", containerJsonContext.read("$[0].title"));
                assertEquals("UP", containerJsonContext.read("$[0].status"));

                // Get Discoverable Client swagger
                String dcJsonResponse = containerJsonContext.read("$[0].services[0]").toString();
                DocumentContext dcJsonContext = JsonPath.parse(dcJsonResponse);

                validateDiscoverableClientApiV1(dcJsonResponse, dcJsonContext);
            }
        }
    }

    @Nested
    class WhenGettingDifferenceBetweenVersions {
        @Nested
        class ReturnDifference {
            @Test
            void givenValidServiceAndVersions() throws Exception {
                final HttpResponse response = getResponse(GET_API_SERVICE_VERSION_DIFF_ENDPOINT, HttpStatus.SC_OK);

                //When
                final String textResponse = EntityUtils.toString(response.getEntity());
                assertThat(textResponse, startsWith("<!DOCTYPE html><html lang=\"en\">"));
                assertThat(textResponse, containsString("<header><h1>Api Change Log</h1></header>"));
                assertThat(textResponse, containsString(
                    "<div><h2>What&#x27;s New</h2><hr><ol><li><span class=\"GET\">GET</span>/greeting/{yourName} <span>Get a greeting</span></li></ol></div>"));
                assertThat(textResponse, containsString(
                    "<div><h2>What&#x27;s Deleted</h2><hr><ol><li><span class=\"GET\">GET</span><del>/{yourName}/greeting</del><span> Get a greeting</span></li>"));
            }
        }

        @Nested
        class ReturnNotFound {
            @Test
            void givenWrongVersion() throws Exception {
                getResponse(GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_VERSION, HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void givenWrongService() throws Exception {
                getResponse(GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_SERVICE, HttpStatus.SC_NOT_FOUND);
            }
        }
    }

    // Execute the endpoint and check the response for a return code
    private HttpResponse getResponse(String endpoint, int returnCode) throws IOException {
        HttpGet request = HttpRequestUtils.getRequest(endpoint);
        String cookie = HttpSecurityUtils.getCookieForGateway();
        HttpSecurityUtils.addCookie(request, cookie);

        // When
        HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));

        return response;
    }

    private void validateDiscoverableClientApiV1(String jsonResponse, DocumentContext jsonContext) throws IOException {
        String apiCatalogSwagger = "\n**************************\n" +
            "Integration Test: Discoverable Client Swagger" +
            "\n**************************\n" +
            jsonResponse +
            "\n**************************\n";

        // When
        LinkedHashMap swaggerInfo = jsonContext.read("$.info");
        String swaggerBasePath = jsonContext.read("$.basePath");
        LinkedHashMap paths = jsonContext.read("$.paths");
        LinkedHashMap definitions = jsonContext.read("$.definitions");
        LinkedHashMap externalDoc = jsonContext.read("$.externalDocs");

        // Then
        assertTrue(swaggerInfo.get("description").toString().contains("API"), apiCatalogSwagger);
        assertEquals("/discoverableclient/api/v1", swaggerBasePath, apiCatalogSwagger);
        assertEquals("External documentation", externalDoc.get("description"), apiCatalogSwagger);

        assertFalse(paths.isEmpty(), apiCatalogSwagger);
        assertNotNull(paths.get("/greeting"), apiCatalogSwagger);

        assertFalse(definitions.isEmpty(), apiCatalogSwagger);

        assertNotNull(definitions.get("ApiMessage"), apiCatalogSwagger);
        assertNotNull(definitions.get("ApiMessageView"), apiCatalogSwagger);
        assertNotNull(definitions.get("Greeting"), apiCatalogSwagger);
        assertNotNull(definitions.get("Pet"), apiCatalogSwagger);
        assertNotNull(definitions.get("RedirectLocation"), apiCatalogSwagger);
    }
}
