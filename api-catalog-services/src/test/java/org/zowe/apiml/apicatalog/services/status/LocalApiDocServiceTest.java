/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.services.status;

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
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.apicatalog.instance.InstanceRetrievalService;
import org.zowe.apiml.apicatalog.services.cached.model.ApiDocInfo;
import org.zowe.apiml.apicatalog.services.status.model.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.services.status.model.ApiVersionNotFoundException;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.gateway.GatewayClient;
import org.zowe.apiml.product.instance.ServiceAddress;
import org.zowe.apiml.util.HttpClientMockHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LocalApiDocServiceTest {
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

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    private APIDocRetrievalService apiDocRetrievalService;

    @Mock
    private ApimlLogger apimlLogger;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse response;

    @BeforeEach
    void setup() {
        HttpClientMockHelper.mockExecuteWithResponse(httpClient, response);

        apiDocRetrievalService = new APIDocRetrievalService(
            httpClient,
            instanceRetrievalService,
            new GatewayClient(getProperties()));

        ReflectionTestUtils.setField(apiDocRetrievalService, "apimlLogger", apimlLogger);
    }

    @Nested
    class WhenGetApiDoc {
        @Test
        void givenValidApiInfo_thenReturnApiDoc() {
            String responseBody = "api-doc body";

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getStandardMetadata(), true));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V);

            assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
            assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
            assertEquals(SERVICE_VERSION, actualResponse.getApiInfo().getVersion());
            assertEquals(SWAGGER_URL, actualResponse.getApiInfo().getSwaggerUrl());

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());
            assertEquals(responseBody, actualResponse.getApiDocContent());

            assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());
        }

        @Nested
        class ThenThrowException {
            @Test
            void givenNoApiDocFoundForService() {
                Exception exception = assertThrows(ApiDocNotFoundException.class, () -> apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V));
                assertEquals("Could not load instance information for service " + SERVICE_ID + ".", exception.getMessage());
            }

            @Test
            void givenServerErrorWhenRequestingSwaggerUrl() {
                String responseBody = "Server not found";

                when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                    .thenReturn(getStandardInstance(getStandardMetadata(), true));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_INTERNAL_SERVER_ERROR, responseBody);

                Exception exception = assertThrows(ApiDocNotFoundException.class, () -> apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V));
                assertEquals("No API Documentation was retrieved due to " + SERVICE_ID + " server error: '" + responseBody + "'.", exception.getMessage());
            }

            @Test
            void givenNoInstanceMetadata() {
                String responseBody = "api-doc body";

                when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                    .thenReturn(getStandardInstance(new HashMap<>(), true));

                HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

                Exception exception = assertThrows(ApiDocNotFoundException.class, () -> apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V));
                assertEquals("No API Documentation defined for service " + SERVICE_ID + ".", exception.getMessage());
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
                """;
            String responseBody = "api-doc body";

            generatedResponseBody = generatedResponseBody.replaceAll("\\s+", "");
            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getMetadataWithoutSwaggerUrl(), true));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V);

            assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
            assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
            assertEquals(SERVICE_VERSION, actualResponse.getApiInfo().getVersion());
            assertNull(actualResponse.getApiInfo().getSwaggerUrl());

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());
            assertEquals(generatedResponseBody, actualResponse.getApiDocContent().replaceAll("\\s+", ""));

            assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());
        }

        @Test
        void givenApiDocUrlInRouting_thenCreateApiDocUrlFromRoutingAndReturnApiDoc() {
            String responseBody = "api-doc body";

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getMetadataWithoutApiInfo(), true));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V);

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());

            assertEquals(responseBody, actualResponse.getApiDocContent());
        }

        @Test
        void shouldCreateApiDocUrlFromRoutingAndUseHttp() {
            String responseBody = "api-doc body";

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getMetadataWithoutApiInfo(), false));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION_V);

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());

            assertEquals(responseBody, actualResponse.getApiDocContent());
        }

        @Test
        void givenServerCommunicationErrorWhenRequestingSwaggerUrl_thenLogCustomError() {
            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getStandardMetadata(), true));

            var exception = new IOException("Unable to reach the host");
            HttpClientMockHelper.whenExecuteThenThrow(httpClient, exception);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveDefaultApiDoc(SERVICE_ID);

            assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
            assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
            assertEquals(SERVICE_VERSION, actualResponse.getApiInfo().getVersion());
            assertEquals(SWAGGER_URL, actualResponse.getApiInfo().getSwaggerUrl());

            assertEquals("", actualResponse.getApiDocContent());

            verify(apimlLogger, times(1)).log("org.zowe.apiml.apicatalog.apiDocHostCommunication", SERVICE_ID, exception.getMessage());
        }
    }

    @Nested
    class WhenGetDefaultApiDoc {
        @Test
        void givenDefaultApiDoc_thenReturnIt() {
            String responseBody = "api-doc body";
            Map<String, String> metadata = getMetadataWithMultipleApiInfo();

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(metadata, true));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveDefaultApiDoc(SERVICE_ID);

            assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
            assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
            assertEquals(SERVICE_VERSION, actualResponse.getApiInfo().getVersion());
            assertEquals(SWAGGER_URL, actualResponse.getApiInfo().getSwaggerUrl());

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());
            assertEquals(responseBody, actualResponse.getApiDocContent());

            assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());
        }

        @Test
        void givenNoDefaultApiDoc_thenReturnHighestVersion() {
            String responseBody = "api-doc body";
            Map<String, String> metadata = getMetadataWithMultipleApiInfo();
            metadata.remove(API_INFO + ".1." + API_INFO_IS_DEFAULT); // unset default API, so higher version becomes default

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(metadata, true));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveDefaultApiDoc(SERVICE_ID);

            assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
            assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
            assertEquals(HIGHER_SERVICE_VERSION, actualResponse.getApiInfo().getVersion());
            assertEquals(SWAGGER_URL, actualResponse.getApiInfo().getSwaggerUrl());

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());
            assertEquals(responseBody, actualResponse.getApiDocContent());

            assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());
        }

        @Test
        void givenNoDefaultApiDocAndDifferentVersionFormat_thenReturnHighestVersion() {
            String responseBody = "api-doc body";

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getMetadataWithMultipleApiInfoWithDifferentVersionFormat(), true));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveDefaultApiDoc(SERVICE_ID);

            assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
            assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
            assertEquals(HIGHER_SERVICE_VERSION_V, actualResponse.getApiInfo().getVersion());
            assertEquals(SWAGGER_URL, actualResponse.getApiInfo().getSwaggerUrl());

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());
            assertEquals(responseBody, actualResponse.getApiDocContent());

            assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());
        }

        @Test
        void givenNoApiDocs_thenReturnNull() {
            String responseBody = "api-doc body";

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getMetadataWithoutApiInfo(), true));

            HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK, responseBody);

            ApiDocInfo actualResponse = apiDocRetrievalService.retrieveDefaultApiDoc(SERVICE_ID);

            assertNotNull(actualResponse);
            assertNotNull(actualResponse.getApiDocContent());

            assertEquals(responseBody, actualResponse.getApiDocContent());
        }
    }

    @Nested
    class WhenGetApiVersions {
        @Test
        void givenApiVersions_thenReturnThem() {
            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getStandardMetadata(), false));

            List<String> actualVersions = apiDocRetrievalService.retrieveApiVersions(SERVICE_ID);
            assertEquals(Collections.singletonList(SERVICE_VERSION_V), actualVersions);
        }

        @Test
        void givenNoApiVersions_thenThrowException() {
            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID)).thenReturn(null);

            Exception exception = assertThrows(ApiVersionNotFoundException.class, () ->
                apiDocRetrievalService.retrieveApiVersions(SERVICE_ID)
            );
            assertEquals("Could not load instance information for service " + SERVICE_ID + ".", exception.getMessage());
        }
    }

    @Nested
    class WhenGetDefaultApiVersion {
        @Test
        void givenDefaultApiVersion_thenReturnIt() {
            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(getMetadataWithMultipleApiInfo(), false));

            String defaultVersion = apiDocRetrievalService.retrieveDefaultApiVersion(SERVICE_ID);
            assertEquals(SERVICE_VERSION_V, defaultVersion);
        }

        @Test
        void givenNoDefaultApiVersion_thenReturnHighestVersion() {
            Map<String, String> metadata = getMetadataWithMultipleApiInfo();
            metadata.remove(API_INFO + ".1." + API_INFO_IS_DEFAULT); // unset default API, so higher version becomes default

            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
                .thenReturn(getStandardInstance(metadata, false));

            String defaultVersion = apiDocRetrievalService.retrieveDefaultApiVersion(SERVICE_ID);
            assertEquals(HIGHER_SERVICE_VERSION_V, defaultVersion);
        }

        @Test
        void givenNoApiInfo_thenThrowException() {
            when(instanceRetrievalService.getInstanceInfo(SERVICE_ID)).thenReturn(null);

            Exception exception = assertThrows(ApiVersionNotFoundException.class, () ->
                apiDocRetrievalService.retrieveDefaultApiVersion(SERVICE_ID)
            );
            assertEquals("Could not load instance information for service " + SERVICE_ID + ".", exception.getMessage());
        }
    }

    private InstanceInfo getStandardInstance(Map<String, String> metadata, Boolean isPortSecure) {
        return InstanceInfo.Builder.newBuilder()
            .setAppName(SERVICE_ID)
            .setHostName(SERVICE_HOST)
            .setPort(SERVICE_PORT)
            .setSecurePort(SERVICE_PORT)
            .enablePort(InstanceInfo.PortType.SECURE, isPortSecure)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(metadata)
            .build();
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

    private ServiceAddress getProperties() {
        return ServiceAddress.builder()
            .scheme(GATEWAY_SCHEME)
            .hostname(GATEWAY_HOST)
            .build();
    }
}
