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
import io.swagger.models.*;
import org.apache.commons.io.IOUtils;
import org.hamcrest.collection.IsMapContaining;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import jakarta.validation.UnexpectedTypeException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicReference;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiDocV2ServiceTest {

    private static final String SERVICE_ID = "serviceId";
    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String EXTERNAL_DOCUMENTATION = "External documentation";
    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String API_ID = "org.zowe.apicatalog";
    private static final String API_VERSION = "v1.0.0";
    private static final String SEPARATOR = "/";
    private static final String URL_ENCODED_SPACE = "%20";

    private ApiDocV2Service apiDocV2Service;
    private GatewayConfigProperties gatewayConfigProperties;
    private GatewayClient gatewayClient;

    @BeforeEach
    void setUp() {
        gatewayConfigProperties = getProperties();
        gatewayClient = new GatewayClient(gatewayConfigProperties);
        apiDocV2Service = new ApiDocV2Service(gatewayClient);
        ReflectionTestUtils.setField(apiDocV2Service, "scheme", "https");
    }

    @Test
    void givenSwaggerJsonNotAsExpectedFormat_whenConvertToSwagger_thenThrowIOException() {
        String apiDocContent = "Failed content";

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, apiDocContent, null);

        Exception exception = assertThrows(UnexpectedTypeException.class, () -> apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo));
        assertEquals("Response is not a Swagger type object.", exception.getMessage());
    }

    @Nested
    class WhenApiDocTransform {
        @Nested
        class ThenCheckUpdatedValues {
            @Test
            void givenSwaggerValidJson() {
                Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
                String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
                RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

                RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);
                routedServices.addRoutedService(routedService3);

                ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

                String actualContent = apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                Swagger actualSwagger = convertJsonToSwagger(actualContent);

                assertNotNull(actualSwagger);

                String expectedDescription = dummySwaggerObject.getInfo().getDescription() +
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

                assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
                assertEquals(gatewayConfigProperties.getHostname(), actualSwagger.getHost());
                assertEquals(EXTERNAL_DOCUMENTATION, actualSwagger.getExternalDocs().getDescription());
                assertEquals(apiDocInfo.getApiInfo().getDocumentationUrl(), actualSwagger.getExternalDocs().getUrl());
                assertEquals("/" + SERVICE_ID + "/api/v1", actualSwagger.getBasePath());

                assertThat(actualSwagger.getSchemes(), hasItem(Scheme.forValue(gatewayConfigProperties.getScheme())));
                assertThat(actualSwagger.getPaths(), is(dummySwaggerObject.getPaths()));
            }

            @Test
            void givenSwaggerValidYaml() {
                Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
                String apiDocContent = convertSwaggerToYaml(dummySwaggerObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
                RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

                RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);
                routedServices.addRoutedService(routedService3);

                ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

                String actualContent = apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                Swagger actualSwagger = convertYamlToSwagger(actualContent);

                assertNotNull(actualSwagger);

                String expectedDescription = dummySwaggerObject.getInfo().getDescription() +
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

                assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
                assertEquals(gatewayConfigProperties.getHostname(), actualSwagger.getHost());
                assertEquals(EXTERNAL_DOCUMENTATION, actualSwagger.getExternalDocs().getDescription());
                assertEquals(apiDocInfo.getApiInfo().getDocumentationUrl(), actualSwagger.getExternalDocs().getUrl());
                assertEquals("/" + SERVICE_ID + "/api/v1", actualSwagger.getBasePath());

                assertThat(actualSwagger.getSchemes(), hasItem(Scheme.forValue(gatewayConfigProperties.getScheme())));
                assertThat(actualSwagger.getPaths(), is(dummySwaggerObject.getPaths()));
            }

            @Test
            void givenApiInfoNullAndBasePathAsNotRoot() {
                Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
                String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

                RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);

                ApiDocInfo apiDocInfo = new ApiDocInfo(null, apiDocContent, routedServices);

                String actualContent = apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                Swagger actualSwagger = convertJsonToSwagger(actualContent);
                assertNotNull(actualSwagger);

                assertEquals("/" + SERVICE_ID + "/api/v1", actualSwagger.getBasePath());
                assertThat(actualSwagger.getPaths(), is(dummySwaggerObject.getPaths()));
            }

            @Test
            void givenApiInfoNullAndBasePathAsRoot() {
                Swagger dummySwaggerObject = getDummySwaggerObject("/", false);
                String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

                RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);

                ApiDocInfo apiDocInfo = new ApiDocInfo(null, apiDocContent, routedServices);

                String actualContent = apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                Swagger actualSwagger = convertJsonToSwagger(actualContent);
                assertNotNull(actualSwagger);

                assertEquals("", actualSwagger.getBasePath());
                assertThat(actualSwagger.getPaths(), is(dummySwaggerObject.getPaths()));
            }

            @Test
            void givenMultipleRoutedService() {
                Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
                String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog/api1");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");
                RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog/api2");

                final RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);
                routedServices.addRoutedService(routedService3);

                ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

                String actualContent = apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                Swagger actualSwagger = convertJsonToSwagger(actualContent);

                assertNotNull(actualSwagger);

                String expectedDescription = dummySwaggerObject.getInfo().getDescription() +
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

                assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
                assertEquals(gatewayConfigProperties.getHostname(), actualSwagger.getHost());
                assertEquals(EXTERNAL_DOCUMENTATION, actualSwagger.getExternalDocs().getDescription());
                assertEquals(apiDocInfo.getApiInfo().getDocumentationUrl(), actualSwagger.getExternalDocs().getUrl());
                assertEquals("", actualSwagger.getBasePath());

                assertThat(actualSwagger.getSchemes(), hasItem(Scheme.forValue(gatewayConfigProperties.getScheme())));
                assertThat(actualSwagger.getPaths(), IsMapContaining.hasKey("/" + SERVICE_ID + "/" + routedService.getGatewayUrl()));
                assertThat(actualSwagger.getPaths(), IsMapContaining.hasKey("/" + SERVICE_ID + "/" + routedService3.getGatewayUrl()));
            }

            @Test
            void givenServiceUrlAsRoot() {
                Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
                String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

                RoutedService routedService = new RoutedService("api_v1", "api/v1", "/");
                RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

                RoutedServices routedServices = new RoutedServices();
                routedServices.addRoutedService(routedService);
                routedServices.addRoutedService(routedService2);

                ApiInfo apiInfo = new ApiInfo(API_VERSION, "api/v1", API_ID, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
                ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

                String actualContent = apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo);
                Swagger actualSwagger = convertJsonToSwagger(actualContent);
                assertNotNull(actualSwagger);

                assertEquals("/" + SERVICE_ID + "/api/v1", actualSwagger.getBasePath());

                dummySwaggerObject.getPaths().forEach((k, v) ->
                    assertThat(actualSwagger.getPaths(), IsMapContaining.hasKey(dummySwaggerObject.getBasePath() + k))
                );
            }
        }

        @Test
        void givenApimlHiddenTag_thenShouldBeSameDescriptionAndPaths() {
            Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", true);
            String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

            RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
            RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

            RoutedServices routedServices = new RoutedServices();
            routedServices.addRoutedService(routedService);
            routedServices.addRoutedService(routedService2);

            ApiInfo apiInfo = new ApiInfo(API_ID, "api/v1", API_VERSION, "https://localhost:10014/apicatalog/api-doc", null);
            ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

            String actualContent = apiDocV2Service.transformApiDoc(SERVICE_ID, apiDocInfo);
            Swagger actualSwagger = convertJsonToSwagger(actualContent);
            assertNotNull(actualSwagger);

            assertNotNull(actualSwagger);
            assertEquals(dummySwaggerObject.getInfo().getDescription(), actualSwagger.getInfo().getDescription());

            assertThat(actualSwagger.getPaths(), is(dummySwaggerObject.getPaths()));
        }

        private void verifySwagger2(Swagger swagger) {
            assertEquals("APIML test API", swagger.getInfo().getTitle());
            assertEquals("Example of GET endpoint", swagger.getPaths().get("/api/v1/").getGet().getSummary());
            assertEquals("exampleResponse200",  swagger.getDefinitions().entrySet().iterator().next().getKey());
        }

        @Test
        void givenInputFile_thenParseItCorrectly() throws IOException {
            GatewayConfigProperties gatewayConfigProperties = GatewayConfigProperties.builder().scheme("https").hostname("localhost").build();
            GatewayClient gatewayClient = new GatewayClient(gatewayConfigProperties);

            AtomicReference<Swagger> swaggerHolder = new AtomicReference<>();
            ApiDocV2Service apiDocV2Service = new ApiDocV2Service(gatewayClient) {
                @Override
                protected void updateExternalDoc(Swagger swagger, ApiDocInfo apiDocInfo) {
                    super.updateExternalDoc(swagger, apiDocInfo);
                    swaggerHolder.set(swagger);
                }
            };
            String transformed = apiDocV2Service.transformApiDoc("serviceId", new ApiDocInfo(
                mock(ApiInfo.class),
                IOUtils.toString(new ClassPathResource("swagger/swagger2.json").getInputStream(), StandardCharsets.UTF_8),
                mock(RoutedServices.class)
            ));
            assertNotNull(transformed);
            verifySwagger2(swaggerHolder.get());
        }

    }

    private String convertSwaggerToJson(Swagger swagger) {
        ObjectMapper objectMapper = new ObjectMapper();
        return writeOpenApiAsString(swagger, objectMapper);
    }

    private String convertSwaggerToYaml(Swagger swagger) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return writeOpenApiAsString(swagger, objectMapper);
    }

    private String writeOpenApiAsString(Swagger swagger, ObjectMapper objectMapper) {
        try {
            return objectMapper.writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private Swagger convertJsonToSwagger(String content) {
        ObjectMapper objectMapper = new ObjectMapper();
        return readStringToOpenApi(content, objectMapper);
    }

    private Swagger convertYamlToSwagger(String content) {
        ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory());
        return readStringToOpenApi(content, objectMapper);
    }

    private Swagger readStringToOpenApi(String content, ObjectMapper objectMapper) {
        Swagger swagger = null;
        try {
            swagger = objectMapper.readValue(content, Swagger.class);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return swagger;
    }

    private Swagger getDummySwaggerObject(String batePath, boolean apimlHidden) {
        Swagger swagger = new Swagger();
        swagger.setPaths(new HashMap<>());
        swagger.setTags(new ArrayList<>());
        swagger.setSwagger("2.0");
        swagger.setBasePath(batePath);
        //
        io.swagger.models.Info info = new Info();
        info.setTitle("API Catalog");
        info.setDescription("REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.");
        info.setVersion("1.0.0");
        swagger.setInfo(info);
        //
        io.swagger.models.Tag tag = new io.swagger.models.Tag();
        tag.setName("API Catalog");
        tag.setDescription("Current state information");
        swagger.getTags().add(tag);

        if (apimlHidden) {
            tag = new Tag();
            tag.setName(HIDDEN_TAG);
            swagger.getTags().add(tag);
        }

        swagger.getPaths().put("/api1", new Path());
        swagger.getPaths().put("/api2", new Path());

        return swagger;
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }
}
