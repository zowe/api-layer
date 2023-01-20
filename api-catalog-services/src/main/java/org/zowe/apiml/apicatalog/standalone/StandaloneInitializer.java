/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * Initializes Catalog instances from files
 */
@Component
@ConditionalOnProperty(
    value = "apiml.catalog.standalone.enabled",
    havingValue = "true")
@RequiredArgsConstructor
@Order(Ordered.HIGHEST_PRECEDENCE)
public class StandaloneInitializer {

    private final StandaloneLoaderService standaloneLoaderService;
    private final AtomicBoolean hasRun = new AtomicBoolean(false);

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationEvent(ApplicationReadyEvent event) {
        if (isStandalone() && hasRun.compareAndSet(false, true)) {
            standaloneLoaderService.initializeCache();
        }
    }

    private boolean isStandalone() {
        return standaloneLoaderService != null;
    }

}
