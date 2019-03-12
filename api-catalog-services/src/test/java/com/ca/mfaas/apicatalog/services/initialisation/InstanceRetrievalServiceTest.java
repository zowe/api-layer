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
import com.ca.mfaas.apicatalog.util.ApplicationsWrapper;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.registry.ApplicationWrapper;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;

import static org.junit.Assert.assertEquals;


import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.*;
import org.springframework.retry.RetryException;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;


import java.util.*;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {InstanceRetrievalServiceTest.TestConfiguration.class})
public class InstanceRetrievalServiceTest {
    @EnableConfigurationProperties(MFaaSConfigPropertiesContainer.class)
    public static class TestConfiguration {

    }

    private InstanceRetrievalService instanceRetrievalService;

    @Spy
    CachedProductFamilyService cachedProductFamilyService;

    @Autowired
    MFaaSConfigPropertiesContainer propertiesContainer;

    @Mock
    CachedServicesService cachedServicesService;

    @Mock
    RestTemplate restTemplate;

    @Before
    public void setup() {
        instanceRetrievalService = new InstanceRetrievalService(cachedProductFamilyService, propertiesContainer, cachedServicesService, restTemplate);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status, HashMap<String, String> metadata) {
        return new InstanceInfo(null, serviceId, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
            new InstanceInfo.PortWrapper(true, 9090), "https://localhost:9090/", null, null, null, "localhost", "localhost", 0, null,
            "localhost", status, null, null, null, null, metadata, new Date().getTime(), null, null, null);
    }

    @Test
    public void shouldChangeHomePageValue() throws RetryException, CannotRegisterServiceException, JsonProcessingException {
        String discoveryServiceLocatorUrl = propertiesContainer.getDiscovery().getLocations() + "apps";
        assertEquals(discoveryServiceLocatorUrl, "http://localhost:10011/eureka/apps");

        assertNotNull(instanceRetrievalService);

        ObjectMapper mapper = new ObjectMapper();

        InstanceInfo GATEWAY_INSTANCE = getStandardInstance(
            CoreService.GATEWAY.getServiceId(),
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("apimediationlayer"));

        String bodyGateway = mapper.writeValueAsString(new ApplicationWrapper(new Application(CoreService.GATEWAY.getServiceId(), Collections.singletonList(GATEWAY_INSTANCE))));

        InstanceInfo API_CATALOG_INSTANCE = getStandardInstance(
            CoreService.API_CATALOG.getServiceId(),
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("apimediationlayer"));

        String bodyCatalog = mapper.writeValueAsString(new ApplicationWrapper(new Application(CoreService.API_CATALOG.getServiceId(), Collections.singletonList(API_CATALOG_INSTANCE))));

        InstanceInfo STATICCLIENT_INSTANCE = getStandardInstance(
            "STATICCLIENT",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("static"));

        InstanceInfo STATICCLIENT2_INSTANCE = getStandardInstance(
            "STATICCLIENT2",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("static"));

        InstanceInfo ZOSMFTSO21_INSTANCE = getStandardInstance(
            "ZOSMFTSO21",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf"));

        InstanceInfo ZOSMFCA32_INSTANCE = getStandardInstance(
            "ZOSMFCA32",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf"));

        Applications applications = new Applications();
        applications.addApplication(new Application(CoreService.GATEWAY.getServiceId(), Collections.singletonList(GATEWAY_INSTANCE)));
        applications.addApplication(new Application(CoreService.API_CATALOG.getServiceId(), Collections.singletonList(API_CATALOG_INSTANCE)));
        applications.addApplication(new Application("STATICCLIENT", Collections.singletonList(STATICCLIENT_INSTANCE)));
        applications.addApplication(new Application("STATICCLIENT2", Collections.singletonList(STATICCLIENT2_INSTANCE)));
        applications.addApplication(new Application("ZOSMFTSO21", Collections.singletonList(ZOSMFTSO21_INSTANCE)));
        applications.addApplication(new Application("ZOSMFCA32", Collections.singletonList(ZOSMFCA32_INSTANCE)));

        String bodyAll = mapper.writeValueAsString(new ApplicationsWrapper(applications));

        when(
            restTemplate.exchange(
                discoveryServiceLocatorUrl + "/" + CoreService.API_CATALOG.getServiceId(),
                HttpMethod.GET,
                getHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<>(bodyCatalog, HttpStatus.OK));

        when(
            restTemplate.exchange(
                discoveryServiceLocatorUrl + "/" + CoreService.GATEWAY.getServiceId(),
                HttpMethod.GET,
                getHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<>(bodyGateway, HttpStatus.OK));


        when(
            restTemplate.exchange(
                discoveryServiceLocatorUrl + "/",
                HttpMethod.GET,
                getHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<>(bodyAll, HttpStatus.OK));


        instanceRetrievalService.retrieveAndRegisterAllInstancesWithCatalog();


        cachedProductFamilyService.getAllContainers().forEach(f -> {
            System.out.println(f.getId());
            f.getServices().forEach(f1 -> {
                System.out.print("-" + f1.getServiceId() + "-");
                System.out.println("-" + f1.getHomePageUrl() + "-");
            });

        });
    }

    private HashMap<String, String> getMetadataByCatalogUiTitleId(String catalogUiTileId) {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", catalogUiTileId);
        return metadata;
    }

    private HttpEntity<?> getHttpEntity() {
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
