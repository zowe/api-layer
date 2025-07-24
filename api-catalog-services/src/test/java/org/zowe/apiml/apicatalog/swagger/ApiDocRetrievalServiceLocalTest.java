/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.netflix.appinfo.InstanceInfo;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.servers.Server;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springdoc.webflux.api.OpenApiWebfluxResource;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.config.ApiInfo;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ApiDocRetrievalServiceLocalTest {

    private ApiDocRetrievalServiceLocal service;

    @BeforeEach
    void init() {
        service = new ApiDocRetrievalServiceLocal(Collections.emptyList(), null, null, null, null, null, null, null);
    }

    @Test
    void givenUnknownServiceId_whenGetApiDoc_thenThrowException() {
        var instance = new EurekaServiceInstance(InstanceInfo.Builder.newBuilder().setAppName("unknownService").build());
        var apiInfo = ApiInfo.builder().build();
        var exception = assertThrows(ApiDocNotFoundException.class, () -> service.retrieveApiDoc(instance, apiInfo));

        assertEquals("Cannot obtain API doc for service unknownservice", exception.getMessage());
    }

    private OpenApiWebfluxResource mockApiDocResource() {
        var apiDocResource = mock(OpenApiWebfluxResource.class);
        ((Map<String, OpenApiWebfluxResource>) ReflectionTestUtils.getField(service, "apiDocResource")).put("service", apiDocResource);
        return apiDocResource;
    }

    @Test
    void givenInvalidApiDoc_whenGetApiDoc_thenThrowException() throws JsonProcessingException {
        var apiDocResource = mockApiDocResource();
        doThrow(new JsonProcessingException("an error") {}).when(apiDocResource).openapiJson(any(), eq("/"), any());

        var instance = new EurekaServiceInstance(InstanceInfo.Builder.newBuilder().setAppName("service").build());
        var apiInfo = ApiInfo.builder().build();

        var exception = assertThrows(ApiDocNotFoundException.class, () -> service.retrieveApiDoc(instance, apiInfo));
        assertEquals("Cannot obtain API doc for service", exception.getMessage());
        assertInstanceOf(JsonProcessingException.class, exception.getCause());
        assertEquals("an error", exception.getCause().getMessage());
    }

    @Test
    void givenValidApiDoc_whenGetApiDoc_thenReturnApiDoc() throws JsonProcessingException {
        var apiDocResource = mockApiDocResource();
        doReturn(Mono.just("Api doc".getBytes(StandardCharsets.UTF_8))).when(apiDocResource).openapiJson(any(), eq("/"), any());

        var instance = new EurekaServiceInstance(InstanceInfo.Builder.newBuilder().setAppName("service").build());
        var apiInfo = ApiInfo.builder().build();

        StepVerifier.create(service.retrieveApiDoc(instance, apiInfo))
            .expectNextMatches(apiDocInfo -> {
                assertEquals("Api doc", apiDocInfo.getApiDocContent());
                assertSame(apiInfo, apiDocInfo.getApiInfo());
                return true;
            })
            .verifyComplete();
    }

    @Nested
    class NormalizePathsCustomizer {

        @ParameterizedTest
        @CsvSource({
            "/api1/a,/api1/b/c,/api1",
            "/a/start*,/a/start,/a",
            "/a/start/*,/a/start,/a/start",
            "/a/b/c,/a/b/c,/a/b/c",
            "/a/b/c,/a/b/c/,/a/b/c",
            "/a/b/c/,/a/b/c/,/a/b/c",
            "/a/b*/c/**,/a/b*/c**,/a"
        })
        void givenTwoPatterns_whenGetCommonBasePath_thenGetBasePath(String pattern1, String pattern2, String basePath) {
            assertEquals(basePath, service.getCommonBasePath(Arrays.asList(pattern1, pattern2)));
        }

        @Test
        void givenOpenApiAndPath_whenNormalizePaths_thenModifyOpenApi() {
            OpenAPI openApi = new OpenAPI();

            var server = new Server();
            server.setUrl("https://localhost:10014");
            openApi.setServers(Collections.singletonList(server));

            var paths = new Paths();
            paths.addPathItem("/apicatalog/api/v1/endpoint", new PathItem());
            openApi.setPaths(paths);

            service.normalizePathsCustomizer(Collections.singletonList("/apicatalog/api/v1/"))
                .customise(openApi);

            assertEquals(1, openApi.getServers().size());
            assertEquals("https://localhost:10014/apicatalog/api/v1", server.getUrl());

            assertEquals(1, openApi.getPaths().size());
            assertNotNull(openApi.getPaths().get("/endpoint"));
        }

    }

}
