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

import com.netflix.appinfo.InstanceInfo;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.exceptions.ApiVersionNotFoundException;
import org.zowe.apiml.apicatalog.model.ApiDocInfo;
import org.zowe.apiml.config.ApiInfo;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.util.HttpClientMockHelper;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ApiDocServiceTest {

    private static final String SERVICE_ID = "service";
    private static final String SERVICE_HOST = "service";
    private static final int SERVICE_PORT = 8080;
    private static final String SERVICE_VERSION = "1.0.0";
    private static final String HIGHER_SERVICE_VERSION = "2.0.0";
    private static final String SERVICE_VERSION_V = "test.app v1.0.0";
    private static final String HIGHER_SERVICE_VERSION_V = "test.app v2.0.0";
    private static final String GATEWAY_SCHEME = "http";
    private static final String GATEWAY_HOST = "gateway:10000";
    private static final String GATEWAY_URL = "api/v1";
    private static final String API_ID = "test.app";
    private static final String SWAGGER_URL = "https://service:8080/service/api-doc";

    private static final ServiceAddress GW_SERVICE_ADDRESS = ServiceAddress.builder().scheme(GATEWAY_SCHEME).hostname(GATEWAY_HOST).build();

    @Nested
    class ViaRestCall {

        @Mock
        private DiscoveryClient discoveryClient;

        private ApiDocService apiDocService;

        @Mock
        private ApimlLogger apimlLogger;

        @Mock
        private CloseableHttpClient httpClient;

        @Mock
        private CloseableHttpResponse response;

        private AtomicReference<ApiInfo> lastApiInfo = new AtomicReference<>();

        @BeforeEach
        void setup() {
            lastApiInfo.set(null);

            HttpClientMockHelper.mockExecuteWithResponse(httpClient, response);
            var apiDocRetrievalServiceRest = new ApiDocRetrievalServiceRest(httpClient);
            apiDocService = new ApiDocService(
                discoveryClient,
                new GatewayClient(GW_SERVICE_ADDRESS),
                new TransformApiDocService(null) {
                    @Override
                    public String transformApiDoc(String serviceId, ApiDocInfo apiDocInfo) {
                        return apiDocInfo.getApiDocContent();
                    }
                },
                mock(ApiDocRetrievalServiceLocal.class),
                apiDocRetrievalServiceRest
            ) {
                @Override
                Mono<String> retrieveApiDoc(ServiceInstance serviceInstance, ApiInfo apiInfo) {
                    lastApiInfo.set(apiInfo);
                    return super.retrieveApiDoc(serviceInstance, apiInfo);
                }
            };

            ReflectionTestUtils.setField(apiDocRetrievalServiceRest, "apimlLogger", apimlLogger);
        }

        @Nested
        class WhenGetApiDoc {

            @Test
            void givenValidApiInfo_thenReturnApiDoc() {
                var responseBody = "api-doc body";

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getStandardMetadata(), true)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(lastApiInfo.get());
                        assertEquals(API_ID, lastApiInfo.get().getApiId());
                        assertEquals(GATEWAY_URL, lastApiInfo.get().getGatewayUrl());
                        assertEquals(SERVICE_VERSION, lastApiInfo.get().getVersion());
                        assertEquals(SWAGGER_URL, lastApiInfo.get().getSwaggerUrl());

                        assertNotNull(actualApiDoc);
                        assertEquals(responseBody, actualApiDoc);
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }

            @Nested
            class ThenThrowException {

                @Test
                void givenNoApiDocFoundForService() {
                    Exception exception = assertThrows(ApiDocNotFoundException.class, () -> apiDocService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V));
                    assertEquals("Could not load instance information for service " + SERVICE_ID + ".", exception.getMessage());
                }

                @Test
                void givenServerErrorWhenRequestingSwaggerUrl() {
                    String responseBody = "Server not found";

                    when(discoveryClient.getInstances(SERVICE_ID))
                        .thenReturn(Collections.singletonList(getStandardInstance(getStandardMetadata(), true)));

                    HttpClientMockHelper.mockResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, responseBody);

                    Mono<String> apiDocMono = apiDocService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V);
                    Exception exception = assertThrows(ApiDocNotFoundException.class, apiDocMono::block);
                    assertEquals("No API Documentation was retrieved due to " + SERVICE_ID + " server error: 500 " + responseBody, exception.getMessage());
                }

            }

            @Test
            void givenNoSwaggerUrl_thenReturnSubstituteApiDoc() {
                //language=JSON
                String generatedResponseBody = """
                    {
                        "swagger": "2.0",
                        "info": {
                            "title": "Test service"
                          , "description": "Test service description"
                          , "version": "1.0.0"
                        },
                        "host": "gateway:10000",
                        "basePath": "/service/api/v1",
                        "schemes": ["http"],
                        "tags": [
                            {
                                "name": "apimlHidden"
                            }
                        ],
                        "paths": {
                            "/apimlHidden": {
                                "get": {
                                    "tags": ["apimlHidden"],
                                    "responses": {
                                        "200": {
                                            "description": "OK"
                                        }
                                    }
                                }
                            }
                        }
                    }
                    """.replaceAll("\\s+", "");
                String responseBody = "api-doc body";

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getMetadataWithoutSwaggerUrl(), true)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(lastApiInfo.get());
                        assertEquals(API_ID, lastApiInfo.get().getApiId());
                        assertEquals(GATEWAY_URL, lastApiInfo.get().getGatewayUrl());
                        assertEquals(SERVICE_VERSION, lastApiInfo.get().getVersion());
                        assertNull(lastApiInfo.get().getSwaggerUrl());

                        assertNotNull(actualApiDoc);
                        assertEquals(generatedResponseBody, actualApiDoc.replaceAll("\\s+", ""));
                    })
                    .verifyComplete();
                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void givenApiDocUrlInRouting_thenCreateApiDocUrlFromRoutingAndReturnApiDoc() {
                var responseBody = "api-doc body";

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getMetadataWithoutApiInfo(), true)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(actualApiDoc);
                        assertEquals(responseBody, actualApiDoc);
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void shouldCreateApiDocUrlFromRoutingAndUseHttp() {
                var responseBody = "api-doc body";

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getMetadataWithoutApiInfo(), false)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(actualApiDoc);
                        assertEquals(responseBody, actualApiDoc);
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void givenServerCommunicationErrorWhenRequestingSwaggerUrl_thenLogCustomError() {
                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getStandardMetadata(), true)));

                var exception = new IOException("Unable to reach the host");
                HttpClientMockHelper.whenExecuteThenThrow(httpClient, exception);
                Mono<String> apiDocMono = apiDocService.retrieveDefaultApiDoc(SERVICE_ID);
                assertThrows(ApiDocNotFoundException.class, apiDocMono::block);

                assertNotNull(lastApiInfo.get());
                assertEquals(API_ID, lastApiInfo.get().getApiId());
                assertEquals(GATEWAY_URL, lastApiInfo.get().getGatewayUrl());
                assertEquals(SERVICE_VERSION, lastApiInfo.get().getVersion());
                assertEquals(SWAGGER_URL, lastApiInfo.get().getSwaggerUrl());

                verify(apimlLogger, times(1)).log("org.zowe.apiml.apicatalog.apiDocHostCommunication", SERVICE_ID, exception.getMessage());
            }
        }

        @Nested
        class WhenGetDefaultApiDoc {

            @Test
            void givenDefaultApiDoc_thenReturnIt() {
                var responseBody = "api-doc body";
                var metadata = getMetadataWithMultipleApiInfo();

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(metadata, true)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveDefaultApiDoc(SERVICE_ID))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(lastApiInfo.get());
                        assertEquals(API_ID, lastApiInfo.get().getApiId());
                        assertEquals(GATEWAY_URL, lastApiInfo.get().getGatewayUrl());
                        assertEquals(SERVICE_VERSION, lastApiInfo.get().getVersion());
                        assertEquals(SWAGGER_URL, lastApiInfo.get().getSwaggerUrl());

                        assertNotNull(actualApiDoc);
                        assertEquals(responseBody, actualApiDoc);
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void givenNoDefaultApiDoc_thenReturnHighestVersion() {
                var responseBody = "api-doc body";
                var metadata = getMetadataWithMultipleApiInfo();
                metadata.remove(API_INFO + ".1." + API_INFO_IS_DEFAULT); // unset default API, so higher version becomes default

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(metadata, true)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveDefaultApiDoc(SERVICE_ID))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(lastApiInfo.get());
                        assertEquals(API_ID, lastApiInfo.get().getApiId());
                        assertEquals(GATEWAY_URL, lastApiInfo.get().getGatewayUrl());
                        assertEquals(HIGHER_SERVICE_VERSION, lastApiInfo.get().getVersion());
                        assertEquals(SWAGGER_URL, lastApiInfo.get().getSwaggerUrl());

                        assertNotNull(actualApiDoc);
                        assertEquals(responseBody, actualApiDoc);
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void givenNoDefaultApiDocAndDifferentVersionFormat_thenReturnHighestVersion() {
                var responseBody = "api-doc body";

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getMetadataWithMultipleApiInfoWithDifferentVersionFormat(), true)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveDefaultApiDoc(SERVICE_ID))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(lastApiInfo.get());
                        assertEquals(API_ID, lastApiInfo.get().getApiId());
                        assertEquals(GATEWAY_URL, lastApiInfo.get().getGatewayUrl());
                        assertEquals(HIGHER_SERVICE_VERSION_V, lastApiInfo.get().getVersion());
                        assertEquals(SWAGGER_URL, lastApiInfo.get().getSwaggerUrl());

                        assertNotNull(actualApiDoc);
                        assertEquals(responseBody, actualApiDoc);
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }

            @Test
            void givenNoApiDocs_thenReturnNull() {
                var responseBody = "api-doc body";

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getMetadataWithoutApiInfo(), true)));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                var elapsed = StepVerifier.create(apiDocService.retrieveDefaultApiDoc(SERVICE_ID))
                    .assertNext(actualApiDoc -> {
                        assertNotNull(actualApiDoc);
                        assertEquals(responseBody, actualApiDoc);
                    })
                    .verifyComplete();

                assertEquals(0L, elapsed.toSeconds());
            }
        }

        @Nested
        class WhenGetApiVersions {
            @Test
            void givenApiVersions_thenReturnThem() {
                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getStandardMetadata(), false)));

                List<String> actualVersions = apiDocService.retrieveApiVersions(SERVICE_ID);
                assertEquals(Collections.singletonList(SERVICE_VERSION_V), actualVersions);
            }

            @Test
            void givenNoApiVersions_thenThrowException() {
                when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.emptyList());

                Exception exception = assertThrows(ApiVersionNotFoundException.class, () ->
                    apiDocService.retrieveApiVersions(SERVICE_ID)
                );
                assertEquals("Could not load instance information for service " + SERVICE_ID + ".", exception.getMessage());
            }
        }

        @Nested
        class WhenGetDefaultApiVersion {
            @Test
            void givenDefaultApiVersion_thenReturnIt() {
                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(getMetadataWithMultipleApiInfo(), false)));

                String defaultVersion = apiDocService.retrieveDefaultApiVersion(SERVICE_ID);
                assertEquals(SERVICE_VERSION_V, defaultVersion);
            }

            @Test
            void givenNoDefaultApiVersion_thenReturnHighestVersion() {
                Map<String, String> metadata = getMetadataWithMultipleApiInfo();
                metadata.remove(API_INFO + ".1." + API_INFO_IS_DEFAULT); // unset default API, so higher version becomes default

                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(Collections.singletonList(getStandardInstance(metadata, false)));

                String defaultVersion = apiDocService.retrieveDefaultApiVersion(SERVICE_ID);
                assertEquals(HIGHER_SERVICE_VERSION_V, defaultVersion);
            }

            @Test
            void givenNoApiInfo_thenThrowException() {
                when(discoveryClient.getInstances(SERVICE_ID)).thenReturn(Collections.emptyList());

                Exception exception = assertThrows(ApiVersionNotFoundException.class, () ->
                    apiDocService.retrieveDefaultApiVersion(SERVICE_ID)
                );
                assertEquals("Could not load instance information for service " + SERVICE_ID + ".", exception.getMessage());
            }
        }

        private ServiceInstance getStandardInstance(Map<String, String> metadata, Boolean isPortSecure) {
            InstanceInfo instance = InstanceInfo.Builder.newBuilder()
                .setAppName(SERVICE_ID)
                .setHostName(SERVICE_HOST)
                .setPort(SERVICE_PORT)
                .setSecurePort(SERVICE_PORT)
                .enablePort(InstanceInfo.PortType.SECURE, isPortSecure)
                .setStatus(InstanceInfo.InstanceStatus.UP)
                .setMetadata(metadata)
                .build();
            return new EurekaServiceInstance(instance);
        }

        private Map<String, String> getStandardMetadata() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(API_INFO + ".1." + API_INFO_API_ID, API_ID);
            metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, GATEWAY_URL);
            metadata.put(API_INFO + ".1." + API_INFO_VERSION, SERVICE_VERSION);
            metadata.put(API_INFO + ".1." + API_INFO_SWAGGER_URL, SWAGGER_URL);
            metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api");
            metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
            metadata.put(SERVICE_TITLE, "Test service");
            metadata.put(SERVICE_DESCRIPTION, "Test service description");

            return metadata;
        }

        private Map<String, String> getMetadataWithoutSwaggerUrl() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(API_INFO + ".1." + API_INFO_API_ID, API_ID);
            metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, GATEWAY_URL);
            metadata.put(API_INFO + ".1." + API_INFO_VERSION, SERVICE_VERSION);
            metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api");
            metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
            metadata.put(SERVICE_TITLE, "Test service");
            metadata.put(SERVICE_DESCRIPTION, "Test service description");

            return metadata;
        }

        private Map<String, String> getMetadataWithMultipleApiInfo() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(API_INFO + ".1." + API_INFO_API_ID, API_ID);
            metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, GATEWAY_URL);
            metadata.put(API_INFO + ".1." + API_INFO_VERSION, SERVICE_VERSION);
            metadata.put(API_INFO + ".1." + API_INFO_SWAGGER_URL, SWAGGER_URL);
            metadata.put(API_INFO + ".1." + API_INFO_IS_DEFAULT, "true");

            metadata.put(API_INFO + ".2." + API_INFO_API_ID, API_ID);
            metadata.put(API_INFO + ".2." + API_INFO_GATEWAY_URL, GATEWAY_URL);
            metadata.put(API_INFO + ".2." + API_INFO_VERSION, HIGHER_SERVICE_VERSION);
            metadata.put(API_INFO + ".2." + API_INFO_SWAGGER_URL, SWAGGER_URL);

            metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api");
            metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
            metadata.put(SERVICE_TITLE, "Test service");
            metadata.put(SERVICE_DESCRIPTION, "Test service description");

            return metadata;
        }

        private Map<String, String> getMetadataWithMultipleApiInfoWithDifferentVersionFormat() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(API_INFO + ".1." + API_INFO_API_ID, API_ID);
            metadata.put(API_INFO + ".1." + API_INFO_GATEWAY_URL, GATEWAY_URL);
            metadata.put(API_INFO + ".1." + API_INFO_VERSION, SERVICE_VERSION_V);
            metadata.put(API_INFO + ".1." + API_INFO_SWAGGER_URL, SWAGGER_URL);

            metadata.put(API_INFO + ".2." + API_INFO_API_ID, API_ID);
            metadata.put(API_INFO + ".2." + API_INFO_GATEWAY_URL, GATEWAY_URL);
            metadata.put(API_INFO + ".2." + API_INFO_VERSION, HIGHER_SERVICE_VERSION_V);
            metadata.put(API_INFO + ".2." + API_INFO_SWAGGER_URL, SWAGGER_URL);

            metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api");
            metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
            metadata.put(SERVICE_TITLE, "Test service");
            metadata.put(SERVICE_DESCRIPTION, "Test service description");

            return metadata;
        }

        private Map<String, String> getMetadataWithoutApiInfo() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(ROUTES + ".api-v1." + ROUTES_GATEWAY_URL, "api");
            metadata.put(ROUTES + ".api-v1." + ROUTES_SERVICE_URL, "/");
            metadata.put(ROUTES + ".apidoc." + ROUTES_GATEWAY_URL, "api/v1/api-doc");
            metadata.put(ROUTES + ".apidoc." + ROUTES_SERVICE_URL, SERVICE_ID + "/api-doc");
            metadata.put(SERVICE_TITLE, "Test service");
            metadata.put(SERVICE_DESCRIPTION, "Test service description");

            return metadata;
        }

    }

    @Nested
    @SpringBootTest
    class ViaApiCall {

        @MockitoSpyBean
        private ApiDocService apiDocService;

        @MockitoSpyBean
        private ApiDocRetrievalServiceLocal apiDocRetrievalServiceLocal;

        @Autowired
        private GatewayClient gatewayClient;

        @BeforeEach
        void onboardCatalog() {
            InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
                .setAppName("apicatalog")
                .setMetadata(Map.of(
                    "apiml.apiInfo.0.apiId", "zowe.apiml.apicatalog",
                    "apiml.apiInfo.0.gatewayUrl", "api/v1",
                    "apiml.apiInfo.0.swaggerUrl", "https://localhost:10010/v3/api-doc",
                    "apiml.apiInfo.0.version", "1.0.0"
                ))
                .build();
            doReturn(new EurekaServiceInstance(instanceInfo)).when(apiDocService).getInstanceInfo("apicatalog");

            gatewayClient.setGatewayConfigProperties(GW_SERVICE_ADDRESS);
        }

        @Test
        void givenApiCatalogId_whenRetrieveApiDoc_thenCallLocally() {
            StepVerifier.create(apiDocService.retrieveApiDoc(CoreService.API_CATALOG.getServiceId(), "zowe.apiml.apicatalog v1.0.0"))
                .expectNextMatches(apiDoc -> apiDoc.contains("/containers/{id}"))
                .verifyComplete();

            verify(apiDocRetrievalServiceLocal).retrieveApiDoc(any(), any());
        }

    }

}
