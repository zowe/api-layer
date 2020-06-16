/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */


package org.zowe.apiml.gateway.metadata.service;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that extracts the instances metadata from the registered applications and set the ones related to the ribbon timeout
 * (connectionTimeout, readTimeout and connectionManagerTimeout) as system properties
 */
@Service
@RequiredArgsConstructor
public class MetadataProcessor implements EurekaEventListener {
    private final EurekaApplications applications;

    private void checkMetadata(InstanceInfo instanceInfo) {
        String serviceId = instanceInfo.getVIPAddress();
        Map<String, String> metadata = instanceInfo.getMetadata();
        if (metadata != null) {
            setConnectTimeout(serviceId, metadata);
            setReadTimeout(serviceId, metadata);
            setConnectionManagerTimeout(serviceId, metadata);
        }
    }

    public void setConnectTimeout(String serviceId, Map<String, String> metadata) {
        String connectTimeout = metadata.get("apiml.connectTimeout");
        if (!Strings.isEmpty(connectTimeout) && StringUtils.isNumeric(connectTimeout)) {
            System.setProperty(serviceId + ".ribbon.ConnectTimeout", connectTimeout);
        }
    }

    public void setReadTimeout(String serviceId, Map<String, String> metadata) {
        String readTimeout = metadata.get("apiml.readTimeout");
        if (!Strings.isEmpty(readTimeout) && StringUtils.isNumeric(readTimeout)) {
            System.setProperty(serviceId + ".ribbon.ReadTimeout", readTimeout);
        }
    }

    public void setConnectionManagerTimeout(String serviceId, Map<String, String> metadata) {
        String connectionManagerTimeout = metadata.get("apiml.connectionManagerTimeout");
        if (!Strings.isEmpty(connectionManagerTimeout) && StringUtils.isNumeric(connectionManagerTimeout)) {
            System.setProperty(serviceId + ".ribbon.ConnectionManagerTimeout", connectionManagerTimeout);
        }
    }

    /**
     * Processes all the instances metadata of the registered applications by checking ribbon timeout parameters and by setting them as system properties
     * @param applications
     */
    public void process(List<Application> applications) {
        Set<InstanceInfo> infoSet = applications.stream()
            .flatMap(application -> application.getInstances().stream())
            .collect(Collectors.toSet());

        infoSet.forEach(instanceInfo -> checkMetadata(instanceInfo));
    }

    @Override
    public void onEvent(EurekaEvent event) {
        process(applications.getRegistered());
    }
}
