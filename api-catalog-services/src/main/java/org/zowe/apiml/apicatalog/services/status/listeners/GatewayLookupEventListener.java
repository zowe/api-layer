/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.services.status.listeners;

import org.zowe.apiml.apicatalog.instance.InstanceInitializeService;
import org.zowe.apiml.product.gateway.GatewayLookupCompleteEvent;
import org.zowe.apiml.product.registry.CannotRegisterServiceException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * This class fires on GatewayLookupCompleteEvent event
 * Initializes Catalog instances from Eureka
 */
@Component
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GatewayLookupEventListener {

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
