/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog;

import com.ca.mfaas.utils.config.ConfigReader;
import com.ca.mfaas.utils.config.GatewayServiceConfiguration;
import com.ca.mfaas.utils.http.HttpClientUtils;
import com.ca.mfaas.utils.http.HttpRequestUtils;
import com.ca.mfaas.utils.http.HttpSecurityUtils;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import lombok.extern.slf4j.Slf4j;
import net.minidev.json.JSONArray;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.LinkedHashMap;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

@Slf4j
public class ApiCatalogEndpointIntegrationTest {
    private String getAllContainersEndpoint = "/api/v1/apicatalog/containers";
    private String invalidContainerEndpoint = "/api/v1/apicatalog/containerz";
    private String invalidStatusUpdatesEndpoint = "/api/v1/apicatalog/statuz/updatez";
    private String getApiCatalogApiDocEndpoint = "/api/v1/apicatalog/apidoc/apicatalog/v1";
    private String invalidApiCatalogApiDocEndpoint = "/api/v1/apicatalog/apidoc/apicatalog/v2";

    private GatewayServiceConfiguration gatewayServiceConfiguration;

    private String scheme;
    private String host;
    private int port;
    private String baseHost;

    @Before
    public void setUp() throws URISyntaxException {
        gatewayServiceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        scheme = gatewayServiceConfiguration.getScheme();
        host = gatewayServiceConfiguration.getHost();
        port = gatewayServiceConfiguration.getPort();
        baseHost = host + ":" + port;
    }

    @Test
    public void whenGetContainerStatuses_thenResponseWithAtLeastOneUp() throws Exception {
        final HttpResponse response = getResponse(getAllContainersEndpoint, HttpStatus.SC_OK);

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
    public void whenCatalogApiDoc_thenResponseOK() throws Exception {
        final HttpResponse response = getResponse(getApiCatalogApiDocEndpoint, HttpStatus.SC_OK);

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
        assertEquals(apiCatalogSwagger, "/api/v1/apicatalog", swaggerBasePath);
        assertNull(apiCatalogSwagger, paths.get("/status/updates"));
        assertNotNull(apiCatalogSwagger, paths.get("/containers/{id}"));
        assertNotNull(apiCatalogSwagger, paths.get("/containers"));
        assertNotNull(apiCatalogSwagger, paths.get("/apidoc/{service-id}/{api-version}"));
        assertNotNull(apiCatalogSwagger, definitions.get("APIContainer"));
        assertNotNull(apiCatalogSwagger, definitions.get("APIService"));
        assertNotNull(apiCatalogSwagger, definitions.get("TimeZone"));
    }

    @Test
    public void whenMisSpeltContainersEndpoint_thenNotFoundResponseWithAPIMessage() throws Exception {
        HttpResponse response = getResponse(invalidContainerEndpoint, HttpStatus.SC_NOT_FOUND);
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
    public void whenMisSpeltStatusUpdateEndpoint_thenNotFoundResponse() throws Exception {
        getResponse(invalidStatusUpdatesEndpoint, HttpStatus.SC_NOT_FOUND);
    }

    @Test
    public void whenInvalidApiDocVersion_thenInvalidResponse() throws Exception {
        getResponse(invalidApiCatalogApiDocEndpoint, HttpStatus.SC_INTERNAL_SERVER_ERROR);
    }

    /**
     * Execute the endpoint and check the response for a return code
     *
     * @param endpoint   execute thus
     * @param returnCode check for this
     * @return response
     * @throws URISyntaxException oops
     * @throws IOException        oops
     */
    private HttpResponse getResponse(String endpoint, int returnCode) throws IOException {
        HttpGet request = HttpRequestUtils.getRequest(endpoint);
        String cookie = HttpSecurityUtils.getCookieForApiCatalog();
        HttpSecurityUtils.addCookie(request, cookie);

        // When
        HttpResponse response = HttpClientUtils.client().execute(request);

        // Then
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));

        return response;
    }
}
