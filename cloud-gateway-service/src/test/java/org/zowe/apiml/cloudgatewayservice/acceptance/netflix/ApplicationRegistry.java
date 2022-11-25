/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.acceptance.netflix;

import com.netflix.appinfo.DataCenterInfo;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.appinfo.MyDataCenterInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.RequiredArgsConstructor;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MetadataBuilder;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.Service;
import org.zowe.apiml.product.routing.RoutedService;
import org.zowe.apiml.product.routing.RoutedServices;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Register the route to all components that need the information for the request to pass properly through the
 * Cloud Gateway. This class is heavily depended upon from the Stubs in this package.
 */
public class ApplicationRegistry {
    private String currentApplication;
    private Map<String, Applications> applicationsToReturn = new HashMap<>();
    private List<Services> servicesToAdd = new ArrayList<>();
    public ApplicationRegistry() {
    }

    /**
     * Add new application. The customization to the metadata are done via the MetadataBuilder.
     *
     * @param service           Details of the service to be registered in the Cloud Gateway
     * @param builder           The builder pattern for metadata.
     * @param multipleInstances Whether there are multiple instances of the service.
     */
    public void addApplication(Service service, MetadataBuilder builder, boolean multipleInstances) {
        String id = service.getId();
        String serviceRoute = service.getServiceRoute();
        Applications applications = new Applications();
        Application withMetadata = new Application(id);

        Map<String, String> metadata = builder.build();

        withMetadata.addInstance(getStandardInstance(metadata, id, id));
        if (multipleInstances) {
            withMetadata.addInstance(getStandardInstance(metadata, id, id + "-copy"));
        }
        applications.addApplication(withMetadata);

        applicationsToReturn.put(id, applications);

        RoutedServices routedServices = new RoutedServices();
        routedServices.addRoutedService(new RoutedService("test", serviceRoute, ""));
        servicesToAdd.add(new Services(id, routedServices));
    }

    /**
     * Remove all applications from internal mappings. This needs to be followed by adding new ones in order for the
     * discovery infrastructure to work properly.
     */
    public void clearApplications() {
        applicationsToReturn.clear();
    }

    /**
     * Sets which application should be returned for all the callers looking up the service.
     *
     * @param currentApplication Id of the application.
     */
    public void setCurrentApplication(String currentApplication) {
        this.currentApplication = currentApplication;
    }


    public Applications getApplications() {
        return applicationsToReturn.get(currentApplication);
    }

    public List<InstanceInfo> getInstances() {
        if (applicationsToReturn.get(currentApplication) == null) {
            currentApplication = "serviceid1";
        }
        return applicationsToReturn.get(currentApplication).getRegisteredApplications(currentApplication).getInstances();
    }

    public InstanceInfo getInstanceInfo() {
        return applicationsToReturn.get(currentApplication).getRegisteredApplications(currentApplication).getInstances().get(0);
    }

    private InstanceInfo getStandardInstance(Map<String, String> metadata, String serviceId, String instanceId) {
        return InstanceInfo.Builder.newBuilder()
            .setAppName(serviceId)
            .setInstanceId(instanceId)
            .setHostName("localhost")
            .setVIPAddress(serviceId)
            .setMetadata(metadata)
            .setDataCenterInfo(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn))
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .setSecurePort((int) (Math.random() * 10000))
            .setPort((int) (Math.random() * 10000))
            .build();
    }


    @RequiredArgsConstructor
    private class Services {
        private final String id;
        private final RoutedServices routedServices;
    }
}
