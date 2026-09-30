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

import org.zowe.apiml.registry.client.RegistryClient;
import org.zowe.apiml.registry.client.RegistryTransport;
import org.zowe.apiml.registry.model.ServiceInstance;

public class DiscoveryClientProvider implements org.zowe.apiml.eurekaservice.client.EurekaClientProvider {

    @Override
    public RegistryClient client(RegistryTransport transport, ServiceInstance self) {
        return new RegistryClient(transport, self);
    }
}
