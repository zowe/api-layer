/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger.api;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import jakarta.validation.UnexpectedTypeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ApiDocV3ServiceTest {

    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String SERVICE_ID = "serviceId";
    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String EXTERNAL_DOCUMENTATION = "External documentation";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String API_ID = "org.zowe.apicatalog";
    private static final String API_VERSION = "v3.0.0";
    private static final String SEPARATOR = "/";
    private static final String URL_ENCODED_SPACE = "%20";

    private GatewayClient gatewayClient;
    private ApiDocV3Service apiDocV3Service;

    @BeforeEach
    void setUp() {
        ServiceAddress gatewayConfigProperties = getProperties();
        gatewayClient = new GatewayClient(gatewayConfigProperties);
        apiDocV3Service = new ApiDocV3Service(gatewayClient);
        ReflectionTestUtils.setField(apiDocV3Service, "scheme", "https");
    }

    @Nested
    class WhenApiDocTransform {
        @Nested
        class ThenCheckUpdatedValues {
            @Test
            void givenOpenApiValidJson() {
                List<Server> servers = new ArrayList<>();
                servers.add(0, new Server().url("/api/v1/apicatalog"));
                servers.add(1, new Server().url("http://localhost:8080/apicatalog"));
                servers.add(2, new Server().url("http://localhost2:8080/serviceId"));
                OpenAPI dummyOpenApiObject = getDummyOpenApiObject(servers, false);
                String apiDocContent = convertOpenApiToJson(dummyOpenApiObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

                RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);
                ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc",null,  "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

                String actualContent = apiDocV3Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                OpenAPI actualSwagger = convertJsonToOpenApi(actualContent);
                assertNotNull(actualSwagger);
                String expectedDescription = dummyOpenApiObject.getInfo().getDescription() +
                    "\n\n" +
                    SWAGGER_LOCATION_LINK +
                    "(" +
                    gatewayClient.getGatewayConfigProperties().getScheme() +
                    "://" +
                    gatewayClient.getGatewayConfigProperties().getHostname() +
                    SEPARATOR +
                    CoreService.API_CATALOG.getServiceId() +
                    CATALOG_VERSION +
                    CATALOG_APIDOC_ENDPOINT +
                    SEPARATOR +
                    SERVICE_ID +
                    SEPARATOR +
                    API_ID +
                    URL_ENCODED_SPACE +
                    API_VERSION +
                    ")";

                assertEquals("https://localhost:10010/serviceId/api/v1", actualSwagger.getServers().get(0).getUrl());
                assertThat(actualSwagger.getPaths(), samePropertyValuesAs(dummyOpenApiObject.getPaths()));

                assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
                assertEquals(EXTERNAL_DOCUMENTATION, actualSwagger.getExternalDocs().getDescription());
                assertEquals(apiDocInfo.getApiInfo().getDocumentationUrl(), actualSwagger.getExternalDocs().getUrl());
            }

            @Test
            void givenOpenApiValidYaml() {
                List<Server> servers = new ArrayList<>();
                servers.add(0, new Server().url("/api/v1/apicatalog"));
                servers.add(1, new Server().url("http://localhost:8080/apicatalog"));
                servers.add(2, new Server().url("http://localhost2:8080/serviceId"));
                OpenAPI dummyOpenApiObject = getDummyOpenApiObject(servers, false);
                String apiDocContent = convertOpenApiToYaml(dummyOpenApiObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

                RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);
                ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc", null, "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

                String actualContent = apiDocV3Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                OpenAPI actualSwagger = convertYamlToOpenApi(actualContent);
                assertNotNull(actualSwagger);
                String expectedDescription = dummyOpenApiObject.getInfo().getDescription() +
                    "\n\n" +
                    SWAGGER_LOCATION_LINK +
                    "(" +
                    gatewayClient.getGatewayConfigProperties().getScheme() +
                    "://" +
                    gatewayClient.getGatewayConfigProperties().getHostname() +
                    SEPARATOR +
                    CoreService.API_CATALOG.getServiceId() +
                    CATALOG_VERSION +
                    CATALOG_APIDOC_ENDPOINT +
                    SEPARATOR +
                    SERVICE_ID +
                    SEPARATOR +
                    API_ID +
                    URL_ENCODED_SPACE +
                    API_VERSION +
                    ")";

                assertEquals("https://localhost:10010/serviceId/api/v1", actualSwagger.getServers().get(0).getUrl());
                assertThat(actualSwagger.getPaths(), samePropertyValuesAs(dummyOpenApiObject.getPaths()));

                assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
                assertEquals(EXTERNAL_DOCUMENTATION, actualSwagger.getExternalDocs().getDescription());
                assertEquals(apiDocInfo.getApiInfo().getDocumentationUrl(), actualSwagger.getExternalDocs().getUrl());
            }
        }

        @Nested
        class ThenThrowException {
            @Test
            void givenEmptyJson() {
                String invalidJson = "";
                ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc",null,  "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, invalidJson, null);

                Exception exception = assertThrows(UnexpectedTypeException.class, () -> apiDocV3Service.transformApiDoc(SERVICE_ID, apiDocInfo));
                assertEquals("[Null or empty definition]", exception.getMessage());
            }

            @Test
            void givenInvalidJson() {
                String invalidJson = "nonsense";
                String error = "[Cannot construct instance of `java.util.LinkedHashMap` (although at least one Creator exists): no String-argument constructor/factory method to deserialize from String value ('nonsense')\n" +
                    " at [Source: UNKNOWN; byte offset: #UNKNOWN]]";
                ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc",null, "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, invalidJson, null);

                Exception exception = assertThrows(UnexpectedTypeException.class, () -> apiDocV3Service.transformApiDoc(SERVICE_ID, apiDocInfo));
                assertEquals(error, exception.getMessage());
            }
        }

        /**
         * GH #637
         */
        @Test
        void givenValidApiDoc_thenDontCapitalizeEnums() {
            ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc", null, "https://www.zowe.org");
            String content = "{\"openapi\":\"3.0.1\",\"info\":{\"title\":\"ZoweKotlinSampleRESTAPI\",\"description\":\"SampleKotlinSpringBootRESTAPIforZowe.\",\"version\":\"1.0.0\"},\"servers\":[{\"url\":\"https://localhost:10090\",\"description\":\"Generatedserverurl\"}],\"paths\":{\"/api/v1/greeting\":{\"get\":{\"tags\":[\"Greeting\"],\"summary\":\"Returnsagreetingforthenamepassed\",\"operationId\":\"getGreeting\",\"parameters\":[{\"name\":\"name\",\"in\":\"query\",\"description\":\"Personorobjecttobegreeted\",\"required\":false,\"schema\":{\"type\":\"string\"}}],\"responses\":{\"200\":{\"description\":\"Successfulgreeting\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/Greeting\"}}}},\"404\":{\"description\":\"Notfound\",\"content\":{\"application/json\":{\"schema\":{\"$ref\":\"#/components/schemas/ApiMessage\"}}}}}}}},\"components\":{\"schemas\":{\"Greeting\":{\"required\":[\"content\",\"id\",\"languageTag\"],\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"integer\",\"description\":\"GeneratedsequenceIDofthemessage\",\"format\":\"int64\"},\"content\":{\"type\":\"string\",\"description\":\"Thegreetingmessage\"},\"languageTag\":{\"type\":\"string\",\"description\":\"Thelocalelanguagetagusedforthismessage\"}}},\"ApiMessage\":{\"type\":\"object\",\"properties\":{\"messages\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/components/schemas/Message\"}}}},\"Message\":{\"type\":\"object\",\"properties\":{\"messageType\":{\"type\":\"string\",\"enum\":[\"ERROR\",\"WARNING\",\"INFO\",\"DEBUG\",\"TRACE\"]},\"messageNumber\":{\"type\":\"string\"},\"messageContent\":{\"type\":\"string\"},\"messageAction\":{\"type\":\"string\"},\"messageReason\":{\"type\":\"string\"},\"messageKey\":{\"type\":\"string\"},\"messageParameters\":{\"type\":\"array\",\"items\":{\"type\":\"object\"}},\"messageInstanceId\":{\"type\":\"string\"},\"messageComponent\":{\"type\":\"string\"},\"messageSource\":{\"type\":\"string\"}}}}}}";
            RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");

            RoutedServices routedServices = new RoutedServices();
            routedServices.addRoutedService(routedService);

            ApiDocInfo info = new ApiDocInfo(apiInfo, content, routedServices);

            assertThat(content, not(containsString("\"style\":\"form\"")));
            assertThat(content, not(containsString("\"style\":\"FORM\"")));

            String actualContent = apiDocV3Service.transformApiDoc(SERVICE_ID, info);

            assertThat(actualContent, containsString("\"style\":\"form\""));
            assertThat(actualContent, not(containsString("\"style\":\"FORM\"")));
        }

        private void verifyOpenApi3(OpenAPI openAPI) {
            assertEquals("Sample of OpenAPI v3", openAPI.getInfo().getTitle());
            assertEquals("Main server", openAPI.getServers().get(0).getDescription());
            assertEquals("receive", openAPI.getPaths().get("/service/api/v1/endpoint").getPost().getOperationId());
            assertNotNull(openAPI.getPaths().get("/service/api/v1/endpoint").getPost().getResponses().get("204"));
        }

        @Test
        void givenInputFile_thenParseItCorrectly() throws IOException {
            ServiceAddress gatewayConfigProperties = ServiceAddress.builder().scheme("https").hostname("localhost").build();
            GatewayClient gatewayClient = new GatewayClient(gatewayConfigProperties);

            AtomicReference<OpenAPI> openApiHolder = new AtomicReference<>();
            ApiDocV3Service apiDocV3Service = new ApiDocV3Service(gatewayClient) {
                @Override
                protected void updateExternalDoc(OpenAPI openAPI, ApiDocInfo apiDocInfo) {
                    super.updateExternalDoc(openAPI, apiDocInfo);
                    openApiHolder.set(openAPI);
                }
            };
            String transformed = apiDocV3Service.transformApiDoc("serviceId", new ApiDocInfo(
                    mock(ApiInfo.class),
                    IOUtils.toString(new ClassPathResource("swagger/openapi3.json").getInputStream(), StandardCharsets.UTF_8),
                    mock(RoutedServices.class)
            ));
            assertNotNull(transformed);
            verifyOpenApi3(openApiHolder.get());
        }

    }

    private String convertOpenApiToJson(OpenAPI openApi) {
        ObjectMapper objectMapper = new ObjectMapper();
        return writeOpenApiAsString(openApi, objectMapper);
    }

    private String convertOpenApiToYaml(OpenAPI openApi) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return writeOpenApiAsString(openApi, objectMapper);
    }

    private String writeOpenApiAsString(OpenAPI openApi, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(openApi);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private OpenAPI convertJsonToOpenApi(String content) {
        ObjectMapper objectMapper = new ObjectMapper();
        return readStringToOpenApi(content, objectMapper);
    }

    private OpenAPI convertYamlToOpenApi(String content) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return readStringToOpenApi(content, objectMapper);
    }

    private OpenAPI readStringToOpenApi(String content, ObjectMapper objectMapper) {
        OpenAPI openAPI = null;
        try {
            openAPI = objectMapper.readValue(content, OpenAPI.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return openAPI;
    }

    private OpenAPI getDummyOpenApiObject(List<Server> servers, boolean apimlHidden) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(new Paths());
        openAPI.setTags(new ArrayList<>());
        openAPI.setOpenapi("3.0.0");
        openAPI.setServers(servers);

        Info info = new Info();
        info.setTitle("API Catalog");
        info.setDescription("REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.");
        info.setVersion("1.0.0");
        openAPI.setInfo(info);
        openAPI.addExtension("x-custom", "value1");
        openAPI.getInfo().addExtension("x-custom", openAPI.getExtensions());
        openAPI.getServers().get(0).addExtension("x-custom", openAPI.getExtensions());
        openAPI.getServers().get(1).addExtension("x-custom", openAPI.getExtensions());
        openAPI.getServers().get(2).addExtension("x-custom", openAPI.getExtensions());
        Tag tag = new Tag();
        tag.setName("API Catalog");
        tag.setDescription("Current state information");
        tag.setExtensions(openAPI.getExtensions());
        openAPI.getTags().add(tag);
        if (apimlHidden) {
            tag = new Tag();
            tag.setName(HIDDEN_TAG);
            openAPI.getTags().add(tag);
        }
        openAPI.getPaths().put("/api1", new PathItem().$ref("test"));
        openAPI.getPaths().put("/api2", new PathItem().$ref("test"));
        return openAPI;
    }

    private ServiceAddress getProperties() {
        return ServiceAddress.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }
}
