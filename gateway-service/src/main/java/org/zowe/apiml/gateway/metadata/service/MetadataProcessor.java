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

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Class that extracts the instances metadata from the registered applications and set the ones related to the ribbon timeout
 * (connectionTimeout, readTimeout and connectionManagerTimeout) as system properties
 */

public abstract class MetadataProcessor extends RefreshEventListener {


    abstract List<Application> getApplications();

    abstract void checkInstanceInfo(InstanceInfo instanceInfo);

    /**
     * Processes all the instances metadata of the registered applications by checking ribbon timeout parameters and by setting them as system properties
     *
     * @param applications
     */
    public void process(List<Application> applications) {
        Set<InstanceInfo> infoSet = applications.stream()
            .flatMap(application -> application.getInstances().stream())
            .collect(Collectors.toSet());
        infoSet.forEach(this::checkInstanceInfo);
    }

    @Override
    public void refresh() {
        process(getApplications());
    }

}
