/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status;

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.ca.mfaas.apicatalog.services.cached.CachedApiDocService;
import com.ca.mfaas.apicatalog.services.cached.CachedProductFamilyService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.ca.mfaas.apicatalog.services.status.event.model.ContainerStatusChangeEvent;
import com.ca.mfaas.apicatalog.services.status.event.model.STATUS_EVENT_TYPE;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Applications;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class ApiServiceStatusServiceTest {

    @Mock
    private CachedProductFamilyService cachedProductFamilyService;

    @Mock
    private CachedServicesService cachedServicesService;

    @Mock
    private CachedApiDocService cachedApiDocService;

    @InjectMocks
    private APIServiceStatusService apiServiceStatusService;

    @Test
    public void getCachedApplicationState() {
        when(cachedServicesService.getAllCachedServices()).thenReturn(new Applications());
        ResponseEntity<Applications> responseEntity = apiServiceStatusService.getCachedApplicationStateResponse();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void addFirstContainer() {
        when(cachedServicesService.getAllCachedServices()).thenReturn(new Applications());
        ResponseEntity<Applications> responseEntity = apiServiceStatusService.getCachedApplicationStateResponse();
        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
    }

    @Test
    public void testGetContainerStatusAsEvents() {
        List<APIContainer> containers = new ArrayList<>(createContainers());
        when(cachedProductFamilyService.getAllContainers()).thenReturn(containers);
        doNothing().when(this.cachedProductFamilyService).calculateContainerServiceTotals(any(APIContainer.class));

        List<ContainerStatusChangeEvent> expectedEvents = new ArrayList<>();
        containers.forEach(container -> {
            STATUS_EVENT_TYPE eventType;
            if (InstanceInfo.InstanceStatus.DOWN.name().equalsIgnoreCase(container.getStatus())) {
                eventType = STATUS_EVENT_TYPE.CANCEL;
            } else {
                eventType = STATUS_EVENT_TYPE.RENEW;
            }
            expectedEvents.add(new ContainerStatusChangeEvent(
                container.getId(),
                container.getTitle(),
                container.getStatus(),
                container.getTotalServices(),
                container.getActiveServices(),
                container.getServices(),
                eventType)
            );
        });
        List<ContainerStatusChangeEvent> actualEvents = apiServiceStatusService.getContainersStateAsEvents();
        assertEquals(expectedEvents.size(), actualEvents.size());
    }

    @Test
    public void getCachedApiDocInfoForService() {
        String apiDoc = "this is the api doc";
        when(cachedApiDocService.getApiDocForService(anyString(), anyString())).thenReturn(apiDoc);
        ResponseEntity<String> expectedResponse = new ResponseEntity<>(apiDoc, HttpStatus.OK);
        ResponseEntity<String> actualResponse = apiServiceStatusService.getServiceCachedApiDocInfo("aaa", "v1");
        assertEquals(expectedResponse.getStatusCode(), actualResponse.getStatusCode());
        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
    }



    @Test
    public void testGetRecentlyChangedEvents() {
        List<APIContainer> containers = createContainers();
        when(cachedProductFamilyService.getRecentlyUpdatedContainers()).thenReturn(containers);
        doNothing().when(this.cachedProductFamilyService).calculateContainerServiceTotals(any(APIContainer.class));
        List<ContainerStatusChangeEvent> events = apiServiceStatusService.getRecentlyUpdatedContainersAsEvents();
        assertNotNull(events);
        assertEquals(2, events.size());
        assertEquals("API One", events.get(0).getTitle());
        assertEquals("API Two", events.get(1).getTitle());
    }

    private List<APIContainer> createContainers() {
        Set<APIService> services = new HashSet<>();

        APIService service = new APIService("service1", "service-1", "service-1", false, "home");
        services.add(service);

        service = new APIService("service2", "service-2", "service-2", true, "home");
        services.add(service);

        APIContainer container = new APIContainer("api-one", "API One", "This is API One", services);
        container.setTotalServices(2);
        container.setActiveServices(2);
        container.setStatus(InstanceInfo.InstanceStatus.UP.name());
        APIContainer container1 = new APIContainer("api-two", "API Two", "This is API Two", services);
        container1.setTotalServices(2);
        container1.setActiveServices(2);
        container.setStatus(InstanceInfo.InstanceStatus.DOWN.name());
        return Arrays.asList(container, container1);
    }

    private InstanceInfo getStandardInstance(String serviceId, InstanceInfo.InstanceStatus status,
            HashMap<String, String> metadata, String ipAddress, int port) {
        return new InstanceInfo(serviceId + ":" + port, serviceId.toUpperCase(), null, ipAddress, null,
                new InstanceInfo.PortWrapper(true, port), null, null, null, null, null, null, null, 0, null, "hostname",
                status, null, null, null, null, metadata, null, null, null, null);
    }
}
