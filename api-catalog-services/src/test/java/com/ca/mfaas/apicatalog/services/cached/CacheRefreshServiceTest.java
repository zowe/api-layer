/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.cached;


import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.apicatalog.services.status.APIDocRetrievalService;
import com.ca.mfaas.apicatalog.services.status.APIServiceStatusService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.stream.Collectors;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.Silent.class)
public class CacheRefreshServiceTest {

    @InjectMocks
    private CacheRefreshService cacheRefreshService;

    @Mock
    private CachedProductFamilyService cachedProductFamilyService;

    @Mock
    private CachedServicesService cachedServicesService;

    @Mock
    private CachedApiDocService cachedApiDocService;

    @Mock
    private APIServiceStatusService apiServiceStatusService;

    @Mock
    private InstanceRetrievalService instanceRetrievalService;

    @Mock
    private APIDocRetrievalService apiDocRetrievalService;

    @Test
    public void testServiceAddedToDiscoveryThatIsNotInCache() {
        ContainerServiceState cachedState = createContainersServicesAndInstances();
        mockServiceRetrievalFromCache(cachedState.getApplications());

        ContainerServiceState discoveredState = new ContainerServiceState();
        discoveredState.setServices(new ArrayList<>());
        discoveredState.setContainers(new ArrayList<>());
        discoveredState.setInstances(new ArrayList<>());
        discoveredState.setApplications(new ArrayList<>());

        // start up a new instance of service 5 and add it to the service1 application
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "apifive");
        InstanceInfo newInstanceOfService5
            = createInstance("service5", "service5:9999", InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, metadata);
        discoveredState.instances.add(newInstanceOfService5);
        Application service5 = new Application("service5", Collections.singletonList(newInstanceOfService5));
        discoveredState.getApplications().add(service5);

        // Mock the discovery and cached service query
        Applications discoveredServices = new Applications("1", 1L, discoveredState.getApplications());

        when(instanceRetrievalService.extractDeltaFromDiscovery()).thenReturn(discoveredServices);
        Applications cachedServices = new Applications("1", 1L, cachedState.getApplications());
        when(apiServiceStatusService.getCachedApplicationState()).thenReturn(cachedServices);

        when(cachedProductFamilyService.createContainerFromInstance("apifive", newInstanceOfService5))
            .thenReturn(new APIContainer());

        cacheRefreshService.refreshCacheFromDiscovery();

        verify(cachedProductFamilyService, times(1))
            .createContainerFromInstance("apifive", newInstanceOfService5);
    }

    @Test
    public void testServiceRemovedFromDiscoveryThatIsInCache() {
        ContainerServiceState cachedState = createContainersServicesAndInstances();
        mockServiceRetrievalFromCache(cachedState.getApplications());


        // retrieve service 3 and update its instance status so it will be removed
        Application service3 = cachedState.applications.stream().filter(
            application -> application.getName().equalsIgnoreCase("service3")).collect(Collectors.toList()).get(0);

        ContainerServiceState discoveredState = new ContainerServiceState();
        discoveredState.setServices(new ArrayList<>());
        discoveredState.setContainers(new ArrayList<>());
        discoveredState.setInstances(new ArrayList<>());
        discoveredState.setApplications(new ArrayList<>());

        InstanceInfo shutDownInstanceOfService3 = service3.getInstances().get(0);
        shutDownInstanceOfService3.setActionType(InstanceInfo.ActionType.DELETED);
        service3.getInstances().add(0, shutDownInstanceOfService3);
        discoveredState.getApplications().add(service3);

        // Mock the discovery and cached service query
        Applications discoveredServices = new Applications("123", 2L, discoveredState.getApplications());
        when(instanceRetrievalService.extractDeltaFromDiscovery()).thenReturn(discoveredServices);
        Applications cachedServices = new Applications("456", 1L, cachedState.getApplications());
        when(apiServiceStatusService.getCachedApplicationState()).thenReturn(cachedServices);

        List<APIContainer> updatedContainers = new ArrayList<>();
        cachedState.containers.forEach(apiContainer -> {
            if (apiContainer.getId().equals("api-three")) {
                updatedContainers.add(apiContainer);
            }
        });


        when(cachedProductFamilyService.getContainersForService("SERVICE3")).thenReturn(updatedContainers);

        cacheRefreshService.refreshCacheFromDiscovery();

        verify(cachedServicesService, times(1)).updateService(anyString(), any(Application.class));
        verify(cachedProductFamilyService, times(1)).updateContainer(any(APIContainer.class));
        verify(cachedProductFamilyService, times(1)).updateContainer(updatedContainers.get(0));
    }

    @Test
    public void testModifiedServiceWithUpdatedApiDoc() {
        ContainerServiceState cachedState = createContainersServicesAndInstances();
        mockServiceRetrievalFromCache(cachedState.getApplications());


        // retrieve service 3 and update its instance status so it will be updated
        Application service1 = cachedState.applications.stream().filter(
            application -> application.getName().equalsIgnoreCase("service1")).collect(Collectors.toList()).get(0);

        ContainerServiceState discoveredState = new ContainerServiceState();
        discoveredState.setServices(new ArrayList<>());
        discoveredState.setContainers(new ArrayList<>());
        discoveredState.setInstances(new ArrayList<>());
        discoveredState.setApplications(new ArrayList<>());

        InstanceInfo shutDownInstanceOfService1 = service1.getInstances().get(0);
        shutDownInstanceOfService1.setActionType(InstanceInfo.ActionType.MODIFIED);
        service1.getInstances().add(0, shutDownInstanceOfService1);
        discoveredState.getApplications().add(service1);

        // Mock the discovery and cached service query
        Applications discoveredServices = new Applications("12323", 2L, discoveredState.getApplications());
        when(instanceRetrievalService.extractDeltaFromDiscovery()).thenReturn(discoveredServices);
        when(cachedApiDocService.getApiDocForService(any(), anyString())).thenReturn("API DOC CACHED");
        Applications cachedServices = new Applications("4512312316", 1L, cachedState.getApplications());
        when(apiServiceStatusService.getCachedApplicationState()).thenReturn(cachedServices);

        List<APIContainer> updatedContainers = new ArrayList<>();
        cachedState.containers.forEach(apiContainer -> {
            if (apiContainer.getId().equals("api-one")) {
                updatedContainers.add(apiContainer);
            }
        });


        when(cachedProductFamilyService.getContainersForService("SERVICE1")).thenReturn(updatedContainers);

        cacheRefreshService.refreshCacheFromDiscovery();

        verify(cachedServicesService, atLeastOnce()).updateService(anyString(), any(Application.class));
        verify(cachedProductFamilyService, atLeastOnce()).updateContainer(any(APIContainer.class));
        verify(cachedProductFamilyService, atLeastOnce()).updateContainer(updatedContainers.get(0));
    }

    // =========================================== Helper Methods ===========================================

    private ContainerServiceState createContainersServicesAndInstances() {
        ContainerServiceState containerServiceState = new ContainerServiceState();
        List<APIService> allServices = new ArrayList<>();
        List<APIService> services = new ArrayList<>();
        List<InstanceInfo> instances = new ArrayList<>();
        List<Application> applications = new ArrayList<>();

        int index = 1000;

        APIService service = new APIService("service1", "service-1", "service-1", false, "home");
        InstanceInfo instance = createInstance(service.getServiceId(), service.getServiceId() + ":" + index++, InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, new HashMap<>());
        instances.add(instance);

        instance = createInstance(service.getServiceId(), service.getServiceId() + ":" + index++, InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, new HashMap<>());
        instances.add(instance);
        instance = createInstance(service.getServiceId(), service.getServiceId() + ":" + index++, InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, new HashMap<>());
        services.add(service);
        instances.add(instance);
        allServices.add(service);
        applications.add(new Application(service.getServiceId(), new ArrayList<>(instances)));

        service = new APIService("service2", "service-2", "service-2", true, "home");
        instance = createInstance(service.getServiceId(), service.getServiceId() + ":" + index++, InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, new HashMap<>());
        instances.add(instance);

        services.add(service);
        allServices.add(service);
        applications.add(new Application(service.getServiceId(), Collections.singletonList(instance)));

        // two containers same services
        APIContainer container = new APIContainer("api-one", "API One", "This is API One", new HashSet<>(services));
        APIContainer container1 = new APIContainer("api-two", "API Two", "This is API Two", new HashSet<>(services));

        // one extra service
        service = new APIService("service3", "service-3", "service-3", false, "home");
        instance = createInstance(service.getServiceId(), service.getServiceId() + ":" + index++, InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, new HashMap<>());
        instances.add(instance);
        applications.add(new Application(service.getServiceId(), Collections.singletonList(instance)));

        services.add(service);
        allServices.add(service);

        APIContainer container2 = new APIContainer("api-three", "API Three", "This is API Three", new HashSet<>(services));

        // unique service
        services.clear();
        service = new APIService("service4", "service-4", "service-4", true, "home");
        instance = createInstance(service.getServiceId(), service.getServiceId() + ":" + index, InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED, new HashMap<>());
        services.add(service);
        allServices.add(service);
        applications.add(new Application(service.getServiceId(), Collections.singletonList(instance)));

        APIContainer container4 = new APIContainer("api-four", "API Four", "This is API Four", new HashSet<>(services));

        containerServiceState.setContainers(Arrays.asList(container, container1, container2, container4));
        containerServiceState.setServices(allServices);
        containerServiceState.setInstances(instances);
        containerServiceState.setApplications(applications);

        return containerServiceState;
    }

    private InstanceInfo createInstance(String serviceId, String instanceId, InstanceInfo.InstanceStatus status,
                                        InstanceInfo.ActionType actionType,
                                        HashMap<String, String> metadata) {
        return new InstanceInfo(instanceId, serviceId.toUpperCase(), null, "192.168.0.1", null,
            new InstanceInfo.PortWrapper(true, 9090), null, null, null, null, null, null, null, 0, null, "hostname",
            status, null, null, null, null, metadata, null, null, actionType, null);
    }


    private void mockServiceRetrievalFromCache(List<Application> applications) {
        applications.forEach(application ->
            when(cachedServicesService.getService(application.getName())).thenReturn(application));
    }


    private class ContainerServiceState {
        private List<APIContainer> containers;
        private List<APIService> services;
        private List<InstanceInfo> instances;
        private List<Application> applications;

        public ContainerServiceState() {
        }

        public List<APIContainer> getContainers() {
            return this.containers;
        }

        public void setContainers(List<APIContainer> containers) {
            this.containers = containers;
        }

        public List<APIService> getServices() {
            return this.services;
        }

        public void setServices(List<APIService> services) {
            this.services = services;
        }

        public List<InstanceInfo> getInstances() {
            return this.instances;
        }

        public void setInstances(List<InstanceInfo> instances) {
            this.instances = instances;
        }

        public List<Application> getApplications() {
            return this.applications;
        }

        public void setApplications(List<Application> applications) {
            this.applications = applications;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof com.ca.mfaas.apicatalog.services.cached.CacheRefreshServiceTest.ContainerServiceState))
                return false;
            final com.ca.mfaas.apicatalog.services.cached.CacheRefreshServiceTest.ContainerServiceState other = (com.ca.mfaas.apicatalog.services.cached.CacheRefreshServiceTest.ContainerServiceState) o;
            if (!other.canEqual((java.lang.Object) this)) return false;
            final java.lang.Object this$containers = this.containers;
            final java.lang.Object other$containers = other.containers;
            if (this$containers == null ? other$containers != null : !this$containers.equals(other$containers))
                return false;
            final java.lang.Object this$services = this.services;
            final java.lang.Object other$services = other.services;
            if (this$services == null ? other$services != null : !this$services.equals(other$services)) return false;
            final java.lang.Object this$instances = this.instances;
            final java.lang.Object other$instances = other.instances;
            if (this$instances == null ? other$instances != null : !this$instances.equals(other$instances))
                return false;
            final java.lang.Object this$applications = this.applications;
            final java.lang.Object other$applications = other.applications;
            if (this$applications == null ? other$applications != null : !this$applications.equals(other$applications))
                return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof com.ca.mfaas.apicatalog.services.cached.CacheRefreshServiceTest.ContainerServiceState;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final java.lang.Object $containers = this.containers;
            result = result * PRIME + ($containers == null ? 43 : $containers.hashCode());
            final java.lang.Object $services = this.services;
            result = result * PRIME + ($services == null ? 43 : $services.hashCode());
            final java.lang.Object $instances = this.instances;
            result = result * PRIME + ($instances == null ? 43 : $instances.hashCode());
            final java.lang.Object $applications = this.applications;
            result = result * PRIME + ($applications == null ? 43 : $applications.hashCode());
            return result;
        }

        public String toString() {
            return "CacheRefreshServiceTest.ContainerServiceState(containers=" + this.containers + ", services=" + this.services + ", instances=" + this.instances + ", applications=" + this.applications + ")";
        }
    }
}
