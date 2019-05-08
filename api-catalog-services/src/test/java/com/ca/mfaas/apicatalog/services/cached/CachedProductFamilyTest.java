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
import com.ca.mfaas.product.gateway.GatewayConfigProperties;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

@SuppressWarnings({"squid:S2925"}) // replace with proper wait test library
@RunWith(MockitoJUnitRunner.Silent.class)
public class CachedProductFamilyTest {

    private static final String CATALOG_UI_ID_KEY = "mfaas.discovery.catalogUiTile.id";
    private static final String CATALOG_UI_TITLE_KEY = "mfaas.discovery.catalogUiTile.title";
    private static final String CATALOG_UI_DESCRIPTION_KEY = "mfaas.discovery.catalogUiTile.description";
    private static final String CATALOG_UI_VERSION_KEY = "mfaas.discovery.catalogUiTile.version";

    private static final String CATALOG_UI_SERVICE_TITLE_KEY = "mfaas.discovery.service.title";
    private static final String CATALOG_UI_SERVICE_DESCRIPTION_KEY = "mfaas.discovery.service.description";


    public Integer cacheRefreshUpdateThresholdInMillis = 2000;

    private CachedProductFamilyService service;

    @Before
    public void setup() {
        service = new CachedProductFamilyService(
            getProperties(),
            null,
            cacheRefreshUpdateThresholdInMillis);
    }

    @Test
    public void testRetrievalOfRecentlyUpdatedContainers() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);

        service.getContainer("demoapp", instance);

        instance = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp2", instance);

        Collection<APIContainer> containers = service.getRecentlyUpdatedContainers();
        assertEquals(2, containers.size());
    }

    @Test
    public void testRetrievalOfRecentlyUpdatedContainersExcludeOldUpdate() {
        HashMap<String, String> metadata = new HashMap<>();

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        APIContainer apiContainer = service.getContainer("demoapp", instance);
        updateContainerDateToBack(apiContainer, cacheRefreshUpdateThresholdInMillis);

        instance = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp2", instance);

        Collection<APIContainer> containers = service.getRecentlyUpdatedContainers();
        assertEquals(1, containers.size());
    }

    @Test
    public void testRetrievalOfRecentlyUpdatedContainersExcludeAll() {
        HashMap<String, String> metadata = new HashMap<>();

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        APIContainer apiContainer = service.getContainer("demoapp", instance);
        updateContainerDateToBack(apiContainer, cacheRefreshUpdateThresholdInMillis);

        instance = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        apiContainer = service.getContainer("demoapp2", instance);
        updateContainerDateToBack(apiContainer, cacheRefreshUpdateThresholdInMillis);


        Collection<APIContainer> containers = service.getRecentlyUpdatedContainers();
        Assert.assertTrue(containers.isEmpty());
    }

    @Test
    public void testRetrievalOfContainerServices() {
        HashMap<String, String> metadata = new HashMap<>();

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance1 = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp", instance1);

        InstanceInfo instance2 = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        service.addServiceToContainer("demoapp", instance2);

        APIService containerService = service.getContainerService("demoapp", instance1);
        assertEquals("service1", containerService.getServiceId());

        containerService = service.getContainerService("demoapp", instance2);
        assertEquals("service2", containerService.getServiceId());
    }

    @Test(expected = NullPointerException.class)
    public void testCreationOfContainerWithoutInstance() {
        service.getContainer("demoapp", null);
        assertEquals(0, service.getContainerCount());
        assertEquals(0, service.getAllContainers().size());
    }

    @Test
    public void testGetMultipleContainersForASingleService() {
        HashMap<String, String> metadata = new HashMap<>();

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp1", instance);
        service.getContainer("demoapp2", instance);

        List<APIContainer> containersForService = service.getContainersForService("service1");
        assertEquals(2, containersForService.size());
        assertEquals(2, service.getContainerCount());
        assertEquals(2, service.getAllContainers().size());
    }

    @Test
    public void testCallCreationOfContainerThatAlreadyExistsButNothingHasChangedSoNoUpdate() {
        HashMap<String, String> metadata = new HashMap<>();

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        APIContainer originalContainer = service.getContainer("demoapp", instance);
        Calendar createTimestamp = originalContainer.getLastUpdatedTimestamp();

        APIContainer updatedContainer = service.createContainerFromInstance("demoapp", instance);
        Calendar updatedTimestamp = updatedContainer.getLastUpdatedTimestamp();

        boolean equals = updatedTimestamp.equals(createTimestamp);
        Assert.assertTrue(equals);
    }

    @Test
    public void testCallCreationOfContainerThatAlreadyExistsInstanceInfoHasChanged() throws InterruptedException {
        HashMap<String, String> metadata = new HashMap<>();

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        metadata.put(CATALOG_UI_TITLE_KEY, "Title");
        metadata.put(CATALOG_UI_DESCRIPTION_KEY, "Description");
        metadata.put(CATALOG_UI_VERSION_KEY, "1.0.0");
        InstanceInfo instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp", instance);

        metadata.put(CATALOG_UI_TITLE_KEY, "Title 2");
        metadata.put(CATALOG_UI_DESCRIPTION_KEY, "Description 2");
        metadata.put(CATALOG_UI_VERSION_KEY, "1.0.1");
        instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);

        APIContainer updatedContainer = service.createContainerFromInstance("demoapp", instance);
        checkAPIContainer(updatedContainer, instance);

        metadata.put(CATALOG_UI_TITLE_KEY, "Title 3");
        metadata.put(CATALOG_UI_DESCRIPTION_KEY, "Description 3");
        metadata.put(CATALOG_UI_VERSION_KEY, "2.0.0");
        instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);

        service.updateContainerFromInstance("demoapp", instance);
        checkAPIContainer(updatedContainer, instance);
    }

    @Test
    public void testCalculationOfContainerTotalsWithAllServicesUp() {
        HashMap<String, String> metadata = new HashMap<>();
        CachedServicesService cachedServicesService = Mockito.mock(CachedServicesService.class);

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance1 = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        InstanceInfo instance2 = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        Application application = new Application();
        application.addInstance(instance1);
        application.addInstance(instance2);

        when(cachedServicesService.getService("service1")).thenReturn(application);
        service = new CachedProductFamilyService(
            getProperties(),
            cachedServicesService,
            cacheRefreshUpdateThresholdInMillis);

        service.getContainer("demoapp", instance1);
        service.addServiceToContainer("demoapp", instance2);

        List<APIContainer> containersForService = service.getContainersForService("service1");
        assertEquals(1, service.getContainerCount());

        APIContainer container = containersForService.get(0);
        service.calculateContainerServiceTotals(container);
        assertEquals("UP", container.getStatus());
        assertEquals(1, container.getTotalServices().intValue());
        assertEquals(1, container.getActiveServices().intValue());
    }

    @Test
    public void testCalculationOfContainerTotalsWithAllServicesDown() {
        HashMap<String, String> metadata = new HashMap<>();
        CachedServicesService cachedServicesService = Mockito.mock(CachedServicesService.class);

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance1 = getStandardInstance("service1", InstanceInfo.InstanceStatus.DOWN, metadata);
        InstanceInfo instance2 = getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN, metadata);
        Application application1 = new Application();
        application1.addInstance(instance1);
        Application application2 = new Application();
        application2.addInstance(instance2);

        when(cachedServicesService.getService("service1")).thenReturn(application1);
        when(cachedServicesService.getService("service2")).thenReturn(application2);
        service = new CachedProductFamilyService(
            getProperties(),
            cachedServicesService,
            cacheRefreshUpdateThresholdInMillis);

        service.getContainer("demoapp", instance1);
        service.addServiceToContainer("demoapp", instance2);

        APIContainer container = service.retrieveContainer("demoapp");
        Assert.assertNotNull(container);

        service.calculateContainerServiceTotals(container);
        assertEquals("DOWN", container.getStatus());
        assertEquals(2, container.getTotalServices().intValue());
        assertEquals(0, container.getActiveServices().intValue());
    }

    @Test
    public void testCalculationOfContainerTotalsWithSomeServicesDown() {
        HashMap<String, String> metadata = new HashMap<>();
        CachedServicesService cachedServicesService = Mockito.mock(CachedServicesService.class);

        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        InstanceInfo instance1 = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        InstanceInfo instance2 = getStandardInstance("service2", InstanceInfo.InstanceStatus.DOWN, metadata);
        Application application1 = new Application();
        application1.addInstance(instance1);
        Application application2 = new Application();
        application2.addInstance(instance2);

        when(cachedServicesService.getService("service1")).thenReturn(application1);
        when(cachedServicesService.getService("service2")).thenReturn(application2);
        service = new CachedProductFamilyService(
            getProperties(),
            cachedServicesService,
            cacheRefreshUpdateThresholdInMillis);

        service.getContainer("demoapp", instance1);
        service.addServiceToContainer("demoapp", instance2);

        APIContainer container = service.retrieveContainer("demoapp");
        Assert.assertNotNull(container);

        service.calculateContainerServiceTotals(container);
        assertEquals("WARNING", container.getStatus());
        assertEquals(2, container.getTotalServices().intValue());
        assertEquals(1, container.getActiveServices().intValue());
    }

    @Test
    public void givenInstanceIsNotInTheCache_whenCallSaveContainerFromInstance_thenCreateNew() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        metadata.put(CATALOG_UI_TITLE_KEY, "Title");
        metadata.put(CATALOG_UI_DESCRIPTION_KEY, "Description");
        metadata.put(CATALOG_UI_VERSION_KEY, "1.0.0");
        metadata.put(CATALOG_UI_SERVICE_TITLE_KEY, "sTitle");
        metadata.put(CATALOG_UI_SERVICE_DESCRIPTION_KEY, "sDescription");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);

        APIContainer actualDemoAppContainer = service.saveContainerFromInstance("demoapp", instance);

        List<APIContainer> lsContainer = service.getRecentlyUpdatedContainers();
        assertEquals(1, lsContainer.size());

        checkAPIContainer(actualDemoAppContainer, instance);
    }

    @Test
    public void givenInstanceIsInTheCache_whenCallSaveContainerFromInstance_thenUpdate() throws InterruptedException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put(CATALOG_UI_ID_KEY, "demoapp");
        metadata.put(CATALOG_UI_TITLE_KEY, "Title");
        metadata.put(CATALOG_UI_DESCRIPTION_KEY, "Description");
        metadata.put(CATALOG_UI_VERSION_KEY, "1.0.0");
        metadata.put(CATALOG_UI_SERVICE_TITLE_KEY, "sTitle");
        metadata.put(CATALOG_UI_SERVICE_DESCRIPTION_KEY, "sDescription");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        service.saveContainerFromInstance("demoapp", instance);

        metadata.put(CATALOG_UI_TITLE_KEY, "Title2");
        metadata.put(CATALOG_UI_DESCRIPTION_KEY, "Description2");
        metadata.put(CATALOG_UI_VERSION_KEY, "2.0.0");
        metadata.put(CATALOG_UI_SERVICE_TITLE_KEY, "sTitle2");
        metadata.put(CATALOG_UI_SERVICE_DESCRIPTION_KEY, "sDescription2");
        instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        APIContainer updatedContainer = service.saveContainerFromInstance("demoapp", instance);

        List<APIContainer> lsContainer = service.getRecentlyUpdatedContainers();
        assertEquals(1, lsContainer.size());

        checkAPIContainer(updatedContainer, instance);
    }

    private void updateContainerDateToBack(APIContainer apiContainer, int backMillis) {
        Calendar updatedDate = Calendar.getInstance();
        updatedDate.add(Calendar.MILLISECOND, -backMillis);
        apiContainer.setLastUpdatedTimestamp(updatedDate);
    }

    private void checkAPIContainer(APIContainer actualDemoAppContainer, InstanceInfo instance) {
        Map<String, String> metadata = instance.getMetadata();
        assertEquals(metadata.get(CATALOG_UI_ID_KEY), actualDemoAppContainer.getId());
        assertEquals(metadata.get(CATALOG_UI_TITLE_KEY), actualDemoAppContainer.getTitle());
        assertEquals(metadata.get(CATALOG_UI_DESCRIPTION_KEY), actualDemoAppContainer.getDescription());
        assertEquals(metadata.get(CATALOG_UI_VERSION_KEY), actualDemoAppContainer.getVersion());

        Set<APIService> apiServices = actualDemoAppContainer.getServices();
        assertEquals(1, apiServices.size());

        APIService actualService = apiServices.iterator().next();
        assertEquals(instance.getAppName().toLowerCase(), actualService.getServiceId());
        assertEquals(metadata.get(CATALOG_UI_SERVICE_TITLE_KEY), actualService.getTitle());
        assertEquals(metadata.get(CATALOG_UI_SERVICE_DESCRIPTION_KEY), actualService.getDescription());
        assertEquals(instance.isPortEnabled(InstanceInfo.PortType.SECURE), actualService.isSecured());
        assertEquals(instance.getHomePageUrl(), actualService.getHomePageUrl());
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }

    private InstanceInfo getStandardInstance(String serviceId,
                                             InstanceInfo.InstanceStatus status,
                                             HashMap<String, String> metadata) {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(serviceId)
            .setAppName(serviceId)
            .setStatus(status)
            .setMetadata(metadata)
            .build();
    }
}
