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

import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.apicatalog.services.status.model.ApiDocNotFoundException;
import com.ca.mfaas.product.family.ProductFamilyType;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.netflix.appinfo.InstanceInfo;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class LocalApiDocServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private CachedServicesService cachedServicesService;

    @Spy
    @InjectMocks
    private APIDocRetrievalService apiDocRetrievalService;

    @Test
    public void testRetrievalOfAPIDoc() {
        HttpEntity<Object> httpEntity = getObjectHttpEntity();

        String apiDoc = "api doc goes here";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(apiDoc, HttpStatus.OK);

        when(cachedServicesService.getInstanceInfoForService(ProductFamilyType.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(ProductFamilyType.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP));

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

        when(cachedServicesService.getInstanceInfoForService(ProductFamilyType.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(ProductFamilyType.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP));

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

        when(cachedServicesService.getInstanceInfoForService(ProductFamilyType.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(ProductFamilyType.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP));

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
        return new InstanceInfo(serviceId, null, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
                new InstanceInfo.PortWrapper(true, 9090), "https://localhost:9090/", null, null, null, "localhost", "localhost", 0, null,
                "localhost", status, null, null, null, null, null, null, null, null, null);
    }
}
