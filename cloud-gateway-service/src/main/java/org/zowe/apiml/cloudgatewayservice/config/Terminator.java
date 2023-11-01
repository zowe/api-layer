/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;

import java.util.List;

@Getter
@RequiredArgsConstructor
public class Terminator {
    private final List<CloudEurekaClient> discoveryClients;
    public void shutdown() {
        if (discoveryClients != null) {
            discoveryClients.forEach(CloudEurekaClient::shutdown);
        }
    }
}
