/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client;

import org.springframework.cloud.client.DefaultServiceInstance;

import java.util.Map;

/**
 * Spring Cloud view of an instance obtained from the APIML registry.
 * <p>
 * {@link DefaultServiceInstance} inherits the default {@code ServiceInstance.getScheme()} implementation,
 * which returns {@code null}. Eureka's service-instance adapter returned the actual transport scheme and
 * Gateway components rely on that value when constructing direct calls to ZAAS and other services. Keep that
 * behaviour as part of the client-adapter contract.
 */
public final class RegistryServiceInstance extends DefaultServiceInstance {

    public RegistryServiceInstance(
        String instanceId,
        String serviceId,
        String host,
        int port,
        boolean secure,
        Map<String, String> metadata
    ) {
        super(instanceId, serviceId, host, port, secure, metadata);
    }

    @Override
    public String getScheme() {
        return isSecure() ? "https" : "http";
    }

}
