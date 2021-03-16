/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import net.minidev.json.JSONArray;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.*;
import org.zowe.apiml.util.TestWithStartedInstances;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

class ApiCatalogEndpointIntegrationTest implements TestWithStartedInstances  {
    private static final String GET_ALL_CONTAINERS_ENDPOINT = "/apicatalog/api/v1/containers";
    private static final String INVALID_CONTAINER_ENDPOINT = "/apicatalog/api/v1/containerz";
    private static final String GET_CONTAINER_BY_ID_ENDPOINT = "/apicatalog/api/v1/containers/apimediationlayer";
    private static final String GET_CONTAINER_BY_INVALID_ID_ENDPOINT = "/apicatalog/api/v1/containers/bad";
    private static final String GET_DISCOVERABLE_CLIENT_CONTAINER_ENDPOINT = "/apicatalog/api/v1/containers/cademoapps";
    private static final String INVALID_STATUS_UPDATES_ENDPOINT = "/apicatalog/api/v1/statuz/updatez";
    private static final String GET_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/v1";
    private static final String GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/v1";
    private static final String GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT_V2 = "/apicatalog/api/v1/apidoc/discoverableclient/v2";
    private static final String INVALID_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/v2";
    private static final String REFRESH_STATIC_APIS_ENDPOINT = "/apicatalog/api/v1/static-api/refresh";
    private static final String GET_API_SERVICE_VERSION_DIFF_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/v1/v2";
    private static final String GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_VERSION = "/apicatalog/api/v1/apidoc/discoverableclient/v1/v3";
    private static final String GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_SERVICE = "/apicatalog/api/v1/apidoc/invalidService/v1/v2";

    private String baseHost;

    @BeforeEach
    void setUp() {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String host = gatewayServiceConfiguration.getHost();
        int port = gatewayServiceConfiguration.getExternalPort();
        baseHost = host + ":" + port;
    }

    @Test
    void whenGetContainerStatuses_thenResponseWithAtLeastOneUp() throws Exception {
        fail();
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

    @Test
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
    @TestsNotMeantForZowe
    public void whenDiscoveryClientApiDoc_thenResponseOK() throws Exception {
        final HttpResponse response = getResponse(GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT, HttpStatus.SC_OK);
        String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);

        validateDiscoverableClientApiV1(jsonResponse, jsonContext);
    }

    @Test
    @TestsNotMeantForZowe
    public void givenDiscoveryClient_whenGetApiDocV2_thenResponseOk() throws IOException {
        final HttpResponse response = getResponse(GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT_V2, HttpStatus.SC_OK);
        final String jsonResponse = EntityUtils.toString(response.getEntity());

        String apiCatalogSwagger = "\n**************************\n" +
            "Integration Test: Discoverable Client Swagger" +
            "\n**************************\n" +
            jsonResponse +
            "\n**************************\n";
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);

        LinkedHashMap swaggerInfo = jsonContext.read("$.info");
        String swaggerHost = jsonContext.read("$.host");
        String swaggerBasePath = jsonContext.read("$.basePath");
        LinkedHashMap paths = jsonContext.read("$.paths");
        LinkedHashMap definitions = jsonContext.read("$.definitions");
        LinkedHashMap externalDoc = jsonContext.read("$.externalDocs");

        assertTrue(swaggerInfo.get("description").toString().contains("API"), apiCatalogSwagger);
        assertEquals(baseHost, swaggerHost, apiCatalogSwagger);
        assertEquals("/discoverableclient/api/v2", swaggerBasePath, apiCatalogSwagger);
        assertEquals("External documentation", externalDoc.get("description"), apiCatalogSwagger);

        assertFalse(paths.isEmpty(), apiCatalogSwagger);
        assertNotNull(paths.get("/greeting"), apiCatalogSwagger);

        assertFalse(definitions.isEmpty(), apiCatalogSwagger);
        assertNotNull(definitions.get("Greeting"), apiCatalogSwagger);
    }

    @Test
    @TestsNotMeantForZowe
    void givenDiscoveryClient_whenGetContainerById_thenGetDefaultApiVersionSwagger() throws IOException {
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

    @Test
    void whenMisSpeltContainersEndpoint_thenNotFoundResponseWithAPIMessage() throws Exception {
        HttpResponse response = getResponse(INVALID_CONTAINER_ENDPOINT, HttpStatus.SC_NOT_FOUND);
        final String htmlResponse = EntityUtils.toString(response.getEntity());
        Document doc = Jsoup.parse(htmlResponse);
        String title = doc.title();
        Elements h1 = doc.select("h1:first-child");
        Elements a = doc.select("a");
        assertNotNull(title);
        assertEquals("404 Not Found", title);
        assertEquals("404 Page Not Found", h1.text());
        assertEquals("Go to Dashboard", a.text());
    }

    @Test
    void whenMisSpeltStatusUpdateEndpoint_thenNotFoundResponse() throws Exception {
        getResponse(INVALID_STATUS_UPDATES_ENDPOINT, HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void whenInvalidApiDocVersion_thenReturnFirstDoc() throws Exception {
        final HttpResponse response = getResponse(INVALID_API_CATALOG_API_DOC_ENDPOINT, HttpStatus.SC_OK);

        // When
        final String jsonResponse = EntityUtils.toString(response.getEntity());
        String swaggerBasePath = JsonPath.parse(jsonResponse).read("$.basePath");

        assertEquals("/apicatalog/api/v1", swaggerBasePath);
    }

    @Test
    void whenCallStaticApiRefresh_thenResponseOk() throws IOException {
        final HttpResponse response = getStaticApiRefreshResponse(REFRESH_STATIC_APIS_ENDPOINT, HttpStatus.SC_OK);

        // When
        final String jsonResponse = EntityUtils.toString(response.getEntity());

        JSONArray errors = JsonPath.parse(jsonResponse).read("$.errors");

        assertEquals("[]", errors.toString());
    }

    @Test
    void whenCallGetApiDiff_thenReturnDiff() throws Exception {
        final HttpResponse response = getResponse(GET_API_SERVICE_VERSION_DIFF_ENDPOINT, HttpStatus.SC_OK);

        //When
        final String textResponse = EntityUtils.toString(response.getEntity());
        assertThat(textResponse, CoreMatchers.startsWith("<!DOCTYPE html><html lang=\"en\">"));
        assertThat(textResponse, CoreMatchers.containsString("<header><h1>Api Change Log</h1></header>"));
        assertThat(textResponse, CoreMatchers.containsString(
            "<div><h2>What&#x27;s New</h2><hr><ol><li><span class=\"GET\">GET</span>/greeting/{yourName} <span>Get a greeting</span></li></ol></div>"));
        assertThat(textResponse, CoreMatchers.containsString(
            "<div><h2>What&#x27;s Deleted</h2><hr><ol><li><span class=\"GET\">GET</span><del>/{yourName}/greeting</del><span> Get a greeting</span></li>"));
    }

    @Test
    void whenCallGetApiDiffWithWrongVersion_thenReturnNotFound() throws Exception {
        getResponse(GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_VERSION, HttpStatus.SC_NOT_FOUND);
    }

    @Test
    void whenCallGetApiDiffWithWrongService_thenReturnNotFound() throws Exception {
        getResponse(GET_API_SERVICE_VERSION_DIFF_ENDPOINT_WRONG_SERVICE, HttpStatus.SC_NOT_FOUND);
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

    private void validateDiscoverableClientApiV1(String jsonResponse, DocumentContext jsonContext) throws IOException {
        String apiCatalogSwagger = "\n**************************\n" +
            "Integration Test: Discoverable Client Swagger" +
            "\n**************************\n" +
            jsonResponse +
            "\n**************************\n";

        // When
        LinkedHashMap swaggerInfo = jsonContext.read("$.info");
        String swaggerHost = jsonContext.read("$.host");
        String swaggerBasePath = jsonContext.read("$.basePath");
        LinkedHashMap paths = jsonContext.read("$.paths");
        LinkedHashMap definitions = jsonContext.read("$.definitions");
        LinkedHashMap externalDoc = jsonContext.read("$.externalDocs");

        // Then
        assertTrue(swaggerInfo.get("description").toString().contains("API"), apiCatalogSwagger);
        assertEquals(baseHost, swaggerHost, apiCatalogSwagger);
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
