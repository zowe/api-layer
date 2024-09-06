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
import io.restassured.RestAssured;
import io.restassured.response.Response;
import io.restassured.specification.RequestSpecification;
import net.minidev.json.JSONArray;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.util.EntityUtils;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import org.zowe.apiml.util.TestWithStartedInstances;
import org.zowe.apiml.util.categories.CatalogTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.http.HttpClientUtils;
import org.zowe.apiml.util.http.HttpRequestUtils;
import org.zowe.apiml.util.http.HttpSecurityUtils;

import java.io.IOException;
import java.net.URI;
import java.util.LinkedHashMap;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.TestInstance.Lifecycle.PER_CLASS;
import static org.zowe.apiml.util.SecurityUtils.COOKIE_NAME;
import static org.zowe.apiml.util.SecurityUtils.gatewayToken;
import static org.zowe.apiml.util.http.HttpRequestUtils.getUriFromGateway;

@CatalogTest
class ApiCatalogEndpointIntegrationTest implements TestWithStartedInstances {
    private static final String GET_ALL_CONTAINERS_ENDPOINT = "/apicatalog/api/v1/containers";
    private static final String GET_CONTAINER_BY_ID_ENDPOINT = "/apicatalog/api/v1/containers/apimediationlayer";
    private static final String GET_CONTAINER_BY_INVALID_ID_ENDPOINT = "/apicatalog/api/v1/containers/bad";
    private static final String GET_API_CATALOG_API_DOC_DEFAULT_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog";
    private static final String GET_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/zowe.apiml.apicatalog v1.0.0";
    private static final String INVALID_API_CATALOG_API_DOC_ENDPOINT = "/apicatalog/api/v1/apidoc/apicatalog/zowe.apiml.apicatalog v18.0.0";

    private final static String UNAUTHORIZED_USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-unauthorized").get(0).getUser();
    private final static String UNAUTHORIZED_PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-unauthorized").get(0).getPassword();
    private final static String USERNAME = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getUser();
    private final static String PASSWORD = ConfigReader.environmentConfiguration().getAuxiliaryUserList().getCredentials("servicesinfo-authorized").get(0).getPassword();

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

        @Nested
        class GivenApiCatalog_thenResponseOk {
            @Test
            @Disabled("The test is flaky, update needed.")
            void whenGetContainerById() throws IOException {
                final HttpResponse response = getResponse(GET_CONTAINER_BY_ID_ENDPOINT, HttpStatus.SC_OK);

                final String jsonResponse = EntityUtils.toString(response.getEntity());
                DocumentContext jsonContext = JsonPath.parse(jsonResponse);
                JSONArray containerTitles = jsonContext.read("$.[*].title");
                JSONArray containerStatus = jsonContext.read("$.[*].status");

                assertTrue(containerTitles.contains("API Mediation Layer API"), "Tile title did not match: API Mediation Layer API");
                assertTrue(containerStatus.contains("UP"));
            }

            @Test
            void whenGetContainerByInvalidId() throws IOException {
                final HttpResponse response = getResponse(GET_CONTAINER_BY_INVALID_ID_ENDPOINT, HttpStatus.SC_OK);
                assertEquals("[]", EntityUtils.toString(response.getEntity()));
            }
        }
    }

    @Nested
    class ApiDoc {
        @Nested
        class ThenResponseOk {
            @Test
                // Functional
            void whenSpecificCatalogApiDoc() throws Exception {
                final HttpResponse response = getResponse(GET_API_CATALOG_API_DOC_ENDPOINT, HttpStatus.SC_OK);

                // When
                final String jsonResponse = EntityUtils.toString(response.getEntity());

                String apiCatalogSwagger = "\n**************************\n" +
                    "Integration Test: API Catalog Swagger" +
                    "\n**************************\n" +
                    jsonResponse +
                    "\n**************************\n";
                DocumentContext jsonContext = JsonPath.parse(jsonResponse);

                String swaggerServer = jsonContext.read("$.servers[0].url");
                LinkedHashMap<?, ?> paths = jsonContext.read("$.paths");
                LinkedHashMap<?, ?> componentSchemas = jsonContext.read("$.components.schemas");
                LinkedHashMap<?, ?> securitySchemes = jsonContext.read("$.components.securitySchemes");

                // Then
                assertFalse(paths.isEmpty(), apiCatalogSwagger);
                assertFalse(componentSchemas.isEmpty(), apiCatalogSwagger);
                assertEquals("https://" + baseHost + "/apicatalog/api/v1", swaggerServer, apiCatalogSwagger);
                assertNull(paths.get("/status/updates"), apiCatalogSwagger);
                assertNotNull(paths.get("/containers/{id}"), apiCatalogSwagger);
                assertNotNull(paths.get("/containers"), apiCatalogSwagger);
                assertNotNull(paths.get("/apidoc/{serviceId}/{apiId}"), apiCatalogSwagger);
                assertNotNull(componentSchemas.get("APIContainer"), apiCatalogSwagger);
                assertNotNull(componentSchemas.get("APIService"), apiCatalogSwagger);
                assertNotNull(securitySchemes.get("BasicAuthorization"), apiCatalogSwagger);
                assertNotNull(securitySchemes.get("CookieAuth"), apiCatalogSwagger);
            }

            @Test
            void whenDefaultCatalogApiDoc() throws Exception {
                final HttpResponse response = getResponse(GET_API_CATALOG_API_DOC_DEFAULT_ENDPOINT, HttpStatus.SC_OK);

                // When
                final String jsonResponse = EntityUtils.toString(response.getEntity());

                String apiCatalogSwagger = "\n**************************\n" +
                    "Integration Test: API Catalog Swagger" +
                    "\n**************************\n" +
                    jsonResponse +
                    "\n**************************\n";
                DocumentContext jsonContext = JsonPath.parse(jsonResponse);

                String swaggerServer = jsonContext.read("$.servers[0].url");
                LinkedHashMap<?, ?> paths = jsonContext.read("$.paths");
                LinkedHashMap<?, ?> componentSchemas = jsonContext.read("$.components.schemas");
                LinkedHashMap<?, ?> securitySchemes = jsonContext.read("$.components.securitySchemes");

                // Then
                assertFalse(paths.isEmpty(), apiCatalogSwagger);
                assertFalse(componentSchemas.isEmpty(), apiCatalogSwagger);
                assertEquals("https://" + baseHost + "/apicatalog/api/v1", swaggerServer, apiCatalogSwagger);
                assertNull(paths.get("/status/updates"), apiCatalogSwagger);
                assertNotNull(paths.get("/containers/{id}"), apiCatalogSwagger);
                assertNotNull(paths.get("/containers"), apiCatalogSwagger);
                assertNotNull(paths.get("/apidoc/{serviceId}/{apiId}"), apiCatalogSwagger);
                assertNotNull(componentSchemas.get("APIContainer"), apiCatalogSwagger);
                assertNotNull(componentSchemas.get("APIService"), apiCatalogSwagger);
                assertNotNull(securitySchemes.get("BasicAuthorization"), apiCatalogSwagger);
                assertNotNull(securitySchemes.get("CookieAuth"), apiCatalogSwagger);
            }
        }

        @Test
        void whenInvalidApiDocVersion_thenReturn404() throws Exception {
            getResponse(INVALID_API_CATALOG_API_DOC_ENDPOINT, HttpStatus.SC_NOT_FOUND);
        }
    }

    @Nested
    @TestMethodOrder(OrderAnnotation.class)
    @TestInstance(PER_CLASS)
    class StaticApis {

        private static final String STATIC_DEFINITION_GENERATE_ENDPOINT = "/apicatalog/api/v1/static-api/generate";
        private static final String STATIC_DEFINITION_DELETE_ENDPOINT = "/apicatalog/api/v1/static-api/delete";
        private static final String REFRESH_STATIC_APIS_ENDPOINT = "/apicatalog/api/v1/static-api/refresh";
        private String staticDefinitionServiceId = "a" + UUID.randomUUID().toString().replace("-", "").substring(0, 10);

        @AfterAll
        void cleanupStaticDefinition() {
            given().relaxedHTTPSValidation()
                .when()
                .header("Service-Id", staticDefinitionServiceId)
                .cookie(COOKIE_NAME, gatewayToken())
                .delete(getUriFromGateway(STATIC_DEFINITION_DELETE_ENDPOINT));
        }

        @Test
        @Order(1)
        void whenCallStaticApiRefresh_thenResponseOk() throws IOException {
            getStaticApiResponse(REFRESH_STATIC_APIS_ENDPOINT, null, HttpStatus.SC_OK, null, gatewayToken(USERNAME, PASSWORD));
        }

        @Test
        @Order(30)
        void whenCallStaticDefinitionGenerate_thenResponse201() throws IOException {
            String json = "# Dummy content";
            getStaticApiResponse(STATIC_DEFINITION_GENERATE_ENDPOINT, staticDefinitionServiceId, HttpStatus.SC_CREATED, json, gatewayToken(USERNAME, PASSWORD));
        }

        @Test
        @Order(31)
        void whenCallStaticDefinitionGenerateWithUnauthorizedUser_thenResponse403() throws IOException {
            String json = "# Dummy content";
            getStaticApiResponse(STATIC_DEFINITION_GENERATE_ENDPOINT, staticDefinitionServiceId, HttpStatus.SC_FORBIDDEN, json, gatewayToken(UNAUTHORIZED_USERNAME, UNAUTHORIZED_PASSWORD));
        }

        private Response getStaticApiResponse(String endpoint, String definitionFileName, int returnCode, String body, String JWT) throws IOException {
            URI uri = getUriFromGateway(endpoint);
            RestAssured.enableLoggingOfRequestAndResponseIfValidationFails();

            RequestSpecification requestSpecification = given().config(SslContext.tlsWithoutCert).relaxedHTTPSValidation()
                .when()
                .cookie(COOKIE_NAME, JWT)
                .header("Accept", MediaType.APPLICATION_JSON_VALUE);
            if (body != null) {
                requestSpecification
                    .header("Service-Id", definitionFileName)
                    .body(body);
            }

            return requestSpecification.post(uri).then()
                .statusCode(returnCode).extract().response();
        }
    }

    // Execute the endpoint and check the response for a return code
    private HttpResponse getResponse(String endpoint, int returnCode) throws IOException {
        HttpGet request = HttpRequestUtils.getRequest(endpoint);
        String cookie = HttpSecurityUtils.getCookieForGateway();
        HttpSecurityUtils.addCookie(request, cookie);

        HttpResponse response = HttpClientUtils.client().execute(request);
        assertThat(response.getStatusLine().getStatusCode(), equalTo(returnCode));

        return response;
    }


}
