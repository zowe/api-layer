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


import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.swagger.models.*;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.*;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import javax.validation.UnexpectedTypeException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TransformApiDocServiceTest {

    private static final String SERVICE_ID = "serviceId";
    private static final String HIDDEN_TAG = "apimlHidden";

    private TransformApiDocService transformApiDocService;
    private GatewayConfigProperties gatewayConfigProperties;
    private GatewayClient gatewayClient;

    @Before
    public void setUp() {
        gatewayConfigProperties = getProperties();
        gatewayClient = new GatewayClient(gatewayConfigProperties);
        transformApiDocService = new TransformApiDocService(gatewayClient);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void givenSwaggerJsonNotAsExpectedFormat_whenConvertToSwagger_thenThrowException() {
        String apiDocContent = "Failed content";

        ApiDocInfo apiDocInfo = new ApiDocInfo(null, apiDocContent, null);

        exceptionRule.expect(UnexpectedTypeException.class);
        exceptionRule.expectMessage("Response is not a Swagger or OpenAPI type object.");

        transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
    }

    @Test
    public void givenOpenApiContent_whenApiDocTransform_thenCallV3Service() {
        List<Server> servers = new ArrayList<>();
        servers.add(0, new Server().url("/apicatalog"));
        OpenAPI dummyOpenApiObject = getDummyOpenApiObject(servers, false);
        String apiDocContent = convertOpenApiToJson(dummyOpenApiObject);

        RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog/api1");
        RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");
        RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog/api2");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);
        routedServices.addRoutedService(routedService3);

        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", null, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

        String openApiContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
        assertNotNull(openApiContent);

        JsonObject jsonOpenApiContent = new JsonParser().parse(openApiContent).getAsJsonObject();
        assertEquals(jsonOpenApiContent.get("openapi").getAsString(), "3.0.0");

    }

    @Test
    public void givenSwaggerContent_whenApiDocTransform_thenCallV2Service() {
        Swagger dummySwaggerObject = getDummySwaggerObject("/apicatalog", false);
        String apiDocContent = convertSwaggerToJson(dummySwaggerObject);

        RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog/api1");
        RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");
        RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog/api2");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);
        routedServices.addRoutedService(routedService3);

        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", null, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

        String swaggerContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);

        assertNotNull(swaggerContent);

        JsonObject jsonOpenApiContent = new JsonParser().parse(swaggerContent).getAsJsonObject();
        assertEquals(jsonOpenApiContent.get("swagger").getAsString(), "2.0");
    }

    @Test
    public void givenWrongContent_whenApiDocTransform_thenReturnNull() {
        String invalidApiDocContent = "{\"notOpenApiNorSwagger\":\"3.0.0\",\"info\":{\"title\":\"API Catalog\",\"description\":\"REST API for the API Catalog\",\"termsOfService\":null,\"contact\":null,\"license\":null,\"version\":\"1.0.0\",\"extensions\":null},\"externalDocs\":null,\"servers\":[{\"url\":\"/apicatalog\",\"description\":null,\"variables\":null,\"extensions\":null}],\"security\":null,\"tags\":[{\"name\":\"API Catalog\",\"description\":\"Current state information\",\"externalDocs\":null,\"extensions\":null}],\"paths\":{\"/api1\":{\"summary\":null,\"description\":null,\"get\":null,\"put\":null,\"post\":null,\"delete\":null,\"options\":null,\"head\":null,\"patch\":null,\"trace\":null,\"servers\":null,\"parameters\":null,\"$ref\":null,\"extensions\":null},\"/api2\":{\"summary\":null,\"description\":null,\"get\":null,\"put\":null,\"post\":null,\"delete\":null,\"options\":null,\"head\":null,\"patch\":null,\"trace\":null,\"servers\":null,\"parameters\":null,\"$ref\":null,\"extensions\":null}},\"components\":null,\"extensions\":null}";

        RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog/api1");
        RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");
        RoutedService routedService3 = new RoutedService("api_v2", "api/v2", "/apicatalog/api2");

        final RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);
        routedServices.addRoutedService(routedService3);

        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", null, "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, invalidApiDocContent, routedServices);

        String openApiContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);

        assertNull(openApiContent);
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

        if (apimlHidden) {
            tag = new Tag();
            tag.setName(HIDDEN_TAG);
            swagger.getTags().add(tag);
        }

        swagger.getPaths().put("/api1", new Path());
        swagger.getPaths().put("/api2", new Path());

        return swagger;
    }

    private String convertOpenApiToJson(OpenAPI openApi) {
        ObjectMapper objectMapper = new ObjectMapper();
        try {
            return objectMapper.writeValueAsString(openApi);
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            return null;
        }
    }

    private OpenAPI getDummyOpenApiObject(List<Server> servers, boolean apimlHidden) {
        OpenAPI openAPI = new OpenAPI();
        openAPI.setPaths(new Paths());
        openAPI.setTags(new ArrayList<>());
        openAPI.setOpenapi("3.0.0");
        openAPI.setServers(servers);

        io.swagger.v3.oas.models.info.Info info = new io.swagger.v3.oas.models.info.Info();
        info.setTitle("API Catalog");
        info.setDescription("REST API for the API Catalog service which is a component of the API Mediation Layer. Use this API to retrieve information regarding catalog dashboard tiles, tile contents and its status, API documentation and status for the registered services.");
        info.setVersion("1.0.0");
        openAPI.setInfo(info);

        io.swagger.v3.oas.models.tags.Tag tag = new io.swagger.v3.oas.models.tags.Tag();
        tag.setName("API Catalog");
        tag.setDescription("Current state information");
        openAPI.getTags().add(tag);
        if (apimlHidden) {
            tag = new io.swagger.v3.oas.models.tags.Tag();
            tag.setName(HIDDEN_TAG);
            openAPI.getTags().add(tag);
        }

        openAPI.getPaths().put("/api1", new PathItem());
        openAPI.getPaths().put("/api2", new PathItem());
        return openAPI;
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }
}
