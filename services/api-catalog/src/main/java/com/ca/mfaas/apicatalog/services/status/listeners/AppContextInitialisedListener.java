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

import com.ca.mfaas.apicatalog.services.initialisation.InstanceRetrievalService;
import com.ca.mfaas.product.registry.CannotRegisterServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class AppContextInitialisedListener {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(AppContextInitialisedListener.class);
    private final InstanceRetrievalService instanceRetrievalService;

    @Autowired
    public AppContextInitialisedListener(InstanceRetrievalService instanceRetrievalService) {
        this.instanceRetrievalService = instanceRetrievalService;
    }

    /**
     * Create a container for the API Catalog
     *
     * @param event spring event
     */
    @EventListener
    public void onApplicationEvent(ContextRefreshedEvent event) throws CannotRegisterServiceException {
        instanceRetrievalService.retrieveAndRegisterAllInstancesWithCatalog();
    }
}
