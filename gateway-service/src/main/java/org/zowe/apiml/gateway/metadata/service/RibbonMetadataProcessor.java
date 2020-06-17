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
import com.netflix.discovery.shared.Application;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RibbonMetadataProcessor extends MetadataProcessor {

    private final EurekaApplications applications;

    @Override
    List<Application> getApplications() {
        return this.applications.getRegistered();
    }

    protected void checkInstanceInfo(InstanceInfo instanceInfo) {
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
}
