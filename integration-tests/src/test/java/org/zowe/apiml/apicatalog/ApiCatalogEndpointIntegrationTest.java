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
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.http.*;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.*;

public class ApiCatalogEndpointIntegrationTest {
    private static final String GET_ALL_CONTAINERS_ENDPOINT = "/apicatalog/api/v1/containers";
    private static final String INVALID_CONTAINER_ENDPOINT = "/apicatalog/api/v1/containerz";
    private static final String INVALID_STATUS_UPDATES_ENDPOINT = "/apicatalog/api/v1/statuz/updatez";
    private static final String GET_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/v1";
    private static final String GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/discoverableclient/v1";
    private static final String INVALID_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/v2";
    private static final String REFRESH_STATIC_APIS_ENDPOINT = "/apicatalog/api/v1/static-api/refresh";

    private String baseHost;

    @BeforeEach
    public void setUp() {
        GatewayServiceConfiguration gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String host = gatewayServiceConfiguration.getHost();
        int port = gatewayServiceConfiguration.getExternalPort();
        baseHost = host + ":" + port;
    }

    @Test
    void whenGetContainerStatuses_thenResponseWithAtLeastOneUp() throws Exception {
        final HttpResponse response = getResponse(GET_ALL_CONTAINERS_ENDPOINT, HttpStatus.SC_OK);

        // When
        final String jsonResponse = EntityUtils.toString(response.getEntity());
        DocumentContext jsonContext = JsonPath.parse(jsonResponse);
        JSONArray containerTitles = jsonContext.read("$.[*].title");
        JSONArray containerStatus = jsonContext.read("$.[*].status");

        // Then
        assertTrue("Tile title did not match: API Mediation Layer API", containerTitles.contains("API Mediation Layer API"));
        assertTrue(containerStatus.contains("UP"));
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
        assertFalse(apiCatalogSwagger, paths.isEmpty());
        assertFalse(apiCatalogSwagger, definitions.isEmpty());
        assertEquals(apiCatalogSwagger, baseHost, swaggerHost);
        assertEquals(apiCatalogSwagger, "/apicatalog/api/v1", swaggerBasePath);
        assertNull(apiCatalogSwagger, paths.get("/status/updates"));
        assertNotNull(apiCatalogSwagger, paths.get("/containers/{id}"));
        assertNotNull(apiCatalogSwagger, paths.get("/containers"));
        assertNotNull(apiCatalogSwagger, paths.get("/apidoc/{serviceId}/{apiVersion}"));
        assertNotNull(apiCatalogSwagger, definitions.get("APIContainer"));
        assertNotNull(apiCatalogSwagger, definitions.get("APIService"));
        assertNotNull(apiCatalogSwagger, definitions.get("TimeZone"));
    }

    @Test
    @TestsNotMeantForZowe
    public void whenDiscoveryClientApiDoc_thenResponseOK() throws Exception {
        final HttpResponse response = getResponse(GET_DISCOVERABLE_CLIENT_API_DOC_ENDPOINT, HttpStatus.SC_OK);

        // When
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

        // Then
        assertTrue(apiCatalogSwagger, swaggerInfo.get("description").toString().contains("API"));
        assertEquals(apiCatalogSwagger, baseHost, swaggerHost);
        assertEquals(apiCatalogSwagger, "/discoverableclient/api/v1", swaggerBasePath);
        assertEquals(apiCatalogSwagger, "External documentation", externalDoc.get("description"));

        assertFalse(apiCatalogSwagger, paths.isEmpty());
        assertNotNull(apiCatalogSwagger, paths.get("/greeting"));

        assertFalse(apiCatalogSwagger, definitions.isEmpty());

        assertNotNull(apiCatalogSwagger, definitions.get("ApiMessage"));
        assertNotNull(apiCatalogSwagger, definitions.get("ApiMessageView"));
        assertNotNull(apiCatalogSwagger, definitions.get("Greeting"));
        assertNotNull(apiCatalogSwagger, definitions.get("Pet"));
        assertNotNull(apiCatalogSwagger, definitions.get("RedirectLocation"));
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
        String cookie = HttpSecurityUtils.getCookieForGateway();
        HttpSecurityUtils.addCookie(request, cookie);

        // When
        HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));

        return response;
    }

}
