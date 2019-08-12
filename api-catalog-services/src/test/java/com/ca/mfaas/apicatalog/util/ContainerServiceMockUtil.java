/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.util;

import com.ca.mfaas.apicatalog.model.APIContainer;
import com.ca.mfaas.apicatalog.model.APIService;
import com.ca.mfaas.apicatalog.services.cached.CachedServicesService;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.mockito.Mockito.when;

public class ContainerServiceMockUtil {

    public ContainerServiceState createContainersServicesAndInstances() {
        ContainerServiceState containerServiceState = new ContainerServiceState();
        containerServiceState.setApplications(new ArrayList<>());
        containerServiceState.setServices(new ArrayList<>());
        containerServiceState.setInstances(new ArrayList<>());
        containerServiceState.setContainers(new ArrayList<>());

        List<APIService> services = new ArrayList<>();

        int index = 1000;

        index = generateInstancesAndServices(containerServiceState, services, "service1", index, 3);
        index = generateInstancesAndServices(containerServiceState, services, "service2", index, 1);

        // two containers same services
        APIContainer container = new APIContainer("api-one", "API One", "This is API One", new HashSet<>(services));
        APIContainer container1 = new APIContainer("api-two", "API Two", "This is API Two", new HashSet<>(services));

        // one extra service
        index = generateInstancesAndServices(containerServiceState, services, "service3", index, 1);

        APIContainer container2 = new APIContainer("api-three", "API Three", "This is API Three", new HashSet<>(services));

        // unique service
        services.clear();

        index = generateInstancesAndServices(containerServiceState, services, "service4", index, 1);

        APIContainer container4 = new APIContainer("api-four", "API Four", "This is API Four", new HashSet<>(services));

        containerServiceState.setContainers(Arrays.asList(container, container1, container2, container4));

        return containerServiceState;
    }

    public void mockServiceRetrievalFromCache(CachedServicesService cachedServicesService,
                                              List<Application> applications) {
        applications.forEach(application ->
            when(cachedServicesService.getService(application.getName())).thenReturn(application));
    }

    public InstanceInfo createInstance(String serviceId, String instanceId,
                                       InstanceInfo.InstanceStatus status,
                                       InstanceInfo.ActionType actionType,
                                       HashMap<String, String> metadata) {
        return new InstanceInfo(instanceId, serviceId.toUpperCase(), null, "192.168.0.1", null,
            new InstanceInfo.PortWrapper(true, 9090), null, null, null, null, null, null, null, 0, null, "hostname",
            status, null, null, null, null, metadata, null, null, actionType, null);
    }

    private int generateInstancesAndServices(ContainerServiceState containerServiceState,
                                             List<APIService> services,
                                             String serviceId,
                                             int index,
                                             int limit) {
        List<InstanceInfo> generatedInstances = Stream.iterate(index, i -> i + 1)
            .limit(limit)
            .map(mIndex -> {
                return getInstance(mIndex, serviceId);
            }).collect(Collectors.toList());


        addApiService(serviceId, containerServiceState.getServices(), services);
        addInstancesToApplications(
            generatedInstances,
            containerServiceState.getApplications(),
            containerServiceState.getInstances(),
            serviceId);

        return index + limit;
    }

    private void addInstancesToApplications(List<InstanceInfo> instanceCollection,
                                            List<Application> applications,
                                            List<InstanceInfo> instances,
                                            String serviceId) {
        instances.addAll(instanceCollection);
        applications.add(new Application(serviceId, instanceCollection));
    }


    private APIService addApiService(String serviceId,
                                     List<APIService> allServices,
                                     List<APIService> services) {
        APIService service = new APIService(
            serviceId,
            serviceId + "-title",
            serviceId + "-desc",
            false,
            "home");
        services.add(service);
        allServices.add(service);
        return service;
    }

    private InstanceInfo getInstance(int index, String serviceId) {
        InstanceInfo instance = createInstance(
            serviceId,
            serviceId + ":" + index,
            InstanceInfo.InstanceStatus.UP,
            InstanceInfo.ActionType.ADDED,
            new HashMap<>());
        return instance;
    }

}
