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

import lombok.RequiredArgsConstructor;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;
import org.zowe.apiml.discovery.staticdef.StaticServicesRegistrationService;
import org.zowe.apiml.product.service.ServiceStartupEventHandler;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Called by Eureka when its service registry is initialized.
 *
 * It is calling services that require registry to be initialized.
 */
@Component
@RequiredArgsConstructor
public class EurekaRegistryAvailableListener implements ApplicationListener<EurekaRegistryAvailableEvent> {

    private final StaticServicesRegistrationService registrationService;
    private final ServiceStartupEventHandler handler;
    private final AtomicBoolean startUpInfoPublished = new AtomicBoolean(false);

    @Override
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        registrationService.registerServices();
        if (startUpInfoPublished.compareAndSet(false, true)) {
            handler.onServiceStartup("Discovery Service", ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
        }
    }

}
