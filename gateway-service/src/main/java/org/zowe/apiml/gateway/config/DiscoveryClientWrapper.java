/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;

import java.util.List;

@AllArgsConstructor
@Getter
public class DiscoveryClientWrapper {
    private List<ApimlDiscoveryClient> discoveryClients;

    public void shutdown() {
        if (discoveryClients != null) {
            discoveryClients.forEach(ApimlDiscoveryClient::shutdown);
        }
    }
}
