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

import com.ca.mfaas.apicatalog.model.APIContainer;
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
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
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

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        instanceRetrievalService = new InstanceRetrievalService(cachedProductFamilyService, propertiesContainer, cachedServicesService, restTemplate);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status, HashMap<String, String> metadata, String vipAddress, String homePageUrl) {
        return new InstanceInfo(null, serviceId, null, "192.168.0.1", null, new InstanceInfo.PortWrapper(true, 9090),
            new InstanceInfo.PortWrapper(true, 9090), homePageUrl, null, null, null, vipAddress, "localhost", 0, null,
            "localhost", status, null, null, null, null, metadata, new Date().getTime(), null, null, null);
    }

    @Test
    public void shouldChangeHomePageValue() throws RetryException, CannotRegisterServiceException, JsonProcessingException {
        String discoveryServiceLocatorUrl = propertiesContainer.getDiscovery().getLocations() + "apps";
        assertEquals(discoveryServiceLocatorUrl, "http://localhost:10011/eureka/apps");

        assertNotNull(instanceRetrievalService);

        ObjectMapper mapper = new ObjectMapper();

        InstanceInfo gatewayInstance = getStandardInstance(
            CoreService.GATEWAY.getServiceId(),
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("apimediationlayer"),
            "gateway",
            "https://localhost:9090/");

        String bodyGateway = mapper.writeValueAsString(new ApplicationWrapper(new Application(CoreService.GATEWAY.getServiceId(), Collections.singletonList(gatewayInstance))));

        InstanceInfo apiCatalogInstance = getStandardInstance(
            CoreService.API_CATALOG.getServiceId(),
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("apimediationlayer"),
            "apicatalog",
            "https://localhost:9090/");

        String bodyCatalog = mapper.writeValueAsString(new ApplicationWrapper(new Application(CoreService.API_CATALOG.getServiceId(), Collections.singletonList(apiCatalogInstance))));

        InstanceInfo staticClientInstance = getStandardInstance(
            "STATICCLIENT",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("static"),
            "staticclient",
            "https://localhost:9090/");

        InstanceInfo staticClientTwoInstance = getStandardInstance(
            "STATICCLIENT2",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("static"),
            "staticclient2",
            null);

        InstanceInfo zosmftsoInstance = getStandardInstance(
            "ZOSMFTSO21",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf"),
            "zosmftso21",
            null);

        InstanceInfo zosmfcaInstance = getStandardInstance(
            "ZOSMFCA32",
            InstanceInfo.InstanceStatus.UP,
            getMetadataByCatalogUiTitleId("zosmf"),
            "zosmfca32",
            null);

        Applications applications = new Applications();
        applications.addApplication(new Application(CoreService.GATEWAY.getServiceId(), Collections.singletonList(gatewayInstance)));
        applications.addApplication(new Application(CoreService.API_CATALOG.getServiceId(), Collections.singletonList(apiCatalogInstance)));
        applications.addApplication(new Application("STATICCLIENT", Collections.singletonList(staticClientInstance)));
        applications.addApplication(new Application("STATICCLIENT2", Collections.singletonList(staticClientTwoInstance)));
        applications.addApplication(new Application("ZOSMFTSO21", Collections.singletonList(zosmftsoInstance)));
        applications.addApplication(new Application("ZOSMFCA32", Collections.singletonList(zosmfcaInstance)));

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


        Collection<APIContainer> apiContainers = cachedProductFamilyService.getAllContainers();
        apiContainers.stream()
            .filter(f -> f.getId().equals("static"))
            .flatMap(fm -> fm.getServices().stream())
            .filter(f -> f.getServiceId().equals("staticclient"))
            .findFirst()
            .ifPresent(ip -> {
               Assert.assertEquals("https://localhost:9090/ui/v1/staticclient", ip.getHomePageUrl());
            });

    }

    @Test
    public void shouldFailToLocateInstanceForRequest() throws CannotRegisterServiceException {

        when(
            restTemplate.exchange(
                null,
                HttpMethod.GET,
                null,
                String.class
            )).thenReturn(new ResponseEntity<>(HttpStatus.OK));
        exception.expectMessage("An error occurred when trying to get instance info for:  apicatalog");
        instanceRetrievalService.retrieveAndRegisterAllInstancesWithCatalog();
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
