/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway;

import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.client.ServiceInstance;
import org.zowe.apiml.registry.RegistryView;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Register the route to all components that need the information for the request to pass properly through the
 * Gateway. This class is heavily depended upon from the Stubs in this package.
 * <p>
 * Doubles as a {@link RegistryView} so that the components which read the registry in full -
 * {@code ServicesInfoService}, {@code BasicInfoService} - can be driven from the same mock services as the
 * route table. Previously those were fed Netflix {@code Applications} objects built here.
 */
public class ApplicationRegistry implements RegistryView {

    private final Map<String, MockService> instanceIdToService = Collections.synchronizedMap(new HashMap<>());

    public boolean remove(MockService mockService) {
        boolean removed = false;
        for (Iterator<Map.Entry<String, MockService>> i = instanceIdToService.entrySet().iterator(); i.hasNext(); ) {
            Map.Entry<String, MockService> entry = i.next();
            if (entry.getValue() == mockService) {
                i.remove();
                removed = true;
            }
        }
        return removed;
    }

    public boolean update(MockService mockService) {
        switch (mockService.getStatus()) {
            case STARTED:
            case ZOMBIE:
                if (instanceIdToService.get(mockService.getInstanceId()) == mockService) {
                    return false;
                }
                remove(mockService);
                instanceIdToService.put(mockService.getInstanceId(), mockService);
                return true;
            case STOPPED:
            case CANCELLING:
                return remove(mockService);
            default:
                throw new IllegalStateException("Unsupported status: " + mockService.getStatus());
        }
    }

    public Collection<MockService> getMockServices() {
        return instanceIdToService.values();
    }

    @Override
    public List<String> serviceIds() {
        return instanceIdToService.values().stream()
            .map(MockService::getServiceId)
            .distinct()
            .toList();
    }

    @Override
    public List<org.zowe.apiml.registry.model.ServiceInstance> instances(String serviceId) {
        return instanceIdToService.values().stream()
            .filter(i -> StringUtils.equalsIgnoreCase(serviceId, i.getServiceId()))
            .map(MockService::getInstanceInfo)
            .toList();
    }

    public List<org.zowe.apiml.registry.model.ServiceInstance> getInstances() {
        return instanceIdToService.values().stream()
            .map(MockService::getInstanceInfo)
            .toList();
    }

    public List<ServiceInstance> getServiceInstance(String serviceId) {
        return instanceIdToService.values().stream()
            .filter(ms -> StringUtils.equalsIgnoreCase(serviceId, ms.getServiceId()))
            .map(MockService::getServiceInstance)
            .collect(Collectors.toList());
    }

    public boolean afterTest() {
        boolean anyChange = false;
        for (Iterator<Map.Entry<String, MockService>> i = instanceIdToService.entrySet().iterator(); i.hasNext(); ) {
            MockService mockService = i.next().getValue();
            if (mockService.getScope() == MockService.Scope.TEST) {
                i.remove();
                mockService.close();
                anyChange = true;
            }
        }
        return anyChange;
    }

    public boolean afterClass() {
        boolean anyChange = !instanceIdToService.isEmpty();
        instanceIdToService.values().forEach(MockService::close);
        instanceIdToService.clear();
        return anyChange;
    }

}
