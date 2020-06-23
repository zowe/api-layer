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
            setIfExistsAndIsNumeric(serviceId + ".ribbon.ConnectTimeout",
                metadata.get("apiml.connectTimeout"));
            setIfExistsAndIsNumeric(serviceId + ".ribbon.ReadTimeout",
                metadata.get("apiml.readTimeout"));
            setIfExistsAndIsNumeric(serviceId + ".ribbon.ConnectionManagerTimeout",
                metadata.get("apiml.connectionManagerTimeout"));
            setIfExists(serviceId + ".ribbon.OkToRetryOnAllOperations",
                metadata.get("apiml.okToRetryOnAllOperations"));
        }
    }

    void setIfExistsAndIsNumeric(String key, String value) {
        if (!Strings.isEmpty(value) && StringUtils.isNumeric(value)) {
            System.setProperty(key, value);
        }
    }

    void setIfExists(String key, String value) {
        if (!Strings.isEmpty(value)) {
            System.setProperty(key, value);
        }
    }
}
