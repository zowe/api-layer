/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.apicatalog.swagger;


import com.ca.mfaas.apicatalog.gateway.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.model.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.models.*;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.validation.UnexpectedTypeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.*;


@RunWith(MockitoJUnitRunner.Silent.class)
public class TransformApiDocServiceTest {

    //todo: should add test case as new unit method prefixs size is different than 1

    private static final String SERVICE_ID = "serviceId";
    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String EXTERNAL_DOCUMENTATION = "External documentation";
    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String HARDCODED_VERSION = "/v1";
    private static final String SEPARATOR = "/";

    private TransformApiDocService transformApiDocService;
    private GatewayConfigProperties gatewayConfigProperties;

    @Before
    public void setUp() {
        gatewayConfigProperties = getProperties();
        transformApiDocService = new TransformApiDocService(gatewayConfigProperties);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void givenSwaggerJsonNotAsExpectedFormat_whenConvertToSwagger_thenThrowException() {
        String apiDocContent = "Failed content";

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, apiDocContent, null);

        exceptionRule.expect(UnexpectedTypeException.class);
        exceptionRule.expectMessage("Response is not a Swagger type object.");

        transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
    }

    @Test
    public void givenSwaggerValidJson_whenApiDocTransform_thenCheckUpdatedValues() {
        Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
        String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

        final RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
        final RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);

        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", null, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

        String actualContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
        Swagger actualSwagger = convertJsonToSwagger(actualContent);

        assertNotNull(actualSwagger);

        String expectedDescription = new StringBuilder()
            .append(dummySwaggerObject.getInfo().getDescription())
            .append("\n\n")
            .append(SWAGGER_LOCATION_LINK)
            .append("(")
            .append(gatewayConfigProperties.getScheme())
            .append("://")
            .append(gatewayConfigProperties.getHostname())
            .append(CATALOG_VERSION)
            .append(SEPARATOR)
            .append(CoreService.API_CATALOG.getServiceId())
            .append(CATALOG_APIDOC_ENDPOINT)
            .append(SEPARATOR)
            .append(SERVICE_ID)
            .append(HARDCODED_VERSION)
            .append(")").toString();

        assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
        assertEquals(actualSwagger.getHost(), gatewayConfigProperties.getHostname());
        assertEquals(actualSwagger.getExternalDocs().getDescription(), EXTERNAL_DOCUMENTATION);
        assertEquals(actualSwagger.getExternalDocs().getUrl(), apiDocInfo.getApiInfo().getDocumentationUrl());

        assertThat(actualSwagger.getSchemes(), hasItem(Scheme.forValue(gatewayConfigProperties.getScheme())));
        assertEquals(actualSwagger.getBasePath(), "/api/v1/" + SERVICE_ID);

        actualSwagger.getPaths().forEach((k, v) -> assertTrue(dummySwaggerObject.getPaths().containsKey(k)));
    }

    @Test
    public void givenApiInfoNullAndBasePathAsNotRoot_whenApiDocTransform_thenCheckUpdatedValues() {
        Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
        String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

        final RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
        final RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, apiDocContent, routedServices);

        String actualContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
        Swagger actualSwagger = convertJsonToSwagger(actualContent);

        assertEquals(actualSwagger.getBasePath(), "/api/v1/" + SERVICE_ID);
        actualSwagger.getPaths().forEach((k, v) -> assertTrue(dummySwaggerObject.getPaths().containsKey(k)));
    }

    @Test
    public void givenApiInfoNullAndBasePathAsRoot_whenApiDocTransform_thenCheckUpdatedValues() {
        Swagger dummySwaggerObject = getDummySwaggerObject("/", false);
        String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

        final RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
        final RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, apiDocContent, routedServices);

        String actualContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
        Swagger actualSwagger = convertJsonToSwagger(actualContent);

        assertEquals(actualSwagger.getBasePath(), "");
        assertTrue(actualSwagger.getPaths().isEmpty());
    }

    @Test
    public void givenApimlHiddenTag_whenApiDocTransform_thenShouldBeSameDescriptionAndPaths() {
        Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", true);
        String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

        final RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
        final RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);

        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", null, "https://localhost:10014/apicatalog/api-doc", null);
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

        String actualContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
        Swagger actualSwagger = convertJsonToSwagger(actualContent);

        assertNotNull(actualSwagger);
        assertEquals(dummySwaggerObject.getInfo().getDescription(), actualSwagger.getInfo().getDescription());

        actualSwagger.getPaths().forEach((k, v) -> assertTrue(dummySwaggerObject.getPaths().containsKey(k)));
    }

    @Test
    @Ignore
    public void givenServiceUrlAsRoot_whenApiDocTransform_thenCheckUpdatedValues() {
        String swagger = "{\"swagger\":\"2.0\",\"info\":{\"description\":\"REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.\",\"version\":\"1.0.0\",\"title\":\"API Catalog\"},\"basePath\": \"/\",\"tags\":[{\"name\":\"API Catalog\",\"description\":\"Current state information\"},{\"name\":\"API Documentation\",\"description\":\"Service documentation\"}],\"paths\":{\"/apidoc/{service-id}/{api-version}\":{\"get\":{\"tags\":[\"API Documentation\"],\"summary\":\"Retrieves the API documentation for a specific service version\",\"description\":\"Returns the API documentation for a specific service {service-id} and version {api-version}\",\"operationId\":\"getApiDocInfoUsingGET\",\"produces\":[\"application/json;charset=UTF-8\"],\"parameters\":[{\"name\":\"service-id\",\"in\":\"path\",\"description\":\"The unique identifier of the registered service\",\"required\":true,\"type\":\"string\"},{\"name\":\"api-version\",\"in\":\"path\",\"description\":\"The major version of the API documentation (v1, v2, etc.)\",\"required\":true,\"type\":\"string\"}],\"responses\":{\"200\":{\"description\":\"OK\",\"schema\":{\"type\":\"string\"},\"responseSchema\":{\"type\":\"string\"}},\"401\":{\"description\":\"Unauthorized\"},\"403\":{\"description\":\"Forbidden\"},\"404\":{\"description\":\"URI not found\"},\"500\":{\"description\":\"An unexpected condition occurred\"}},\"deprecated\":false}},\"/containers\":{\"get\":{\"tags\":[\"API Catalog\"],\"summary\":\"Lists catalog dashboard tiles\",\"description\":\"Returns a list of tiles including status and tile description\",\"operationId\":\"getAllAPIContainersUsingGET\",\"produces\":[\"application/json;charset=UTF-8\"],\"responses\":{\"200\":{\"description\":\"OK\",\"schema\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/definitions/APIContainer\",\"originalRef\":\"APIContainer\"}},\"responseSchema\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/definitions/APIContainer\",\"originalRef\":\"APIContainer\"}}},\"401\":{\"description\":\"Unauthorized\"},\"403\":{\"description\":\"Forbidden\"},\"404\":{\"description\":\"Not Found\"}},\"deprecated\":false}},\"/containers/{id}\":{\"get\":{\"tags\":[\"API Catalog\"],\"summary\":\"Retrieves a specific dashboard tile information\",\"description\":\"Returns information for a specific tile {id} including status and tile description\",\"operationId\":\"getAPIContainerByIdUsingGET\",\"produces\":[\"application/json;charset=UTF-8\"],\"parameters\":[{\"name\":\"id\",\"in\":\"path\",\"description\":\"id\",\"required\":true,\"type\":\"string\"}],\"responses\":{\"200\":{\"description\":\"OK\",\"schema\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/definitions/APIContainer\",\"originalRef\":\"APIContainer\"}},\"responseSchema\":{\"type\":\"array\",\"items\":{\"$ref\":\"#/definitions/APIContainer\",\"originalRef\":\"APIContainer\"}}},\"401\":{\"description\":\"Unauthorized\"},\"403\":{\"description\":\"Forbidden\"},\"404\":{\"description\":\"Not Found\"}},\"deprecated\":false}}},\"definitions\":{\"APIContainer\":{\"type\":\"object\",\"properties\":{\"activeServices\":{\"type\":\"integer\",\"format\":\"int32\"},\"createdTimestamp\":{\"$ref\":\"#/definitions/Calendar\",\"originalRef\":\"Calendar\"},\"description\":{\"type\":\"string\",\"description\":\"The description of the API\",\"allowEmptyValue\":false},\"id\":{\"type\":\"string\",\"description\":\"The API Container Id\",\"allowEmptyValue\":false},\"lastUpdatedTimestamp\":{\"$ref\":\"#/definitions/Calendar\",\"originalRef\":\"Calendar\"},\"services\":{\"type\":\"array\",\"description\":\"A collection of services which are registered with this API\",\"allowEmptyValue\":false,\"items\":{\"$ref\":\"#/definitions/APIService\",\"originalRef\":\"APIService\"}},\"status\":{\"type\":\"string\",\"description\":\"The Status of the container\",\"allowEmptyValue\":false},\"title\":{\"type\":\"string\",\"description\":\"The API Container title\",\"allowEmptyValue\":false},\"totalServices\":{\"type\":\"integer\",\"format\":\"int32\"},\"version\":{\"type\":\"string\",\"description\":\"The version of the API container\",\"allowEmptyValue\":false}},\"title\":\"APIContainer\"},\"APIService\":{\"type\":\"object\",\"properties\":{\"apiDoc\":{\"type\":\"string\",\"description\":\"The API documentation for this service\",\"allowEmptyValue\":false},\"description\":{\"type\":\"string\",\"description\":\"The description of the API service\",\"allowEmptyValue\":false},\"homePageUrl\":{\"type\":\"string\",\"description\":\"The service home page of the API service\",\"allowEmptyValue\":false},\"secured\":{\"type\":\"boolean\",\"description\":\"The security status of the API service\",\"allowEmptyValue\":false},\"serviceId\":{\"type\":\"string\",\"description\":\"The service id\",\"allowEmptyValue\":false},\"status\":{\"type\":\"string\",\"description\":\"The status of the API service\",\"allowEmptyValue\":false},\"title\":{\"type\":\"string\",\"description\":\"The API service name\",\"allowEmptyValue\":false}},\"title\":\"APIService\"},\"Calendar\":{\"type\":\"object\",\"properties\":{\"calendarType\":{\"type\":\"string\"},\"firstDayOfWeek\":{\"type\":\"integer\",\"format\":\"int32\"},\"lenient\":{\"type\":\"boolean\"},\"minimalDaysInFirstWeek\":{\"type\":\"integer\",\"format\":\"int32\"},\"time\":{\"type\":\"string\",\"format\":\"date-time\"},\"timeInMillis\":{\"type\":\"integer\",\"format\":\"int64\"},\"timeZone\":{\"$ref\":\"#/definitions/TimeZone\",\"originalRef\":\"TimeZone\"},\"weekDateSupported\":{\"type\":\"boolean\"},\"weekYear\":{\"type\":\"integer\",\"format\":\"int32\"},\"weeksInWeekYear\":{\"type\":\"integer\",\"format\":\"int32\"}},\"title\":\"Calendar\"},\"Mono«ResponseEntity«string»»\":{\"type\":\"object\",\"title\":\"Mono«ResponseEntity«string»»\"},\"TimeZone\":{\"type\":\"object\",\"properties\":{\"displayName\":{\"type\":\"string\"},\"dstsavings\":{\"type\":\"integer\",\"format\":\"int32\"},\"id\":{\"type\":\"string\"},\"rawOffset\":{\"type\":\"integer\",\"format\":\"int32\"}},\"title\":\"TimeZone\"}}}";

        final RoutedService routedService = new RoutedService("api_v1", "api/v1", "/");
        final RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);

        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", null, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, swagger, routedServices);

        String transformApiDoc = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);

        assertTrue(transformApiDoc.contains("schemes\":[\"https\"]"));
        assertTrue(transformApiDoc.contains("https://localhost:10010/api/v1/apicatalog/apidoc/Service/v1"));
        assertTrue(transformApiDoc.contains("host\":\"localhost:10010"));
        assertTrue(transformApiDoc.contains("basePath\":\"/api/v1/Service"));
        assertTrue(transformApiDoc.contains("externalDocs\":{\"description\":\"External documentation\",\"url\":\"https://www.zowe.org"));
        assertTrue(transformApiDoc.contains("\"paths\":{\"apidoc/{service-id}/{api-version}"));
    }

    private String convertSwaggerToJson(Swagger swagger) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(swagger);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }
    private Swagger convertJsonToSwagger(String content) {
        ObjectMapper objectMapper = new ObjectMapper();
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
        Info info = new Info();
        info.setTitle("API Catalog");
        info.setDescription("REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.");
        info.setVersion("1.0.0");
        swagger.setInfo(info);
        //
        Tag tag = new Tag();
        tag.setName("API Catalog");
        tag.setDescription("Current state information");
        swagger.getTags().add(tag);

        if(apimlHidden) {
            tag = new Tag();
            tag.setName(HIDDEN_TAG);
            swagger.getTags().add(tag);
        }

        swagger.getPaths().put("/apidoc/{service-id}/{api-version}", new Path());
        swagger.getPaths().put("/containers", new Path());

        return swagger;
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }
}
