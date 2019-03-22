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
import com.ca.mfaas.apicatalog.gateway.GatewayConfigProperties;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import static org.mockito.Mockito.when;

@SuppressWarnings({"squid:S2925", "Duplicates"}) // replace with proper wait test library
@RunWith(MockitoJUnitRunner.Silent.class)
public class CachedProductFamilyTest {

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
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);

        service.getContainer("demoapp", instance);

        instance = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp2", instance);

        Collection<APIContainer> containers = service.getRecentlyUpdatedContainers();
        Assert.assertEquals(2, containers.size());
    }

    @Test
    public void testRetrievalOfRecentlyUpdatedContainersExcludeOldUpdate() throws InterruptedException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp", instance);

        Thread.sleep(4000);

        instance = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp2", instance);

        Collection<APIContainer> containers = service.getRecentlyUpdatedContainers();
        Assert.assertEquals(1, containers.size());
    }

    @Test
    public void testRetrievalOfRecentlyUpdatedContainersExcludeAll() throws InterruptedException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp", instance);

        instance = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp2", instance);

        Thread.sleep(3000);

        Collection<APIContainer> containers = service.getRecentlyUpdatedContainers();
        Assert.assertTrue(containers.isEmpty());
    }

    @Test
    public void testRetrievalOfContainerServices() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        InstanceInfo instance1 = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp", instance1);

        InstanceInfo instance2 = getStandardInstance("service2", InstanceInfo.InstanceStatus.UP, metadata);
        service.addServiceToContainer("demoapp", instance2);

        APIService containerService = service.getContainerService("demoapp", instance1);
        Assert.assertEquals("service1", containerService.getServiceId());

        containerService = service.getContainerService("demoapp", instance2);
        Assert.assertEquals("service2", containerService.getServiceId());
    }

    @Test(expected = NullPointerException.class)
    public void testCreationOfContainerWithoutInstance() {
        service.getContainer("demoapp", null);
        Assert.assertEquals(0, service.getContainerCount());
        Assert.assertEquals(0, service.getAllContainers().size());
    }

    @Test
    public void testGetMultipleContainersForASingleService() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        service.getContainer("demoapp1", instance);
        service.getContainer("demoapp2", instance);

        List<APIContainer> containersForService = service.getContainersForService("service1");
        Assert.assertEquals(2, containersForService.size());
        Assert.assertEquals(2, service.getContainerCount());
        Assert.assertEquals(2, service.getAllContainers().size());
    }

    @Test
    public void testCallCreationOfContainerThatAlreadyExistsButNothingHasChangedSoNoUpdate() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        InstanceInfo instance = getStandardInstance("service1", InstanceInfo.InstanceStatus.UP, metadata);
        APIContainer originalContainer = service.getContainer("demoapp", instance);
        Calendar createTimestamp = originalContainer.getLastUpdatedTimestamp();

        APIContainer updatedContainer = service.createContainerFromInstance("demoapp", instance);
        Calendar updatedTimestamp = updatedContainer.getLastUpdatedTimestamp();

        boolean equals = updatedTimestamp.equals(createTimestamp);
        Assert.assertTrue(equals);
    }

    @Test
    public void testCallCreationOfContainerThatAlreadyExistsInstanceInfoHasChangedSoUpdateLastChangeTime() throws InterruptedException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        metadata.put("mfaas.discovery.catalogUiTile.title", "Title");
        metadata.put("mfaas.discovery.catalogUiTile.description", "Description");
        metadata.put("mfaas.discovery.catalogUiTile.version", "1.0.0");
        InstanceInfo instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        APIContainer originalContainer = service.getContainer("demoapp", instance);
        Calendar createTimestamp = originalContainer.getLastUpdatedTimestamp();

        metadata.put("mfaas.discovery.catalogUiTile.title", "Title 2");
        metadata.put("mfaas.discovery.catalogUiTile.description", "Description 2");
        metadata.put("mfaas.discovery.catalogUiTile.version", "1.0.1");
        instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        Thread.sleep(100);

        APIContainer updatedContainer = service.createContainerFromInstance("demoapp", instance);
        Calendar updatedTimestamp = updatedContainer.getLastUpdatedTimestamp();

        boolean equals = updatedTimestamp.equals(createTimestamp);
        Assert.assertFalse(equals);

        metadata.put("mfaas.discovery.catalogUiTile.title", "Title 2");
        metadata.put("mfaas.discovery.catalogUiTile.description", "Description 2");
        metadata.put("mfaas.discovery.catalogUiTile.version", "2.0.0");
        instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        Thread.sleep(100);

        service.updateContainerFromInstance("demoapp", instance);
        Calendar retrievedTimestamp = updatedContainer.getLastUpdatedTimestamp();

        equals = updatedTimestamp.equals(retrievedTimestamp);
        Assert.assertFalse(equals);
    }

    @Test
    public void testCallCreationOfContainerForNullVersion() throws InterruptedException {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        metadata.put("mfaas.discovery.catalogUiTile.title", "Title");
        metadata.put("mfaas.discovery.catalogUiTile.description", "Description");
        metadata.put("mfaas.discovery.catalogUiTile.version", "1.0.0");
        InstanceInfo instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        APIContainer originalContainer = service.getContainer("demoapp", instance);
        Calendar createTimestamp = originalContainer.getLastUpdatedTimestamp();

        metadata.put("mfaas.discovery.catalogUiTile.title", "Title 2");
        metadata.put("mfaas.discovery.catalogUiTile.description", "Description 2");
        metadata.put("mfaas.discovery.catalogUiTile.version", "1.0.1");
        instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        Thread.sleep(100);

        APIContainer updatedContainer = service.createContainerFromInstance("demoapp", instance);
        Calendar updatedTimestamp = updatedContainer.getLastUpdatedTimestamp();

        boolean equals = updatedTimestamp.equals(createTimestamp);
        Assert.assertFalse(equals);

        metadata.put("mfaas.discovery.catalogUiTile.title", "Title 2");
        metadata.put("mfaas.discovery.catalogUiTile.description", "Description 2");
        metadata.put("mfaas.discovery.catalogUiTile.version", "2.0.0");
        instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        Thread.sleep(100);

        service.updateContainerFromInstance("demoapp", instance);
        Calendar retrievedTimestamp = updatedContainer.getLastUpdatedTimestamp();

        equals = updatedTimestamp.equals(retrievedTimestamp);
        Assert.assertFalse(equals);
    }

    @Test
    public void testUpdateOfContainerFromInstance() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
        InstanceInfo instance = getStandardInstance("service", InstanceInfo.InstanceStatus.UP, metadata);
        service.createContainerFromInstance("demoapp", instance);
    }

    @Test
    public void testCalculationOfContainerTotalsWithAllServicesUp() {
        HashMap<String, String> metadata = new HashMap<>();
        CachedServicesService cachedServicesService = Mockito.mock(CachedServicesService.class);

        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
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
        Assert.assertEquals(1, service.getContainerCount());

        APIContainer container = containersForService.get(0);
        service.calculateContainerServiceTotals(container);
        Assert.assertEquals("UP", container.getStatus());
        Assert.assertEquals(1, container.getTotalServices().intValue());
        Assert.assertEquals(1, container.getActiveServices().intValue());
    }

    @Test
    public void testCalculationOfContainerTotalsWithAllServicesDown() {
        HashMap<String, String> metadata = new HashMap<>();
        CachedServicesService cachedServicesService = Mockito.mock(CachedServicesService.class);

        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
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
        Assert.assertEquals("DOWN", container.getStatus());
        Assert.assertEquals(2, container.getTotalServices().intValue());
        Assert.assertEquals(0, container.getActiveServices().intValue());
    }

    @Test
    public void testCalculationOfContainerTotalsWithSomeServicesDown() {
        HashMap<String, String> metadata = new HashMap<>();
        CachedServicesService cachedServicesService = Mockito.mock(CachedServicesService.class);

        metadata.put("mfaas.discovery.catalogUiTile.id", "demoapp");
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
        Assert.assertEquals("WARNING", container.getStatus());
        Assert.assertEquals(2, container.getTotalServices().intValue());
        Assert.assertEquals(1, container.getActiveServices().intValue());
    }

    private GatewayConfigProperties getProperties() {
        return GatewayConfigProperties.builder()
            .scheme("https")
            .hostname("localhost:10010")
            .build();
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status,
            HashMap<String, String> metadata) {
        return new InstanceInfo(serviceId, serviceId.toUpperCase(), null, "192.168.0.1", null,
                new InstanceInfo.PortWrapper(true, 9090), null, null, null, null, null, null, null, 0, null, "hostname",
                status, null, null, null, null, metadata, null, null, null, null);
    }
}
