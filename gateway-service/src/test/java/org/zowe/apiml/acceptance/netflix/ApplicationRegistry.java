/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.acceptance.netflix;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;
import org.zowe.apiml.product.routing.RoutedServicesUser;

import java.util.*;

/**
 * Register the route to all components that need the information for the request to pass properly through the
 * Gateway. This class is heavily depended upon from the Stubs in this package.
 */
public class ApplicationRegistry {
    private List<RoutedServicesUser> routedServicesUsers;

    private String currentApplication;
    private Map<String, Applications> applicationsToReturn = new HashMap<>();
    private LinkedHashMap<String, ZuulProperties.ZuulRoute> zuulRouteLinkedHashMap = new LinkedHashMap<>();

    private List<Services> servicesToAdd = new ArrayList<>();

    public ApplicationRegistry() {}

    /**
     * Add new route to a service.
     *
     * @param id Id of the service
     * @param locationPattern Pattern of the path solved by the service
     * @param serviceRoute Service route path extracted from the URL
     * @param customMetadata Whether the custom metadata should be provided for given service.
     */
    public void addApplication(String id, String locationPattern, String serviceRoute, boolean customMetadata) {
        Applications applications = new Applications();
        Application withMetadata = new Application(id);

        Map<String, String> metadata = new HashMap<>();
        if (customMetadata) {
            metadata = createMetadata();
        }
        withMetadata.addInstance(getStandardInstance(metadata, id));
        applications.addApplication(withMetadata);

        ZuulProperties.ZuulRoute route = new ZuulProperties.ZuulRoute(id, locationPattern);
        route.setUrl(id);
        zuulRouteLinkedHashMap.put(id, route);

        applicationsToReturn.put(id, applications);

        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(new RoutedService("test", serviceRoute, ""));
        if (this.routedServicesUsers != null) {
            for (RoutedServicesUser routedServicesUser : routedServicesUsers) {
                routedServicesUser.addRoutedServices(id, routedServices);
            }
        } else {
            servicesToAdd.add(new Services(id, routedServices));
        }
    }

    /**
     * Remove all applications from internal mappings. This needs to be followed by adding new ones in order for the
     * discovery infrastructure to work properly.
     */
    public void clearApplications() {
        applicationsToReturn.clear();
        zuulRouteLinkedHashMap.clear();
    }

    /**
     * Sets which application should be returned for all the callers looking up the service.
     * @param currentApplication Id of the application.
     */
    public void setCurrentApplication(String currentApplication) {
        this.currentApplication = currentApplication;
    }

    public LinkedHashMap<String, ZuulProperties.ZuulRoute> getRoutes() {
        return zuulRouteLinkedHashMap;
    }

    public Applications getApplications() {
        return applicationsToReturn.get(currentApplication);
    }

    public List<InstanceInfo> getInstances() {
        return applicationsToReturn.get(currentApplication).getRegisteredApplications(currentApplication).getInstances();
    }

    public InstanceInfo getInstanceInfo() {
        return applicationsToReturn.get(currentApplication).getRegisteredApplications(currentApplication).getInstances().get(0);
    }

    public void setRoutedServices(List<RoutedServicesUser> routedServicesUsers) {
        this.routedServicesUsers = routedServicesUsers;

        if (!servicesToAdd.isEmpty()) {
            for (RoutedServicesUser routedServicesUser : routedServicesUsers) {
                for (Services services: servicesToAdd) {
                    routedServicesUser.addRoutedServices(services.id, services.routedServices);
                }
            }
        }
    }

    private InstanceInfo getStandardInstance(Map<String, String> metadata, String serviceId) {
        return InstanceInfo.Builder.newBuilder()
            .setAppName(serviceId)
            .setHostName("localhost")
            .setVIPAddress(serviceId)
            .setMetadata(metadata)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .build();
    }

    private Map<String, String> createMetadata() {
        Map<String, String> metadata = new HashMap<>();
        metadata.put("apiml.connectTimeout", "5000");
        metadata.put("apiml.readTimeout", "5000");
        metadata.put("apiml.connectionManagerTimeout", "5000");
        metadata.put("apiml.corsEnabled","true");
        metadata.put("apiml.routes","/test");

        return metadata;
    }

    @RequiredArgsConstructor
    private class Services {
        private final String id;
        private final RoutedServices routedServices;
    }
}
