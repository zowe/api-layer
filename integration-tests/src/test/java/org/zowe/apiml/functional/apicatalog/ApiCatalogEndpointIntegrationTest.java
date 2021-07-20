/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.functional.apicatalog;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.CatalogTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.http.HttpSecurityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;

@CatalogTest
class ApiCatalogEndpointIntegrationTest implements TestWithStartedInstances  {
    private static final String GET_ALL_CONTAINERS_ENDPOINT = "/apicatalog/api/v1/containers";
    private static final String GET_CONTAINER_BY_ID_ENDPOINT = "/apicatalog/api/v1/containers/apimediationlayer";
    private static final String GET_CONTAINER_BY_INVALID_ID_ENDPOINT = "/apicatalog/api/v1/containers/bad";
    private static final String GET_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/v1";
    private static final String INVALID_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/v2";
    private static final String REFRESH_STATIC_APIS_ENDPOINT = "/apicatalog/api/v1/static-api/refresh";

    private String baseHost;

    @BeforeEach
    void setUp() {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String host = gatewayServiceConfiguration.getHost();
        int port = gatewayServiceConfiguration.getExternalPort();
        baseHost = host + ":" + port;
    }

    @Nested
    class Containers {
        @Test
        void whenGetContainerStatuses_thenResponseWithAtLeastOneUp() throws Exception {
            final HttpResponse response = getResponse(GET_ALL_CONTAINERS_ENDPOINT, HttpStatus.SC_OK);

            // When
            final String jsonResponse = EntityUtils.toString(response.getEntity());
            DocumentContext jsonContext = JsonPath.parse(jsonResponse);
            JSONArray containerTitles = jsonContext.read("$.[*].title");
            JSONArray containerStatus = jsonContext.read("$.[*].status");

            // Then
            assertTrue(containerTitles.contains("API Mediation Layer API"), "Tile title did not match: API Mediation Layer API");
            assertTrue(containerStatus.contains("UP"));
        }

        @Test
        @Disabled("The test is flaky, update needed.")
        void givenApiCatalog_whenGetContainerById_thenResponseOk() throws IOException {
            final HttpResponse response = getResponse(GET_CONTAINER_BY_ID_ENDPOINT, HttpStatus.SC_OK);

            final String jsonResponse = EntityUtils.toString(response.getEntity());
            DocumentContext jsonContext = JsonPath.parse(jsonResponse);
            JSONArray containerTitles = jsonContext.read("$.[*].title");
            JSONArray containerStatus = jsonContext.read("$.[*].status");

            assertTrue(containerTitles.contains("API Mediation Layer API"), "Tile title did not match: API Mediation Layer API");
            assertTrue(containerStatus.contains("UP"));
        }

        @Test
        void givenApiCatalog_whenGetContainerByInvalidId_thenResponseOk() throws IOException {
            final HttpResponse response = getResponse(GET_CONTAINER_BY_INVALID_ID_ENDPOINT, HttpStatus.SC_OK);
            assertEquals("[]", EntityUtils.toString(response.getEntity()));
        }
    }

    @Nested
    class ApiDoc {
        @Test
        // Functional
        void whenCatalogApiDoc_thenResponseOK() throws Exception {
            final HttpResponse response = getResponse(GET_API_CATALOG_API_DOC_ENDPOINT, HttpStatus.SC_OK);

            // When
            final String jsonResponse = EntityUtils.toString(response.getEntity());

            String apiCatalogSwagger = "\n**************************\n" +
                "Integration Test: API Catalog Swagger" +
                "\n**************************\n" +
                jsonResponse +
                "\n**************************\n";
            DocumentContext jsonContext = JsonPath.parse(jsonResponse);

            String swaggerHost = jsonContext.read("$.host");
            String swaggerBasePath = jsonContext.read("$.basePath");
            LinkedHashMap paths = jsonContext.read("$.paths");
            LinkedHashMap definitions = jsonContext.read("$.definitions");

            // Then
            assertFalse(paths.isEmpty(), apiCatalogSwagger);
            assertFalse(definitions.isEmpty(), apiCatalogSwagger);
            assertEquals(baseHost, swaggerHost, apiCatalogSwagger);
            assertEquals("/apicatalog/api/v1", swaggerBasePath, apiCatalogSwagger);
            assertNull(paths.get("/status/updates"), apiCatalogSwagger);
            assertNotNull(paths.get("/containers/{id}"), apiCatalogSwagger);
            assertNotNull(paths.get("/containers"), apiCatalogSwagger);
            assertNotNull(paths.get("/apidoc/{serviceId}/{apiVersion}"), apiCatalogSwagger);
            assertNotNull(definitions.get("APIContainer"), apiCatalogSwagger);
            assertNotNull(definitions.get("APIService"), apiCatalogSwagger);
            assertNotNull(definitions.get("TimeZone"), apiCatalogSwagger);
        }

        @Test
        void whenInvalidApiDocVersion_thenReturnFirstDoc() throws Exception {
            final HttpResponse response = getResponse(INVALID_API_CATALOG_API_DOC_ENDPOINT, HttpStatus.SC_OK);

            // When
            final String jsonResponse = EntityUtils.toString(response.getEntity());
            String swaggerBasePath = JsonPath.parse(jsonResponse).read("$.basePath");

            assertEquals("/apicatalog/api/v1", swaggerBasePath);
        }
    }

    // Functional
    @Test
    void whenCallStaticApiRefresh_thenResponseOk() throws IOException {
        final HttpResponse response = getStaticApiRefreshResponse(REFRESH_STATIC_APIS_ENDPOINT, HttpStatus.SC_OK);

        // When
        final String jsonResponse = EntityUtils.toString(response.getEntity());

        JSONArray errors = JsonPath.parse(jsonResponse).read("$.errors");

        assertEquals("[]", errors.toString());
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

    // Execute the refresh static apis endpoint and check the response for a return code
    private HttpResponse getStaticApiRefreshResponse(String endpoint, int returnCode) throws IOException {
        URI uri = HttpRequestUtils.getUriFromGateway(endpoint);
        HttpPost request = new HttpPost(uri);
        request.addHeader("Accept", "application/json");

        String cookie = HttpSecurityUtils.getCookieForGateway();
        HttpSecurityUtils.addCookie(request, cookie);

        // When
        HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));

        return response;
    }
}
