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

import com.ca.mfaas.discovery.staticdef.StaticServicesRegistrationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.eureka.server.event.EurekaRegistryAvailableEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

/**
 * Called by Eureka when its service registry is initialized.
 * <p>
 * It is calling services that require registry to be initialized.
 */
@Component
public class EurekaRegistryAvailableListener implements ApplicationListener<EurekaRegistryAvailableEvent> {
    private final StaticServicesRegistrationService registrationService;

    @Autowired
    public EurekaRegistryAvailableListener(StaticServicesRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @SuppressWarnings("NullableProblems")
    @Override
    public void onApplicationEvent(EurekaRegistryAvailableEvent event) {
        registrationService.registerServices();
    }
}
