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
import org.zowe.apiml.cloudgatewayservice.acceptance.common.MetadataBuilder;
import org.zowe.apiml.cloudgatewayservice.acceptance.common.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

/**
 * Register the route to all components that need the information for the request to pass properly through the
 * Cloud Gateway. This class is heavily depended upon from the Stubs in this package.
 */
public class ApplicationRegistry {
    private String currentApplication;

    protected int servicePort;
    private Map<String, Application> applicationToReturn = new HashMap<>();

    public ApplicationRegistry() {
    }

    public synchronized int findFreePort() {
        if (servicePort != 0) return servicePort;
        try (ServerSocket server = new ServerSocket(0);) {
            this.servicePort = server.getLocalPort();
        } catch (IOException e) {
            throw new RuntimeException("Failed to find free local port to bind the agent to", e);
        }
        return servicePort;
    }

    public void addApplication(String instanceId, Application application) {

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
        Applications applications = new Applications();
        Application withMetadata = new Application(id);

        Map<String, String> metadata = builder.build();

        withMetadata.addInstance(getStandardInstance(metadata, id, id));
        if (multipleInstances) {
            withMetadata.addInstance(getStandardInstance(metadata, id, id + "-copy"));
        }
        applicationToReturn.put(id, withMetadata);
    }

    public void addApplication(InstanceInfo instanceInfo) {
        Application application = new Application(instanceInfo.getId());
        application.addInstance(instanceInfo);

        applicationToReturn.put(instanceInfo.getId(), application);
    }

    /**
     * Remove all applications from internal mappings. This needs to be followed by adding new ones in order for the
     * discovery infrastructure to work properly.
     */
    public void clearApplications() {
        applicationToReturn.clear();
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
        Applications output = new Applications();
        applicationToReturn.values().stream().forEach(a -> output.addApplication(a));
        return output;
    }

    public List<InstanceInfo> getInstances() {
        List<InstanceInfo> output = new LinkedList<>();
        applicationToReturn.values().forEach(a -> output.addAll(a.getInstances()));
        return output;
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
            .setSecurePort(findFreePort())
            .setPort(findFreePort())
            .build();
    }
}
