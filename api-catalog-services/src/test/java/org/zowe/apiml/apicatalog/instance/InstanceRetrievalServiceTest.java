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
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.io.entity.BasicHttpEntity;
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
import org.zowe.apiml.util.HttpClientMockHelper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static org.apache.hc.core5.http.ContentType.APPLICATION_JSON;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "/application.yml")
@ContextConfiguration(initializers = ConfigDataApplicationContextInitializer.class, classes = InstanceServicesContextConfiguration.class)
class InstanceRetrievalServiceTest {

    private static final String UNKNOWN = "unknown";

    private InstanceRetrievalService instanceRetrievalService;

    @Autowired
    private DiscoveryConfigProperties discoveryConfigProperties;

    @Mock
    private CloseableHttpClient httpClient;

    @Mock
    private CloseableHttpResponse response;

    @BeforeEach
    void setup() {
        HttpClientMockHelper.mockExecuteWithResponse(httpClient, response);
        HttpClientMockHelper.mockResponse(response, HttpStatus.SC_OK);

        instanceRetrievalService = new InstanceRetrievalService(discoveryConfigProperties, httpClient);
    }

    @Test
    void whenDiscoveryServiceIsNotAvailable_thenTryOthersFromTheList() throws IOException {
        when(response.getCode()).thenReturn(HttpStatus.SC_FORBIDDEN).thenReturn(HttpStatus.SC_OK);

        instanceRetrievalService.getAllInstancesFromDiscovery(false);
        verify(httpClient, times(2)).execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class));
    }

    @Test
    void testGetInstanceInfo_whenServiceIdIsUNKNOWN() {
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(UNKNOWN);
        assertNull(instanceInfo);
    }

    @Test
    void providedNoInstanceInfoIsReturned_thenInstanceInitializationExceptionIsThrown() {
        String serviceId = CoreService.API_CATALOG.getServiceId();
        when(response.getCode()).thenReturn(HttpStatus.SC_FORBIDDEN);

        assertThrows(InstanceInitializationException.class, () -> instanceRetrievalService.getInstanceInfo(serviceId));
    }

    @Test
    void testGetInstanceInfo_whenResponseHasEmptyBody() {
        HttpClientMockHelper.mockResponse(response, "");
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
        assertNull(instanceInfo);
    }

    @Test
    void testGetInstanceInfo_whenResponseCodeIsSuccessWithUnParsedJsonText() {
        HttpClientMockHelper.mockResponse(response, "UNPARSABLE_JSON");
        InstanceInfo instanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());
        assertNull(instanceInfo);
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
        BasicHttpEntity responseEntity = new BasicHttpEntity(IOUtils.toInputStream(bodyCatalog, StandardCharsets.UTF_8), APPLICATION_JSON);
        when(response.getEntity()).thenReturn(responseEntity);

        InstanceInfo actualInstanceInfo = instanceRetrievalService.getInstanceInfo(CoreService.API_CATALOG.getServiceId());

        assertNotNull(actualInstanceInfo);
        assertThat(actualInstanceInfo, hasProperty("instanceId", equalTo(expectedInstanceInfo.getInstanceId())));
        assertThat(actualInstanceInfo, hasProperty("appName", equalTo(expectedInstanceInfo.getAppName())));
        assertThat(actualInstanceInfo, hasProperty("status", equalTo(expectedInstanceInfo.getStatus())));
    }

    @Test
    void testGetAllInstancesFromDiscovery_whenResponseCodeIsNotSuccess() {
        when(response.getCode()).thenReturn(HttpStatus.SC_FORBIDDEN);

        Applications actualApplications = instanceRetrievalService.getAllInstancesFromDiscovery(false);
        assertNull(actualApplications);
    }

    @Test
    void testGetAllInstancesFromDiscovery_whenResponseCodeIsSuccessWithUnParsedJsonText() {
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
        BasicHttpEntity responseEntity = new BasicHttpEntity(IOUtils.toInputStream(bodyAll, StandardCharsets.UTF_8), APPLICATION_JSON);
        when(response.getEntity()).thenReturn(responseEntity);

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
        BasicHttpEntity responseEntity = new BasicHttpEntity(IOUtils.toInputStream(bodyAll, StandardCharsets.UTF_8), APPLICATION_JSON);
        when(response.getEntity()).thenReturn(responseEntity);

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

        instanceInfo = getStandardInstance(CoreService.ZAAS.getServiceId(), InstanceInfo.InstanceStatus.UP);
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
