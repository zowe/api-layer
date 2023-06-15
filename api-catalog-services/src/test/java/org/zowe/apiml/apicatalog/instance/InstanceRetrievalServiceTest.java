/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.instance;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.BasicHttpEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.ConfigDataApplicationContextInitializer;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;
import org.zowe.apiml.apicatalog.util.ApplicationsWrapper;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.registry.ApplicationWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "/application.yml")
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = InstanceServicesContextConfiguration.class)
class InstanceRetrievalServiceTest {

    private static final String APPS_ENDPOINT = "apps/";
    private static final String DELTA_ENDPOINT = "delta";
    private static final String UNKNOWN = "unknown";

    private InstanceRetrievalService instanceRetrievalService;

    @Autowired
    private DiscoveryConfigProperties discoveryConfigProperties;

    @Mock
    CloseableHttpClient httpClient;

    private CloseableHttpResponse response;
    private StatusLine responseStatusLine;
    private BasicHttpEntity responseEntity;
    private String discoveryServiceAllAppsUrl;
    private String[] discoveryServiceList;

    @BeforeEach
    void setup() throws IOException {
        response = mock(CloseableHttpResponse.class);
        responseStatusLine = mock(StatusLine.class);
        responseEntity = new BasicHttpEntity();
        responseEntity.setContent(IOUtils.toInputStream("", StandardCharsets.UTF_8));
        when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        when(response.getStatusLine()).thenReturn(responseStatusLine);
        when(response.getEntity()).thenReturn(responseEntity);
        when(httpClient.execute(any())).thenReturn(response);
        instanceRetrievalService = new InstanceRetrievalService(discoveryConfigProperties, httpClient);
        discoveryServiceList = discoveryConfigProperties.getLocations();
        discoveryServiceAllAppsUrl = discoveryServiceList[0] + APPS_ENDPOINT;
    }

    @Test
    void whenDiscoveryServiceIsNotAvailable_thenTryOthersFromTheList() throws IOException {
        when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN).thenReturn(HttpStatus.SC_OK);

        instanceRetrievalService.getAllInstancesFromDiscovery(false);
        verify(httpClient, times(2)).execute(any());
    }

    @Test
    void testGetInstanceInfo_whenServiceIdIsUNKNOWN() {
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(UNKNOWN);
        assertNull(instanceInfo);
    }

    @Test
    void providedNoInstanceInfoIsReturned_thenInstanceInitializationExceptionIsThrown() {
        String serviceId = CoreService.API_CATALOG.getServiceId();
        when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);

        assertThrows(InstanceInitializationException.class, () -> instanceRetrievalService.getInstanceInfo(serviceId));

    }

    @Test
    void testGetInstanceInfo_whenResponseHasEmptyBody() {
        when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);
        responseEntity.setContent(IOUtils.toInputStream("", StandardCharsets.UTF_8));

        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
        assertNull(instanceInfo);
    }

    @Test
    void testGetInstanceInfo_whenResponseCodeIsSuccessWithUnParsedJsonText() {
        responseEntity.setContent(IOUtils.toInputStream("UNPARSED_JSON", StandardCharsets.UTF_8));

        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
        assertNull(instanceInfo);
    }

    @Test
    void testGetInstanceInfo_whenUnexpectedErrorHappened() {
        responseEntity.setContent(null);

        String serviceId = CoreService.API_CATALOG.getServiceId();
        assertThrows(InstanceInitializationException.class, () -> {
            instanceRetrievalService.getInstanceInfo(serviceId);
        });
    }

    @Test
    void testGetInstanceInfo() throws IOException {
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
        responseEntity.setContent(IOUtils.toInputStream(bodyCatalog, StandardCharsets.UTF_8));

        InstanceInfo actualInstanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());

        assertNotNull(actualInstanceInfo);
        assertThat(actualInstanceInfo, hasProperty("instanceId", equalTo(expectedInstanceInfo.getInstanceId())));
        assertThat(actualInstanceInfo, hasProperty("appName", equalTo(expectedInstanceInfo.getAppName())));
        assertThat(actualInstanceInfo, hasProperty("status", equalTo(expectedInstanceInfo.getStatus())));
    }

    @Test
    void testGetAllInstancesFromDiscovery_whenResponseCodeIsNotSuccess() {
        when(responseStatusLine.getStatusCode()).thenReturn(HttpStatus.SC_FORBIDDEN);

        Applications actualApplications = instanceRetrievalService.getAllInstancesFromDiscovery(false);
        assertNull(actualApplications);
    }

    @Test
    void testGetAllInstancesFromDiscovery_whenResponseCodeIsSuccessWithUnParsedJsonText() throws IOException {
        responseEntity.setContent(IOUtils.toInputStream("UNPARSED_JSON", StandardCharsets.UTF_8));

        Applications actualApplications = instanceRetrievalService.getAllInstancesFromDiscovery(false);
        assertNull(actualApplications);
    }

    @Test
    void testGetAllInstancesFromDiscovery_whenNeedApplicationsWithoutFilter() throws IOException {
        Map<String, InstanceInfo> instanceInfoMap = createInstances();


        Applications expectedApplications = new Applications();
        instanceInfoMap.forEach((key, value) -> expectedApplications.addApplication(new Application(value.getAppName(), Collections.singletonList(value))));

        ObjectMapper mapper = new ObjectMapper();
        String bodyAll = mapper.writeValueAsString(new ApplicationsWrapper(expectedApplications));
        responseEntity.setContent(IOUtils.toInputStream(bodyAll, StandardCharsets.UTF_8));

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
    void testGetAllInstancesFromDiscovery_whenNeedApplicationsWithDeltaFilter() throws IOException {
        Map<String, InstanceInfo> instanceInfoMap = createInstances();

        Applications expectedApplications = new Applications();
        instanceInfoMap.forEach((key, value) -> expectedApplications.addApplication(new Application(value.getAppName(), Collections.singletonList(value))));

        ObjectMapper mapper = new ObjectMapper();
        String bodyAll = mapper.writeValueAsString(new ApplicationsWrapper(expectedApplications));
        responseEntity.setContent(IOUtils.toInputStream(bodyAll, StandardCharsets.UTF_8));

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


        instanceInfo = getStandardInstance("ZOSMF1", InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        instanceInfo = getStandardInstance("ZOSMF2", InstanceInfo.InstanceStatus.UP);
        instanceInfoMap.put(instanceInfo.getAppName(), instanceInfo);

        return instanceInfoMap;
    }

    private InstanceInfo getStandardInstance(String serviceId,
                                             InstanceInfo.InstanceStatus status) {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(serviceId)
            .setAppName(serviceId)
            .setStatus(status)
            .build();
    }
}
