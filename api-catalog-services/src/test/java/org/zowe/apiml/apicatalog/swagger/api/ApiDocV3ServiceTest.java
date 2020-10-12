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
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.gateway.GatewayConfigProperties;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import javax.validation.UnexpectedTypeException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.*;
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

    private GatewayClient gatewayClient;
    private ApiDocV3Service apiDocV3Service;

    @Before
    public void setUp() {
        GatewayConfigProperties gatewayConfigProperties = getProperties();
        gatewayClient = new GatewayClient(gatewayConfigProperties);
        apiDocV3Service = new ApiDocV3Service(gatewayClient);
    }

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void givenOpenApiValidJson_whenApiDocTransform_thenCheckUpdatedValues() {
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
        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", "3.0.0", "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
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
            HARDCODED_VERSION +
            ")";

        assertEquals("https://localhost:10010/serviceId/api/v1", actualSwagger.getServers().get(0).getUrl());
        assertThat(actualSwagger.getPaths(), is(dummyOpenApiObject.getPaths()));

        assertEquals(expectedDescription, actualSwagger.getInfo().getDescription());
        assertEquals(EXTERNAL_DOCUMENTATION, actualSwagger.getExternalDocs().getDescription());
        assertEquals(apiDocInfo.getApiInfo().getDocumentationUrl(), actualSwagger.getExternalDocs().getUrl());

    }

    @Test
    public void givenEmptyJson_whenApiDocTransform_thenThrowException() {
        String invalidJson = "";
        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", "3.0.0", "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, invalidJson, null);

        exceptionRule.expect(UnexpectedTypeException.class);
        exceptionRule.expectMessage("No swagger supplied");
        apiDocV3Service.transformApiDoc(SERVICE_ID, apiDocInfo);
    }

    @Test
    public void givenInvalidJson_whenApiDocTransform_thenThrowException() {
        String invalidJson = "nonsense";
        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", "3.0.0", "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, invalidJson, null);

        exceptionRule.expect(UnexpectedTypeException.class);
        exceptionRule.expectMessage("attribute openapi is not of type `object`");
        apiDocV3Service.transformApiDoc(SERVICE_ID, apiDocInfo);
    }

    /**
     * GH #637
     */
    @Test
    public void givenValidApidoc_whenTransformed_thenDontCapitalizeEnums() {
        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", "3.0.0", "https://localhost:10014/apicatalog/api-doc", "https://www.zowe.org");
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
        openAPI.getPaths().put("/api1", new PathItem().$ref("test"));
        openAPI.getPaths().put("/api2", new PathItem().$ref("test"));
        return openAPI;
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }
}
