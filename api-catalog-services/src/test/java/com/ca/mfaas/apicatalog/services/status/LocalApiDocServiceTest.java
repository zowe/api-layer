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

import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LocalApiDocServiceTest {

    private static final String SERVICE1_ID = "service-1";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Spy
    @InjectMocks
    private APIDocRetrievalService apiDocRetrievalService;

    private InstanceInfo expectedGatewayInstance;
    private InstanceInfo expectedServiceOneInstance;

    @Before
    public void setup() {
        expectedGatewayInstance = getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP);
        expectedServiceOneInstance = getStandardInstance(SERVICE1_ID, InstanceInfo.InstanceStatus.UP);
    }

    @Test
    public void givenInstance_whenMetadataHasNotSwaggerURL_thenCheckParamsByMetadata() {
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(expectedGatewayInstance);

        expectedServiceOneInstance.getMetadata().put("mfaas.discovery.service.title", "title");
        expectedServiceOneInstance.getMetadata().put("mfaas.discovery.service.description", "description");

        expectedServiceOneInstance.getMetadata().put("apiml.apiInfo.1.gatewayUrl", "/gatewayUrl");
        expectedServiceOneInstance.getMetadata().put("apiml.apiInfo.1.version", "version");
        expectedServiceOneInstance.getMetadata().put("apiml.apiInfo.1.documentationUrl", "/documentationUrl");

        when(instanceRetrievalService.getInstanceInfo(SERVICE1_ID)).thenReturn(expectedServiceOneInstance);

        ResponseEntity<String> actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE1_ID, null);
        assertNotNull(actualResponse);
        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());

        JsonObject jsonObject = new JsonParser().parse(actualResponse.getBody()).getAsJsonObject();
        assertEquals("title", jsonObject.get("info").getAsJsonObject().get("title").getAsString());
        assertEquals("description", jsonObject.get("info").getAsJsonObject().get("description").getAsString());
        assertEquals("version", jsonObject.get("info").getAsJsonObject().get("version").getAsString());
        assertEquals("/documentationUrl", jsonObject.get("externalDocs").getAsJsonObject().get("url").getAsString());
        assertEquals("/gatewayUrl/" + SERVICE1_ID, jsonObject.get("basePath").getAsString());
    }

    @Test
    public void givenInstance_whenMetadataHasSwaggerURL_thenCheckParamsByApiDoc() {
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(expectedGatewayInstance);

        expectedServiceOneInstance.getMetadata().put("routed-services.api-v1.service-url", "/swaggerId/api/v1");
        expectedServiceOneInstance.getMetadata().put("apiml.apiInfo.1.gatewayUrl", "api/v1");
        expectedServiceOneInstance.getMetadata().put("apiml.apiInfo.1.swaggerUrl", "swaggerUrl");

        when(instanceRetrievalService.getInstanceInfo(SERVICE1_ID)).thenReturn(expectedServiceOneInstance);

        String url =  "https://localhost:9090/api/v1/api-doc/swaggerId/";
        HttpEntity<Object> httpEntity = getObjectHttpEntity();
        when(restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class))
            .thenReturn(
                new ResponseEntity<>("{\"basePath\":\"/api/v1/swaggerId\"}", HttpStatus.OK)
            );

        ResponseEntity<String> actualResponse = apiDocRetrievalService.retrieveApiDoc(SERVICE1_ID, "v1");
        assertNotNull(actualResponse);
        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());

        String expectedApiDoc = String.format("{\"basePath\":\"/api/v1/%s\"}", SERVICE1_ID);
        assertEquals(expectedApiDoc, actualResponse.getBody());
    }

    @Test
    public void testRetrievalOfAPIDoc() {
        HttpEntity<Object> httpEntity = getObjectHttpEntity();

        String apiDoc = "api doc goes here";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(apiDoc, HttpStatus.OK);

        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(expectedGatewayInstance);

        String url =  "https://localhost:9090/api/v1/api-doc/service1";
        when(
            restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class))
            .thenReturn(expectedResponse);
        ResponseEntity<String> actualResponse = apiDocRetrievalService.retrieveApiDoc("service1", null);
        assertNotNull(actualResponse);
        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    }

    @Test(expected = ApiDocNotFoundException.class)
    public void testFailedRetrievalOfAPIDocWhenGatewayNotFound() {
        HttpEntity<Object> httpEntity = getObjectHttpEntity();

        String apiDoc = "{}";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(apiDoc, HttpStatus.NOT_FOUND);

        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(expectedGatewayInstance);

        String url =  "https://localhost:9090/api/v1/api-doc/service1";
        when(
            restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class))
            .thenReturn(expectedResponse);
        apiDocRetrievalService.retrieveApiDoc("service1", null);
    }

    @Test(expected = ApiDocNotFoundException.class)
    public void testFailedRetrievalOfAPIDocWhenServerError() throws CannotRegisterServiceException {
        HttpEntity<Object> httpEntity = getObjectHttpEntity();

        String apiDoc = "{}";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(apiDoc, HttpStatus.INTERNAL_SERVER_ERROR);

        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(expectedGatewayInstance);

        String url =  "https://localhost:9090/api/v1/api-doc/service1";
        when(
            restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class))
            .thenReturn(expectedResponse);
        apiDocRetrievalService.retrieveApiDoc("service1", null);
    }

    private HttpEntity<Object> getObjectHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON_UTF8);
        headers.setAccept(types);
        return new HttpEntity<>(headers);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status) {
        return new InstanceInfo(serviceId, serviceId, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
                new InstanceInfo.PortWrapper(true, 9090), "https://localhost:9090/", null, null, null, "localhost", "localhost", 0, null,
                "localhost", status, null, null, null, null, new HashMap<>(), null, null, null, null);
    }
}
