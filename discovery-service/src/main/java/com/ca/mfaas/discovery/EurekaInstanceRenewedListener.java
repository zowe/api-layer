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
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceRenewedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listener of updated services.
 */
@Component
@RequiredArgsConstructor
public class EurekaInstanceRenewedListener {

    private final GatewayNotifier gatewayNotifier;

    @EventListener
    public void listen(EurekaInstanceRenewedEvent event) {
        final String instanceId = event.getInstanceInfo().getInstanceId();
        final String serviceId = EurekaUtils.getServiceIdFromInstanceId(instanceId);
        // ie. update instance can have different authentication, this is reason to evict caches on gateway
        gatewayNotifier.serviceUpdated(serviceId);
    }

}
