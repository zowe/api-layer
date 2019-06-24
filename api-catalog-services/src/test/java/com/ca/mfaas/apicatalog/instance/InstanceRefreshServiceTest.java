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


import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.apicatalog.util.ContainerServiceMockUtil;
import com.ca.mfaas.apicatalog.util.ContainerServiceState;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.stream.Collectors;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class InstanceRefreshServiceTest {

    private final ContainerServiceMockUtil containerServiceMockUtil = new ContainerServiceMockUtil();

    @InjectMocks
    private InstanceRefreshService instanceRefreshService;

    @Mock
    private CachedProductFamilyService cachedProductFamilyService;

    @Mock
    private CachedServicesService cachedServicesService;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Test
    public void testServiceAddedToDiscoveryThatIsNotInCache() {
        ContainerServiceState cachedState = containerServiceMockUtil.createContainersServicesAndInstances();
        containerServiceMockUtil.mockServiceRetrievalFromCache(cachedServicesService, cachedState.getApplications());

        ContainerServiceState discoveredState = new ContainerServiceState();
        discoveredState.setServices(new ArrayList<>());
        discoveredState.setContainers(new ArrayList<>());
        discoveredState.setInstances(new ArrayList<>());
        discoveredState.setApplications(new ArrayList<>());

        // start up a new instance of service 5 and add it to the service1 application
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "api-five");
        InstanceInfo newInstanceOfService5
            = containerServiceMockUtil.createInstance("service5", "service5:9999", InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, metadata);
        discoveredState.getInstances().add(newInstanceOfService5);
        Application service5 = new Application("service5", Collections.singletonList(newInstanceOfService5));
        discoveredState.getApplications().add(service5);

        // Mock the discovery and cached service query
        Applications discoveredServices = new Applications("1", 1L, discoveredState.getApplications());
        when(instanceRetrievalService.getAllInstancesFromDiscovery(true)).thenReturn(discoveredServices);

        Applications cachedServices = new Applications("1", 1L, cachedState.getApplications());
        when(cachedServicesService.getAllCachedServices()).thenReturn(cachedServices);

        when(cachedProductFamilyService.saveContainerFromInstance("api-five", newInstanceOfService5))
            .thenReturn(new APIContainer());

        instanceRefreshService.refreshCacheFromDiscovery();

        verify(cachedProductFamilyService, times(1))
            .saveContainerFromInstance("api-five", newInstanceOfService5);
    }

    @Test
    public void testServiceRemovedFromDiscoveryThatIsInCache() {
        ContainerServiceState cachedState = containerServiceMockUtil.createContainersServicesAndInstances();
        containerServiceMockUtil.mockServiceRetrievalFromCache(cachedServicesService, cachedState.getApplications());

        // retrieve service 3 and update its instance status so it will be removed
        Application service3 = cachedState.getApplications()
            .stream()
            .filter(application -> application.getName().equalsIgnoreCase("service3"))
            .collect(Collectors.toList()).get(0);

        ContainerServiceState discoveredState = new ContainerServiceState();
        discoveredState.setServices(new ArrayList<>());
        discoveredState.setContainers(new ArrayList<>());
        discoveredState.setInstances(new ArrayList<>());
        discoveredState.setApplications(new ArrayList<>());

        InstanceInfo shutDownInstanceOfService3 = service3.getInstances().get(0);
        shutDownInstanceOfService3.getMetadata().put("mfaas.discovery.catalogUiTile.id", "api-three");
        shutDownInstanceOfService3.setActionType(InstanceInfo.ActionType.DELETED);
        service3.getInstances().add(0, shutDownInstanceOfService3);
        discoveredState.getApplications().add(service3);

        // Mock the discovery and cached service query
        Applications discoveredServices = new Applications("123", 2L, discoveredState.getApplications());
        when(instanceRetrievalService.getAllInstancesFromDiscovery(true)).thenReturn(discoveredServices);
        Applications cachedServices = new Applications("456", 1L, cachedState.getApplications());
        when(cachedServicesService.getAllCachedServices()).thenReturn(cachedServices);


        instanceRefreshService.refreshCacheFromDiscovery();

        verify(cachedServicesService, times(1)).updateService(anyString(), any(Application.class));
        verify(cachedProductFamilyService, never()).saveContainerFromInstance("api-three", shutDownInstanceOfService3);
    }

    @Test
    public void testServiceModifiedFromDiscoveryThatIsInCache() {
        ContainerServiceState cachedState = containerServiceMockUtil.createContainersServicesAndInstances();
        containerServiceMockUtil.mockServiceRetrievalFromCache(cachedServicesService, cachedState.getApplications());

        // retrieve service 3 and update its instance status so it will be updated
        Application service3 = cachedState.getApplications()
            .stream()
            .filter(application -> application.getName().equalsIgnoreCase("service3"))
            .collect(Collectors.toList()).get(0);

        ContainerServiceState discoveredState = new ContainerServiceState();
        discoveredState.setServices(new ArrayList<>());
        discoveredState.setContainers(new ArrayList<>());
        discoveredState.setInstances(new ArrayList<>());
        discoveredState.setApplications(new ArrayList<>());

        InstanceInfo modifiedInstanceOfService3 = service3.getInstances().get(0);
        modifiedInstanceOfService3.getMetadata().put("mfaas.discovery.catalogUiTile.id", "api-three");
        modifiedInstanceOfService3.setActionType(InstanceInfo.ActionType.MODIFIED);
        service3.getInstances().add(0, modifiedInstanceOfService3);
        discoveredState.getApplications().add(service3);

        // Mock the discovery and cached service query
        Applications discoveredServices = new Applications("12323", 2L, discoveredState.getApplications());
        when(instanceRetrievalService.getAllInstancesFromDiscovery(true)).thenReturn(discoveredServices);
        Applications cachedServices = new Applications("4512312316", 1L, cachedState.getApplications());
        when(cachedServicesService.getAllCachedServices()).thenReturn(cachedServices);

        APIContainer apiContainer3 = cachedState.getContainers()
            .stream()
            .filter(apiContainer -> apiContainer.getId().equals("api-three"))
            .findFirst()
            .orElse(new APIContainer());

        when(cachedProductFamilyService.saveContainerFromInstance("api-three", modifiedInstanceOfService3))
            .thenReturn(apiContainer3);

        instanceRefreshService.refreshCacheFromDiscovery();

        verify(cachedServicesService, times(1)).updateService(modifiedInstanceOfService3.getAppName(), service3);
        verify(cachedProductFamilyService, times(1))
            .saveContainerFromInstance("api-three", modifiedInstanceOfService3);
    }
}
