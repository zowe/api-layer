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

import com.ca.mfaas.apicatalog.services.cached.model.ApiDocInfo;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LocalApiDocServiceTest {
    private static final String HOSTNAME = "localhost";
    private static final int PORT = 10000;
    private static final int SECURE_PORT = 10001;
    private static final String SWAGGER_URL = "https://service/api-doc";
    private static final String GATEWAY_URL = "api/v1";
    private static final String API_ID = "test.app";
    private static final String API_DOC = "/api-doc";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Spy
    @InjectMocks
    private APIDocRetrievalService apiDocRetrievalService;

    @Rule
    public ExpectedException exceptionRule = ExpectedException.none();

    @Test
    public void testRetrievalOfAPIDoc() {
        String serviceId = "service1";
        String version = "v1";
        String responseBody = "api-doc body";

        when(instanceRetrievalService.getInstanceInfo(serviceId))
            .thenReturn(getStandardInstance(serviceId, version, InstanceInfo.InstanceStatus.UP));
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(getStandardInstance(CoreService.GATEWAY.getServiceId(), version, InstanceInfo.InstanceStatus.UP));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(serviceId, version);

        assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
        assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
        assertEquals(version, actualResponse.getApiInfo().getVersion());
        assertEquals(SWAGGER_URL, actualResponse.getApiInfo().getSwaggerUrl());

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getApiDocResponse());
        assertEquals(responseBody, actualResponse.getApiDocResponse().getBody());
        assertEquals(HttpStatus.OK, actualResponse.getApiDocResponse().getStatusCode());

        assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());

        assertEquals(HOSTNAME + ":" + PORT, actualResponse.getGatewayHost());
        assertEquals("http", actualResponse.getGatewayScheme());
    }

    @Test
    public void testFailedRetrievalOfAPIDocWhenServiceNotFound() {
        String serviceId = "service1";
        String version = "v1";

        exceptionRule.expect(ApiDocNotFoundException.class);
        exceptionRule.expectMessage("Could not load instance information for service " + serviceId + " .");

        apiDocRetrievalService.retrieveApiDoc(serviceId, version);
    }

    @Test
    public void testFailedRetrievalOfAPIDocWhenGatewayNotFound() {
        String serviceId = "service1";
        String version = "v1";

        when(instanceRetrievalService.getInstanceInfo(serviceId))
            .thenReturn(getStandardInstance(CoreService.GATEWAY.getServiceId(), version, InstanceInfo.InstanceStatus.UP));

        exceptionRule.expect(ApiDocNotFoundException.class);
        exceptionRule.expectMessage("Could not load gateway instance for service " + serviceId + ".");

        apiDocRetrievalService.retrieveApiDoc(serviceId, version);
    }

    @Test
    public void testFailedRetrievalOfAPIDocWhenServerError() throws CannotRegisterServiceException {
        String serviceId = "service1";
        String version = "v1";
        String responseBody = "Server not found";

        when(instanceRetrievalService.getInstanceInfo(serviceId))
            .thenReturn(getStandardInstance(serviceId, version, InstanceInfo.InstanceStatus.UP));
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(getStandardInstance(CoreService.GATEWAY.getServiceId(), version, InstanceInfo.InstanceStatus.UP));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        exceptionRule.expect(ApiDocNotFoundException.class);
        exceptionRule.expectMessage("No API Documentation was retrieved due to " + serviceId + " server error: 'Server not found'.");

        apiDocRetrievalService.retrieveApiDoc(serviceId, version);
    }

    private HttpEntity<Object> getObjectHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON_UTF8));

        return new HttpEntity<>(headers);
    }

    private InstanceInfo getStandardInstance(String serviceId, String version, InstanceInfo.InstanceStatus status) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.1.apiId", API_ID);
        metadata.put("apiml.apiInfo.1.gatewayUrl", GATEWAY_URL);
        metadata.put("apiml.apiInfo.1.version", version);
        metadata.put("apiml.apiInfo.1.swaggerUrl", SWAGGER_URL);
        metadata.put("routed-services.api-v1.gateway-url", "api");
        metadata.put("routed-services.api-v1.service-url", "/");

        return InstanceInfo.Builder.newBuilder()
            .setAppName(serviceId)
            .setHostName(HOSTNAME)
            .setPort(PORT)
            .setStatus(status)
            .setMetadata(metadata)
            .build();
    }

    private InstanceInfo getStandardInstance2(String serviceId, String version, InstanceInfo.InstanceStatus status) {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.1.apiId", API_ID);
        metadata.put("apiml.apiInfo.1.gatewayUrl", GATEWAY_URL);
        metadata.put("apiml.apiInfo.1.version", version);
        metadata.put("apiml.apiInfo.1.swaggerUrl", null);
        metadata.put("routed-services.api-v1.gateway-url", "api");
        metadata.put("routed-services.api-v1.service-url", "/");

        return InstanceInfo.Builder.newBuilder()
            .setAppName(serviceId)
            .setHostName(HOSTNAME)
            .setPort(PORT)
            .setStatus(status)
            .setMetadata(metadata)
            .build();
    }

    @Test
    public void testFailedRetrievalOfAPIDocWhenMetadataNotDefined() {

        String serviceId = "service1";
        String version = "v1";

        String responseBody = "api-doc body";
        Map<String, String> metadata = new HashMap<>();
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
            .setAppName(serviceId)
            .setHostName(HOSTNAME)
            .setPort(PORT)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(metadata)
            .build();
        when(instanceRetrievalService.getInstanceInfo(serviceId))
            .thenReturn(instanceInfo);
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(instanceInfo);

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);
        exceptionRule.expect(ApiDocNotFoundException.class);
        exceptionRule.expectMessage("No API Documentation defined for service " + serviceId + " .");
        apiDocRetrievalService.retrieveApiDoc(serviceId, version);
    }

    @Test
    public void shouldGenerateSubstituteSwaggerIfSwaggerUrlNull() {

        String serviceId = "service1";
        String version = "v1";
        String generatedResponseBody = "{\n" +
            "    \"swagger\": \"2.0\",\n" +
            "    \"info\": {\n" +
            "        \"title\": \"$title\"\n" +
            "      , \"description\": \"$description\"\n" +
            "      , \"version\": \"v1\"\n" +
            "    },\n" +
            "    \"host\": \"localhost:10000\",\n" +
            "    \"basePath\": \"/api/v1/service1\",\n" +
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
        generatedResponseBody = generatedResponseBody.replaceAll("\\s+","");
        when(instanceRetrievalService.getInstanceInfo(serviceId))
            .thenReturn(getStandardInstance2(serviceId, version, InstanceInfo.InstanceStatus.UP));
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(getStandardInstance2(CoreService.GATEWAY.getServiceId(), version, InstanceInfo.InstanceStatus.UP));

        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.INTERNAL_SERVER_ERROR);
        when(restTemplate.exchange(SWAGGER_URL, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);

        ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(serviceId, version);
        assertEquals(API_ID, actualResponse.getApiInfo().getApiId());
        assertEquals(GATEWAY_URL, actualResponse.getApiInfo().getGatewayUrl());
        assertEquals(version, actualResponse.getApiInfo().getVersion());
        assertEquals(null, actualResponse.getApiInfo().getSwaggerUrl());

        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getApiDocResponse());
        assertEquals(generatedResponseBody, actualResponse.getApiDocResponse().getBody().replaceAll("\\s+",""));
        assertEquals(HttpStatus.OK, actualResponse.getApiDocResponse().getStatusCode());

        assertEquals("[api -> api=RoutedService(subServiceId=api-v1, gatewayUrl=api, serviceUrl=/)]", actualResponse.getRoutes().toString());

        assertEquals(HOSTNAME + ":" + PORT, actualResponse.getGatewayHost());
        assertEquals("http", actualResponse.getGatewayScheme());
    }

    @Test
    public void shouldCreateApiDocUrlFromRouting() {

        String serviceId = "service1";
        String version = "v1";
        String responseBody = "api-doc body";
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.apiId", API_ID);
        metadata.put("apiml.apiInfo.gatewayUrl", GATEWAY_URL);
        metadata.put("apiml.apiInfo.version", version);
        metadata.put("apiml.apiInfo.swaggerUrl", SWAGGER_URL);
        metadata.put("routed-services.api-v1.gateway-url", "api");
        metadata.put("routed-services.api-v1.service-url", "/");
        metadata.put("routed-services.api-doc.service-url", '/' + serviceId + API_DOC);
        InstanceInfo instanceInfo = InstanceInfo.Builder.newBuilder()
            .setAppName(serviceId)
            .setHostName(HOSTNAME)
            .setSecurePort(SECURE_PORT)
            .enablePort(InstanceInfo.PortType.UNSECURE, false)
            .enablePort(InstanceInfo.PortType.SECURE, true)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setMetadata(metadata)
            .build();

        when(instanceRetrievalService.getInstanceInfo(serviceId))
            .thenReturn(instanceInfo);
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(instanceInfo);
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(responseBody, HttpStatus.OK);
        when(restTemplate.exchange("https://" + HOSTNAME + ":" + SECURE_PORT + "/" + serviceId + API_DOC, HttpMethod.GET, getObjectHttpEntity(), String.class))
            .thenReturn(expectedResponse);
        ApiDocInfo actualResponse = apiDocRetrievalService.retrieveApiDoc(serviceId, version);
        assertNotNull(actualResponse);
        assertNotNull(actualResponse.getApiDocResponse());
        assertEquals(responseBody, actualResponse.getApiDocResponse().getBody());
        assertEquals(HttpStatus.OK, actualResponse.getApiDocResponse().getStatusCode());
    }

}
