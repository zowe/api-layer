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
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.swagger.api.dummy.DummyApiDocService;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasKey;
import static org.junit.jupiter.api.Assertions.*;


class AbstractApiDocServiceTest {

    private static final String GATEWAY_SCHEME = "http";
    private static final String GATEWAY_HOST = "gateway:10000";

    private AbstractApiDocService abstractApiDocService;

    @BeforeEach
    void setUp() {
        GatewayClient gatewayClient = new GatewayClient(getProperties());
        abstractApiDocService = new DummyApiDocService(gatewayClient);
    }

    @Test
    void givenRoutedUrlAndEndpoint_whenGetShortEndpoint_thenReturnEndpoint() {
        String shortEndpoint = abstractApiDocService.getShortEndPoint("/apicatalog/api/v1", "/apicatalog");
        assertEquals("/apicatalog", shortEndpoint);
    }

    @Nested
    class WhenGetEndpoint {
        @Test
        void givenPathAndEndpoint_thenReturnThem() {
            String endpoint = abstractApiDocService.getEndPoint("/api/v1/api-doc", "/apicatalog");
            assertEquals("/api/v1/api-doc/apicatalog", endpoint);
        }

        @Test
        void givenBasePathAndEndpointThatWillResultInMalformedUrl_thenReturnNormalizedEndpoint() {
            String endpoint = abstractApiDocService.getEndPoint("/api/v1/api-doc/", "/apicatalog");
            assertEquals("/api/v1/api-doc/apicatalog", endpoint);
        }

        @Test
        void givenSwaggerTemplateCharacter_thenReturnTemplateNotNormalized() {
            String endpoint = abstractApiDocService.getEndPoint("/api/v1/api-doc/", "/containers/{id}");
            assertEquals("/api/v1/api-doc/containers/{id}", endpoint);
        }
    }

    @Test
    void givenRoutedEndpoint_whenGetEndpointPairs_thenReturnRoutedPair() {
        RoutedService routedService = new RoutedService("api_v1", "api/v1", "/apicatalog/api/v1");
        Pair endpointPairs = abstractApiDocService.getEndPointPairs("/apicatalog", "apicatalog", routedService);
        ImmutablePair expectedPairs = new ImmutablePair("/apicatalog", "/apicatalog/api/v1/apicatalog");
        assertEquals(expectedPairs, endpointPairs);
    }

    @Test
    void givenNullApiInfo_whenGetRoutedServiceByApiInfo_thenReturnNull() {
        ApiDocInfo apiDocInfo = new ApiDocInfo(null, null, null);
        assertNull(abstractApiDocService.getRoutedServiceByApiInfo(apiDocInfo, "/"));
    }

    @Test
    void givenSwaggerDoc_whenPreparePaths_thenSetpathsInSwaggerDoc() {
        List<Server> servers = new ArrayList<>();
        servers.add(0, new Server().url("/apicatalog"));
        ApiDocPath<PathItem> apiDocPath = new ApiDocPath<>();
        OpenAPI openAPI = getDummyOpenApiObject(servers);

        String apiDocContent = convertOpenApiToJson(openAPI);
        RoutedService routedService = new RoutedService("api_v1", "api/v1", "/api/v1");
        RoutedService routedService2 = new RoutedService("ui_v1", "ui/v1", "/ui/v1");

        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(routedService);
        routedServices.addRoutedService(routedService2);

        ApiInfo apiInfo = new ApiInfo("org.zowe.apicatalog", "api/v1", null, "https://localhost:10014/apicatalog/api-doc", null, "https://www.zowe.org");
        ApiDocInfo apiDocInfo = new ApiDocInfo(apiInfo, apiDocContent, routedServices);

        abstractApiDocService.preparePath(openAPI.getPaths(), apiDocPath, apiDocInfo, "/api/v1/api-doc", "/", "apicatalog");

        assertThat(apiDocPath.getLongPaths(), hasKey("/apicatalog/api/v1/api-doc/"));
        assertThat(apiDocPath.getShortPaths(), hasKey("/api-doc/"));
        assertTrue(apiDocPath.getPrefixes().contains("api/v1"));

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

    private OpenAPI getDummyOpenApiObject(List<Server> servers) {
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

        openAPI.getPaths().put("/api1", new PathItem());
        openAPI.getPaths().put("/api2", new PathItem());
        return openAPI;
    }

    private ServiceAddress getProperties() {
        return ServiceAddress.builder()
            .scheme(GATEWAY_SCHEME)
            .hostname(GATEWAY_HOST)
            .build();
    }

}
