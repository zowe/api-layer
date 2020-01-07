/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery;

import com.ca.mfaas.util.EurekaUtils;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.shared.Application;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import com.netflix.eureka.registry.PeerAwareInstanceRegistry;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Component
@AllArgsConstructor
@Slf4j
public class GatewayNotifier {

    private final RestTemplate restTemplate;

    private EurekaServerContext getServerContext() {
        return EurekaServerContextHolder.getInstance().getServerContext();
    }

    private PeerAwareInstanceRegistry getRegistry() {
        return getServerContext().getRegistry();
    }

    public void serviceUpdated(String serviceId) {
        final PeerAwareInstanceRegistry registry = getRegistry();
        final Application application = registry.getApplication("gateway");
        if (application == null) {
            log.error("Gateway application doesn't exists, cannot be notified about service change");
            return;
        }

        final List<InstanceInfo> gatewayInstances = application.getInstances();

        for (final InstanceInfo instanceInfo : gatewayInstances) {
            final StringBuilder url = new StringBuilder();
            url.append(EurekaUtils.getUrl(instanceInfo)).append("/cache/services");
            if (serviceId != null) url.append('/').append(serviceId);
            restTemplate.delete(url.toString());
        }
    }

}
