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

import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.shared.Application;
import com.netflix.discovery.shared.Applications;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Class that retrieves all the registered applications from Eureka Client
 */
@Service
@RequiredArgsConstructor
public class EurekaApplications {

    private final List<EurekaClient> eurekaClientList;

    public List<Application> getRegistered() {
        List<Application> result = new ArrayList<>();
        for(EurekaClient eurekaClient : eurekaClientList) {
            Applications applications = eurekaClient.getApplications();
            if (applications == null) {
                continue;
            }
            result.addAll(applications.getRegisteredApplications());
        }
        return result;
    }
}
