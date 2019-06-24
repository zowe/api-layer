/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.services.status.listeners;

import com.ca.mfaas.apicatalog.instance.InstanceInitializeService;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppContextInitialisedListener {

    private final InstanceInitializeService instanceInitializeService;

    @Autowired
    public AppContextInitialisedListener(InstanceInitializeService instanceInitializeService) {
        this.instanceInitializeService = instanceInitializeService;
    }

    /**
     * Create a container for the API Catalog
     *
     * @param event spring event
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws CannotRegisterServiceException {
        log.info("AppContextInitialisedListener");
        instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
    }
}
