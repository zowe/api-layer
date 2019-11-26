/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.apicatalog.swagger.api;

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.swagger.TransformApiDocService;
import com.ca.mfaas.config.ApiInfo;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.gateway.GatewayClient;
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.ca.mfaas.product.routing.RoutedService;
import com.ca.mfaas.product.routing.RoutedServices;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.oas.models.tags.Tag;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.*;

public class ApiDocV3ServiceTest {

    private static final String HIDDEN_TAG = "apimlHidden";
    private static final String SERVICE_ID = "serviceId";
    private static final String SWAGGER_LOCATION_LINK = "[Swagger/OpenAPI JSON Document]";
    private static final String EXTERNAL_DOCUMENTATION = "External documentation";
    private static final String CATALOG_VERSION = "/api/v1";
    private static final String CATALOG_APIDOC_ENDPOINT = "/apidoc";
    private static final String HARDCODED_VERSION = "/v1";
    private static final String SEPARATOR = "/";

    private TransformApiDocService transformApiDocService;
    private GatewayClient gatewayClient;
    private ApiDocV3Service apiDocV3Service;

    @Before
    public void setUp() {
        GatewayConfigProperties gatewayConfigProperties = getProperties();
        gatewayClient = new GatewayClient(gatewayConfigProperties);
        transformApiDocService = new TransformApiDocService(gatewayClient);
        apiDocV3Service = new ApiDocV3Service(gatewayClient);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void givenOpenApiValidJson_whenApiDocTransform_thenCheckUpdatedValues() {
        List<Server> servers = new ArrayList<>();
        servers.add(0, new Server().url("/apicatalog"));
        OpenAPI dummyOpenApiObject = getDummyOpenApiObject(servers, false);
        String apiDocContent = convertOpenApiToJson(dummyOpenApiObject);

        RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog");
        RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/apicatalog");

        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);
        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", "3.0.0", "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

        String actualContent = transformApiDocService.transformApiDoc(SERVICE_ID, apiDocInfo);
        OpenAPI actualSwagger = convertJsonToOpenApi(actualContent);
        assertNotNull(actualSwagger);
        String expectedDescription = dummyOpenApiObject.getInfo().getDescription() +
            "\n\n" +
            SWAGGER_LOCATION_LINK +
            "(" +
            gatewayClient.getGatewayConfigProperties().getScheme() +
            "://" +
            gatewayClient.getGatewayConfigProperties().getHostname() +
            CATALOG_VERSION +
            SEPARATOR +
            CoreService.API_CATALOG.getServiceId() +
            CATALOG_APIDOC_ENDPOINT +
            SEPARATOR +
            SERVICE_ID +
            HARDCODED_VERSION +
            ")";

        assertEquals("https://localhost:10010/api/v1/serviceId", actualSwagger.getServers().get(0).getUrl());
        assertThat(actualSwagger.getPaths(), is(dummyOpenApiObject.getPaths()));

        assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
        assertEquals(EXTERNAL_DOCUMENTATION, actualSwagger.getExternalDocs().getDescription());
        assertEquals(apiDocInfo.getApiInfo().getDocumentationUrl(), actualSwagger.getExternalDocs().getUrl());

    }

    @Test
    public void givenInvalidJson_whenApiDocTransform_thenThrowExeption() throws IOException {
        String invalidJson = "";
        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", "3.0.0", "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, invalidJson, null);

        exceptionRule.expect(MismatchedInputException.class);
        exceptionRule.expectMessage("No content to map due to end-of-input\n" +
            " at [Source: (String)\"\"; line: 1, column: 0]");
        apiDocV3Service.transformApiDoc(SERVICE_ID, apiDocInfo);
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

    private OpenAPI convertJsonToOpenApi(String content) {
        ObjectMapper objectMapper = new ObjectMapper();
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

        Tag tag = new Tag();
        tag.setName("API Catalog");
        tag.setDescription("Current state information");
        openAPI.getTags().add(tag);
        if (apimlHidden) {
            tag = new Tag();
            tag.setName(HIDDEN_TAG);
            openAPI.getTags().add(tag);
        }

        openAPI.getPaths().put("/api1", new PathItem());;
        openAPI.getPaths().put("/api2", new PathItem());;
        return openAPI;
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }
}
