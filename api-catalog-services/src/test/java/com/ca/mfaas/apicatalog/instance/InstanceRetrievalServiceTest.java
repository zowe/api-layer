/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.apicatalog.instance;

import com.ca.mfaas.apicatalog.discovery.DiscoveryConfigProperties;
import com.ca.mfaas.apicatalog.util.ApplicationsWrapper;
import com.ca.mfaas.product.constants.CoreService;
import com.ca.mfaas.product.instance.InstanceInitializationException;
import com.ca.mfaas.product.registry.ApplicationWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.*;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.yml")
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@Import(InstanceRetrievalServiceTest.TestConfig.class)
public class InstanceRetrievalServiceTest {

    private static final String APPS_ENDPOINT = "apps/";
    private static final String DELTA_ENDPOINT = "delta";
    private static final String UNKNOWN = "unknown";

    private InstanceRetrievalService instanceRetrievalService;

    @Autowired
    private DiscoveryConfigProperties discoveryConfigProperties;

    @Mock
    RestTemplate restTemplate;

    private String discoveryServiceAllAppsUrl;


    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        instanceRetrievalService = new InstanceRetrievalService(discoveryConfigProperties, restTemplate);
        discoveryServiceAllAppsUrl = discoveryConfigProperties.getLocations() + APPS_ENDPOINT;
    }

    @Test
    public void testGetInstanceInfo_whenServiceIdIsUNKNOWN() {
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(UNKNOWN);
        assertNull(instanceInfo);
    }

    @Test
    public void testGetInstanceInfo_whenResponseCodeIsNotSuccess() {
        when(
            restTemplate.exchange(
                discoveryServiceAllAppsUrl + CoreService.API_CATALOG.getServiceId(),
                HttpMethod.GET,
                getHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.FORBIDDEN));

        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
        assertNull(instanceInfo);
    }

    @Test
    public void testGetInstanceInfo_whenResponseHasNullBody() {
        when(
            restTemplate.exchange(
                discoveryServiceAllAppsUrl + CoreService.API_CATALOG.getServiceId(),
                HttpMethod.GET,
                getHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.OK));

        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
        assertNull(instanceInfo);
    }

    @Test
    public void testGetInstanceInfo_whenResponseCodeIsSuccessWithUnParsedJsonText() {
        mockRetrieveApplicationService(
            discoveryServiceAllAppsUrl + CoreService.API_CATALOG.getServiceId(),
            "UNPARSED_JSON"
        );

        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
        assertNull(instanceInfo);
    }

    @Test(expected = InstanceInitializationException.class)
    public void testGetInstanceInfo_whenUnexpectedErrorHappened() {
        when(
            restTemplate.exchange(
                discoveryServiceAllAppsUrl + CoreService.API_CATALOG.getServiceId(),
                HttpMethod.GET,
                null,
                String.class
            )).thenReturn(new ResponseEntity<>("{}", HttpStatus.OK));

        instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
    }

    @Test
    public void testGetInstanceInfo() throws JsonProcessingException {
        InstanceInfo expectedInstanceInfo = getStandardInstance(
            CoreService.API_CATALOG.getServiceId(),
            InstanceInfo.InstanceStatus.UP
        );

        ObjectMapper mapper = new ObjectMapper();
        String bodyCatalog = mapper.writeValueAsString(
            new ApplicationWrapper(new Application(
                CoreService.API_CATALOG.getServiceId(),
                Collections.singletonList(expectedInstanceInfo)
            )));


        mockRetrieveApplicationService(
            discoveryServiceAllAppsUrl + CoreService.API_CATALOG.getServiceId(),
            bodyCatalog
        );

        InstanceInfo actualInstanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());

        assertNotNull(actualInstanceInfo);
        assertThat(actualInstanceInfo, hasProperty("instanceId", equalTo(expectedInstanceInfo.getInstanceId())));
        assertThat(actualInstanceInfo, hasProperty("appName", equalTo(expectedInstanceInfo.getAppName())));
        assertThat(actualInstanceInfo, hasProperty("status", equalTo(expectedInstanceInfo.getStatus())));
    }

    @Test
    public void testGetAllInstancesFromDiscovery_whenResponseCodeIsNotSuccess() {
        when(
            restTemplate.exchange(
                discoveryServiceAllAppsUrl,
                HttpMethod.GET,
                getHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<>(null, HttpStatus.FORBIDDEN));


        Applications actualApplications = instanceRetrievalService.getAllInstancesFromDiscovery(false);
        assertNull(actualApplications);
    }

    @Test
    public void testGetAllInstancesFromDiscovery_whenResponseCodeIsSuccessWithUnParsedJsonText() {
        mockRetrieveApplicationService(
            discoveryServiceAllAppsUrl,
            "UNPARSED_JSON"
        );


        Applications actualApplications = instanceRetrievalService.getAllInstancesFromDiscovery(false);
        assertNull(actualApplications);
    }

    @Test
    public void testGetAllInstancesFromDiscovery_whenNeedApplicationsWithoutFilter() throws JsonProcessingException {
        Map<String, InstanceInfo> instanceInfoMap = createInstances();


        Applications expectedApplications = new Applications();
        instanceInfoMap.forEach((key, value) -> expectedApplications.addApplication(new Application(value.getAppName(), Collections.singletonList(value))));

        ObjectMapper mapper = new ObjectMapper();
        String bodyAll = mapper.writeValueAsString(new ApplicationsWrapper(expectedApplications));
        mockRetrieveApplicationService(discoveryServiceAllAppsUrl, bodyAll);

        Applications actualApplications = instanceRetrievalService.getAllInstancesFromDiscovery(false);

        assertEquals(expectedApplications.size(), actualApplications.size());

        List<Application> actualApplicationList =
            new ArrayList<>(actualApplications.getRegisteredApplications());


        expectedApplications
            .getRegisteredApplications()
            .forEach(expectedApplication ->
                assertThat(actualApplicationList, hasItem(hasProperty("name", equalTo(expectedApplication.getName()))))
            );
    }

    @Test
    public void testGetAllInstancesFromDiscovery_whenNeedApplicationsWithDeltaFilter() throws JsonProcessingException {
        String discoveryServiceAppsUrl = discoveryConfigProperties.getLocations() + APPS_ENDPOINT + DELTA_ENDPOINT;

        Map<String, InstanceInfo> instanceInfoMap = createInstances();

        Applications expectedApplications = new Applications();
        instanceInfoMap.forEach((key, value) -> expectedApplications.addApplication(new Application(value.getAppName(), Collections.singletonList(value))));

        ObjectMapper mapper = new ObjectMapper();
        String bodyAll = mapper.writeValueAsString(new ApplicationsWrapper(expectedApplications));
        mockRetrieveApplicationService(discoveryServiceAppsUrl, bodyAll);

        Applications actualApplications = instanceRetrievalService.getAllInstancesFromDiscovery(true);

        assertEquals(expectedApplications.size(), actualApplications.size());

        List<Application> actualApplicationList =
            new ArrayList<>(actualApplications.getRegisteredApplications());


        expectedApplications
            .getRegisteredApplications()
            .forEach(expectedApplication ->
                assertThat(actualApplicationList, hasItem(hasProperty("name", equalTo(expectedApplication.getName()))))
            );
    }

    private Map<String, InstanceInfo> createInstances() {
        Map<String, InstanceInfo> instanceInfoMap = new HashMap<>();

        InstanceInfo instanceInfo = getStandardInstance(CoreService.GATEWAY.getServiceId(), InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance(CoreService.API_CATALOG.getServiceId(), InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);


        instanceInfo = getStandardInstance("STATICCLIENT", InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);


        instanceInfo = getStandardInstance("STATICCLIENT2", InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);


        instanceInfo = getStandardInstance("ZOSMFTSO21", InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance("ZOSMFCA32", InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        return instanceInfoMap;
    }

    private HttpEntity<?> getHttpEntity() {
        HttpHeaders headers = new HttpHeaders();
        String encodedCredentials = Base64.getEncoder().encodeToString(
            (discoveryConfigProperties.getEurekaUserName() + ":" + discoveryConfigProperties.getEurekaUserPassword()).getBytes()
        );

        headers.add("Authorization", "Basic " + encodedCredentials);
        headers.add("Content-Type", "application/json");
        headers.add("Accept", "application/json");
        List<MediaType> types = new ArrayList<>();
        types.add(MediaType.APPLICATION_JSON);
        headers.setAccept(types);
        return new HttpEntity<>(headers);
    }

    private InstanceInfo getStandardInstance(String serviceId,
                                             InstanceInfo.InstanceStatus status) {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(serviceId)
            .setAppName(serviceId)
            .setStatus(status)
            .build();
    }

    private void mockRetrieveApplicationService(String url, String body) {
        when(
            restTemplate.exchange(
                url,
                HttpMethod.GET,
                getHttpEntity(),
                String.class
            )).thenReturn(new ResponseEntity<>(body, HttpStatus.OK));
    }

    @Configuration
    public static class TestConfig {

        @Bean
        public DiscoveryConfigProperties discoveryConfigProperties() {
            return new DiscoveryConfigProperties();
        }

    }
}
