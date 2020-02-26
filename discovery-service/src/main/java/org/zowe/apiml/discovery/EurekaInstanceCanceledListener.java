/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.discovery;

import org.zowe.apiml.util.EurekaUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.server.event.EurekaInstanceCanceledEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
/**
 * Called by Eureka when the service instance is unregistered (/cancelled)
 */
@Component
@RequiredArgsConstructor
public class EurekaInstanceCanceledListener {

    private final GatewayNotifier gatewayNotifier;

    /**
     * Translates service instance Eureka metadata from older versions to the current version
     */
    @EventListener
    public void listen(EurekaInstanceCanceledEvent event) {
        gatewayNotifier.serviceUpdated(EurekaUtils.getServiceIdFromInstanceId(event.getServerId()), null);
    }

}
