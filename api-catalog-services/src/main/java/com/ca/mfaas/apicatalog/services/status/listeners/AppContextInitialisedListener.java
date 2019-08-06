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
import com.ca.mfaas.product.gateway.GatewayLookupCompleteEvent;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This class fires on ContextRefreshedEvent event during Spring context initialization
 * Initializes Catalog instances from Eureka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppContextInitialisedListener {

    private final InstanceInitializeService instanceInitializeService;

    /**
     * Retrieves and registers all instances known to Eureka
     *
     * @param event Spring event
     */
    @EventListener
    public void onApplicationEvent(GatewayLookupCompleteEvent event) throws CannotRegisterServiceException {
        instanceInitializeService.retrieveAndRegisterAllInstancesWithCatalog();
    }
}
