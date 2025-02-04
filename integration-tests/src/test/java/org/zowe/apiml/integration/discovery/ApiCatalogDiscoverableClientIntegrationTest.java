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
import net.minidev.json.JSONArray;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.CatalogTest;
import org.zowe.apiml.util.categories.DiscoverableClientDependentTest;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.http.HttpSecurityUtils;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.zowe.apiml.util.requests.Endpoints.*;

/**
 * Verify integration of the API Catalog with Discoverable client to make sure that the discovery service and gateways
 * work properly together.
 */
@CatalogTest
@DiscoverableClientDependentTest
class ApiCatalogDiscoverableClientIntegrationTest implements TestWithStartedInstances {

    @Nested
    class WhenGettingApiDoc {
        @Nested
        class ReturnRelevantApiDoc {
            @Test
            void givenV1ApiDocPath() throws Exception {
                final HttpResponse response = getResponse(DISCOVERABLE_CLIENT_API_DOC_ENDPOINT, HttpStatus.SC_OK);
                String jsonResponse = EntityUtils.toString(response.getEntity());
                DocumentContext jsonContext = JsonPath.parse(jsonResponse);

                validateDiscoverableClientApiV1(jsonResponse, jsonContext);
            }

            @Test
            void givenV2ApiDocPath() throws IOException {
                final HttpResponse response = getResponse(DISCOVERABLE_CLIENT_API_DOC_ENDPOINT_V2, HttpStatus.SC_OK);
                final String jsonResponse = EntityUtils.toString(response.getEntity());

                String apiCatalogSwagger = "\n**************************\n" +
                    "Integration Test: Discoverable Client Swagger" +
                    "\n**************************\n" +
                    jsonResponse +
                    "\n**************************\n";
                DocumentContext jsonContext = JsonPath.parse(jsonResponse);

                LinkedHashMap swaggerInfo = jsonContext.read("$.info");
                String swaggerServer = jsonContext.read("$.servers[0].url");
                LinkedHashMap paths = jsonContext.read("$.paths");
                LinkedHashMap definitions = jsonContext.read("$.components.schemas");
                LinkedHashMap externalDoc = jsonContext.read("$.externalDocs");

                assertTrue(swaggerInfo.get("description").toString().contains("API"), apiCatalogSwagger);
                assertThat(swaggerServer, endsWith(""));
                assertEquals("External documentation", externalDoc.get("description"), apiCatalogSwagger);

                assertFalse(paths.isEmpty(), apiCatalogSwagger);
                assertNotNull(paths.get("/greeting"), apiCatalogSwagger);

                assertFalse(definitions.isEmpty(), apiCatalogSwagger);
                assertNotNull(definitions.get("Greeting"), apiCatalogSwagger);
            }

            // Verify that by default v1 swagger is returned.
            @Test
            void givenUrlForContainer() throws IOException {
                HttpResponse response = getResponse(DISCOVERABLE_CLIENT_CONTAINER_ENDPOINT, HttpStatus.SC_OK);
                String containerJsonResponse = EntityUtils.toString(response.getEntity());
                DocumentContext containerJsonContext = JsonPath.parse(containerJsonResponse);

                validateContainer(containerJsonContext);

                // Get Discoverable Client swagger
                String dcJsonResponse = containerJsonContext.read("$[0].services[0]").toString();
                DocumentContext dcJsonContext = JsonPath.parse(dcJsonResponse);

                validateDiscoverableClientApiV1(dcJsonResponse, dcJsonContext);
            }
        }
    }

    @Test
    void givenApis_whenGetContainer_thenApisReturned() throws IOException {
        HttpResponse response = getResponse(DISCOVERABLE_CLIENT_CONTAINER_ENDPOINT, HttpStatus.SC_OK);
        String containerJsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext containerJsonContext = JsonPath.parse(containerJsonResponse);

        validateContainer(containerJsonContext);

        LinkedHashMap<String, LinkedHashMap<String, String>> apis = containerJsonContext.read("$[0].services[0].apis");

        assertNotNull(apis.get("default"));
        assertNotNull(apis.get("zowe.apiml.discoverableclient.rest v2.0.0"));
        assertNotNull(apis.get("zowe.apiml.discoverableclient.rest v1.0.0"));
        assertNotNull(apis.get("zowe.apiml.discoverableclient.ws v1.0.0"));

        LinkedHashMap defaultApi = apis.get("default");
        assertEquals("zowe.apiml.discoverableclient.rest", defaultApi.get("apiId"));
        assertEquals("api/v1", defaultApi.get("gatewayUrl"));
        assertEquals("1.0.0", defaultApi.get("version"));
        assertThat((String) defaultApi.get("swaggerUrl"), endsWith("/discoverableclient/v3/api-docs/apiv1"));
        assertEquals("https://www.zowe.org", defaultApi.get("documentationUrl"));
        assertEquals(true, defaultApi.get("defaultApi"));

        JSONArray codeSnippets = (JSONArray) defaultApi.get("codeSnippet");
        assertEquals(2, codeSnippets.size());
        LinkedHashMap<String, String> codeSnippet = (LinkedHashMap<String, String>) codeSnippets.get(0);
        assertEquals("/greeting", codeSnippet.get("endpoint"));
        assertNotNull(codeSnippet.get("codeBlock"));
        assertThat(codeSnippet.get("codeBlock"), not(is(emptyString())));
        assertEquals("java", codeSnippet.get("language"));
    }

    @Nested
    class WhenGettingDifferenceBetweenVersions {
        @Nested
        class ReturnDifference {
            @Test
            void givenValidServiceAndVersions() throws Exception {
                final HttpResponse response = getResponse(API_SERVICE_VERSION_DIFF_ENDPOINT, HttpStatus.SC_OK);

                //When
                final String textResponse = EntityUtils.toString(response.getEntity());
                assertThat(textResponse, startsWith("<!DOCTYPE html><html lang=\"en\">"));
                assertThat(textResponse, containsString("<header><h1>Api Change Log</h1></header>"));
                assertThat(textResponse, containsString(
                    "<div><h2>What&#x27;s New</h2><hr><ol><li><span class=\"GET\">GET</span>"));
                assertThat(textResponse, containsString(
                    "<div><h2>What&#x27;s Deleted</h2><hr><ol><li><span class=\"GET\">GET</span>"));
            }
        }

        @Nested
        class ReturnNotFound {
            @Test
            void givenWrongVersion() throws Exception {
                getResponse(API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_VERSION, HttpStatus.SC_NOT_FOUND);
            }

            @Test
            void givenWrongService() throws Exception {
                getResponse(API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_SERVICE, HttpStatus.SC_NOT_FOUND);
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

    private void validateContainer(DocumentContext containerJsonContext) {
        assertEquals("cademoapps", containerJsonContext.read("$[0].id"));
        assertEquals("Sample API Mediation Layer Applications", containerJsonContext.read("$[0].title"));
        assertEquals("UP", containerJsonContext.read("$[0].status"));
    }

    private void validateDiscoverableClientApiV1(String jsonResponse, DocumentContext jsonContext) throws IOException {
        String apiCatalogSwagger = "\n**************************\n" +
            "Integration Test: Discoverable Client Swagger" +
            "\n**************************\n" +
            jsonResponse +
            "\n**************************\n";

        // When
        LinkedHashMap swaggerInfo = jsonContext.read("$.info");
        String swaggerServer = jsonContext.read("$.servers[0].url");
        LinkedHashMap paths = jsonContext.read("$.paths");
        LinkedHashMap definitions = jsonContext.read("$.components.schemas");
        LinkedHashMap externalDoc = jsonContext.read("$.externalDocs");

        // Then
        assertTrue(swaggerInfo.get("description").toString().contains("API"), apiCatalogSwagger);
        assertThat(swaggerServer, endsWith("/discoverableclient/api/v1"));
        assertEquals("External documentation", externalDoc.get("description"), apiCatalogSwagger);

        assertFalse(paths.isEmpty(), apiCatalogSwagger);
        assertNotNull(paths.get("/greeting"), apiCatalogSwagger);

        assertFalse(definitions.isEmpty(), apiCatalogSwagger);

        assertNotNull(definitions.get("Greeting"), apiCatalogSwagger);
        assertNotNull(definitions.get("Pet"), apiCatalogSwagger);
        assertNotNull(definitions.get("RedirectLocation"), apiCatalogSwagger);
    }
}
