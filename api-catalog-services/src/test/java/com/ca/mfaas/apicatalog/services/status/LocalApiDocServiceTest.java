/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status;

import com.ca.mfaas.apicatalog.gateway.GatewayConfigProperties;
import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LocalApiDocServiceTest {
    private static final String SERVICE_ID = "service";
    private static final String SERVICE_HOST = "service";
    private static final int SERVICE_PORT = 8080;
    private static final String SERVICE_VERSION = "1.0.0";
    private static final String GATEWAY_SCHEME = "http";
    private static final String GATEWAY_HOST = "gateway:10000";
    private static final String GATEWAY_URL = "api/v1";
    private static final String API_ID = "test.app";
    private static final String SWAGGER_URL = "https://service:8080/service/api-doc";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    private APIDocRetrievalService apiDocRetrievalService;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Before
    public void setup() {
        apiDocRetrievalService = new APIDocRetrievalService(
            restTemplate,
            instanceRetrievalService,
            getProperties());
    }

    @Test
    public void testRetrievalOfAPIDoc() {
        String responseBody = "api-doc body";

        when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
            .thenReturn(getStandardInstance(getStandardMetadata(), true));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION);

        assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
        assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
        assertEquals(SERVICE_VERSION, actualResponse.getApiInfo().getVersion());
        assertEquals(SWAGGER_URL, actualResponse.getApiInfo().getSwaggerUrl());

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getApiDocContent());
        assertEquals(responseBody, actualResponse.getApiDocContent());

        assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());

        assertEquals(GATEWAY_HOST, actualResponse.getGatewayHost());
        assertEquals(GATEWAY_SCHEME, actualResponse.getGatewayScheme());
    }

    @Test
    public void testFailedRetrievalOfAPIDocWhenServiceNotFound() {
        exceptionRule.expect(ApiDocNotFoundException.class);
        exceptionRule.expectMessage("Could not load instance information for service " + SERVICE_ID + " .");

        apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION);
    }

    @Test
    public void testFailedRetrievalOfAPIDocWhenServerError() {
        String responseBody = "Server not found";

        when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
            .thenReturn(getStandardInstance(getStandardMetadata(), true));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        exceptionRule.expect(ApiDocNotFoundException.class);
        exceptionRule.expectMessage("No API Documentation was retrieved due to " + SERVICE_ID + " server error: '" + responseBody + "'.");

        apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION);
    }

    @Test
    public void testFailedRetrievalOfAPIDocWhenMetadataNotDefined() {
        String responseBody = "api-doc body";

        when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
            .thenReturn(getStandardInstance(new HashMap<>(), true));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        exceptionRule.expect(ApiDocNotFoundException.class);
        exceptionRule.expectMessage("No API Documentation defined for service " + SERVICE_ID + " .");

        apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION);
    }

    @Test
    public void shouldGenerateSubstituteSwaggerIfSwaggerUrlNull() {
        String generatedResponseBody = "{\n" +
            "    \"swagger\": \"2.0\",\n" +
            "    \"info\": {\n" +
            "        \"title\": \"Test service\"\n" +
            "      , \"description\": \"Test service description\"\n" +
            "      , \"version\": \"1.0.0\"\n" +
            "    },\n" +
            "    \"host\": \"gateway:10000\",\n" +
            "    \"basePath\": \"/api/v1/service\",\n" +
            "    \"schemes\": [\"http\"],\n" +
            "    \"tags\": [\n" +
            "        {\n" +
            "            \"name\": \"apimlHidden\"\n" +
            "        }\n" +
            "    ],\n" +
            "    \"paths\": {\n" +
            "        \"/apimlHidden\": {\n" +
            "            \"get\": {\n" +
            "                \"tags\": [\"apimlHidden\"],\n" +
            "                \"responses\": {\n" +
            "                    \"200\": {\n" +
            "                        \"description\": \"OK\"\n" +
            "                    }\n" +
            "                }\n" +
            "            }\n" +
            "        }\n" +
            "    }\n" +
            "}\n";
        String responseBody = "api-doc body";

        generatedResponseBody = generatedResponseBody.replaceAll("\\s+", "");
        when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
            .thenReturn(getStandardInstance(getMetadataWithoutSwaggerUrl(), true));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION);

        assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
        assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
        assertEquals(SERVICE_VERSION, actualResponse.getApiInfo().getVersion());
        assertNull(actualResponse.getApiInfo().getSwaggerUrl());

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getApiDocContent());
        assertEquals(generatedResponseBody, actualResponse.getApiDocContent().replaceAll("\\s+", ""));

        assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());

        assertEquals(GATEWAY_HOST, actualResponse.getGatewayHost());
        assertEquals(GATEWAY_SCHEME, actualResponse.getGatewayScheme());
    }

    @Test
    public void shouldCreateApiDocUrlFromRouting() {
        String responseBody = "api-doc body";

        when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
            .thenReturn(getStandardInstance(getMetadataWithoutApiInfo(),true));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getApiDocContent());

        assertEquals(responseBody, actualResponse.getApiDocContent());
    }

    @Test
    public void shouldCreateApiDocUrlFromRoutingAndUseHttp() {
        String responseBody = "api-doc body";

        when(instanceRetrievalService.getInstanceInfo(SERVICE_ID))
            .thenReturn(getStandardInstance(getMetadataWithoutApiInfo(),false));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange("http://service:8080/service/api-doc", HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE_ID, SERVICE_VERSION);

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getApiDocContent());

        assertEquals(responseBody, actualResponse.getApiDocContent());
    }

    private HttpEntity<Object> getObjectHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

        return new HttpEntity<>(headers);
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
        metadata.put("apiml.apiInfo.1.apiId", API_ID);
        metadata.put("apiml.apiInfo.1.gatewayUrl", GATEWAY_URL);
        metadata.put("apiml.apiInfo.1.version", SERVICE_VERSION);
        metadata.put("apiml.apiInfo.1.swaggerUrl", SWAGGER_URL);
        metadata.put("routed-services.api-v1.gateway-url", "api");
        metadata.put("routed-services.api-v1.service-url", "/");
        metadata.put("mfaas.discovery.service.title", "Test service");
        metadata.put("mfaas.discovery.service.description", "Test service description");

        return metadata;
    }

    private Map<String, String> getMetadataWithoutSwaggerUrl() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.1.apiId", API_ID);
        metadata.put("apiml.apiInfo.1.gatewayUrl", GATEWAY_URL);
        metadata.put("apiml.apiInfo.1.version", SERVICE_VERSION);
        metadata.put("routed-services.api-v1.gateway-url", "api");
        metadata.put("routed-services.api-v1.service-url", "/");
        metadata.put("mfaas.discovery.service.title", "Test service");
        metadata.put("mfaas.discovery.service.description", "Test service description");

        return metadata;
    }

    private Map<String, String> getMetadataWithoutApiInfo() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("routed-services.api-v1.gateway-url", "api");
        metadata.put("routed-services.api-v1.service-url", "/");
        metadata.put("routed-services.apidoc.gateway-url", "api/v1/api-doc");
        metadata.put("routed-services.apidoc.service-url", SERVICE_ID + "/api-doc");
        metadata.put("mfaas.discovery.service.title", "Test service");
        metadata.put("mfaas.discovery.service.description", "Test service description");

        return metadata;
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme(GATEWAY_SCHEME)
            .hostname(GATEWAY_HOST)
            .build();
    }
}
