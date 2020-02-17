/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.eurekaservice.client.impl;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.discovery.AbstractDiscoveryClientOptionalArgs;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;

public class DiscoveryClientProvider implements org.zowe.apiml.eurekaservice.client.EurekaClientProvider {
    @Override
    public EurekaClient client(ApplicationInfoManager applicationInfoManager, EurekaClientConfig clientConfig, AbstractDiscoveryClientOptionalArgs args) {
        return new DiscoveryClient(applicationInfoManager, clientConfig, args);
    }
}
