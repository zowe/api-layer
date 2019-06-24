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

import com.ca.mfaas.product.service.ServiceStartupEventHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AppReadyListener {


    /**
     * Create a container for the API Catalog
     *
     * @param event spring event
     */
    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        log.info("ApplicationReadyEvent");
       new ServiceStartupEventHandler().onServiceStartup("API Catalog Service",
            ServiceStartupEventHandler.DEFAULT_DELAY_FACTOR);
    }
}
