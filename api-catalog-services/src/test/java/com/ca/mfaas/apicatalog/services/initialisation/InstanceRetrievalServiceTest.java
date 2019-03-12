/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.apicatalog.services.initialisation;

import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.netflix.appinfo.InstanceInfo;

import static org.junit.Assert.assertEquals;

import org.junit.Ignore;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.retry.RetryException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;


import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {InstanceRetrievalServiceTest.TestConfiguration.class})
public class InstanceRetrievalServiceTest {
    @EnableConfigurationProperties(MFaaSConfigPropertiesContainer.class)
    public static class TestConfiguration {

    }
    @InjectMocks
    @Spy
    private InstanceRetrievalService instanceRetrievalService;

    @Mock
    CachedProductFamilyService cachedProductFamilyService;
    @Autowired
    MFaaSConfigPropertiesContainer propertiesContainer;
    @Mock
    CachedServicesService cachedServicesService;
    @Mock
    RestTemplate restTemplate;


private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status) {
    return new InstanceInfo(serviceId, null, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
        new InstanceInfo.PortWrapper(true, 9090), "https://localhost:9090/", null, null, null, "localhost", "localhost", 0, null,
        "localhost", status, null, null, null, null, null, null, null, null, null);
}

    @Ignore
    public void shouldChangeHomePageValue() throws RetryException, CannotRegisterServiceException {
        String serviceId = "gateway";
        HttpEntity<Object> httpEntity = getObjectHttpEntity();

        String apiDoc = "api doc goes here";
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(HttpStatus.OK);


        String url =  "http://localhost:10011/eureka/apps/gateway";

//        cachedServicesService = mock(CachedServicesService.class);
        cachedProductFamilyService = new CachedProductFamilyService(null, propertiesContainer);
        instanceRetrievalService = new InstanceRetrievalService(cachedProductFamilyService, propertiesContainer, cachedServicesService, restTemplate);
        when(instanceRetrievalService.getInstanceInfo(CoreService.GATEWAY.getServiceId()))
            .thenReturn(
                getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP));
//        doReturn(expectedResponse).when(restTemplate).exchange(
//            any(URI.class),
//            any(HttpMethod.class),
//            any(HttpEntity.class),
//            any(Class.class)
//        );
        when(restTemplate.exchange(url, HttpMethod.GET, httpEntity, String.class))
            .thenReturn(expectedResponse);
        instanceRetrievalService.retrieveAndRegisterAllInstancesWithCatalog();

        assertEquals(instanceRetrievalService.getInstanceInfo(serviceId).getHomePageUrl(), "hdh");
    }

    private HttpEntity<Object> getObjectHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        String basicToken = "Basic " + Base64.getEncoder().encodeToString(("eureka" + ":"
            + "password").getBytes());
        headers.add("Authorization", basicToken);
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON);
        headers.setAccept(types);
        return new HttpEntity<>(headers);
    }

}
