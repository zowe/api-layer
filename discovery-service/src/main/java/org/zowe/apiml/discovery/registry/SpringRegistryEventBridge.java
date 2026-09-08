/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.registry;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.zowe.apiml.discovery.registry.event.RegistryAvailableEvent;
import org.zowe.apiml.discovery.registry.event.RegistryInstanceRegisteredEvent;
import org.zowe.apiml.registry.RegistryEvent;
import org.zowe.apiml.registry.RegistryListener;
import org.zowe.apiml.registry.ServiceRegistry;

/**
 * Republishes registry events as Spring application events.
 * <p>
 * This is the seam that keeps the registry core free of Spring while letting the rest of APIML - the Gateway's
 * route refresh, the Catalog's availability tracking, the health indicators - carry on listening the way they
 * always have.
 */
@Component
@RequiredArgsConstructor
public class SpringRegistryEventBridge implements RegistryListener {

    private final ServiceRegistry registry;
    private final ApplicationEventPublisher publisher;

    @PostConstruct
    void subscribe() {
        registry.addListener(this);
    }

    /*
     * if-else over instanceof rather than a pattern switch: switch patterns are preview on the Java 17 baseline.
     */
    @Override
    public void onRegistryEvent(RegistryEvent event) {
        if (event instanceof RegistryEvent.InstanceRegistered registered) {
            publisher.publishEvent(
                new RegistryInstanceRegisteredEvent(this, registered.instance(), registered.fromPeer()));
        } else if (event instanceof RegistryEvent.RegistryAvailable) {
            publisher.publishEvent(new RegistryAvailableEvent(this));
        }
        // Renew, cancel and status-change are deliberately not republished. Renewals arrive once per service
        // every thirty seconds; turning each into a Spring event would rebuild the Gateway route table
        // continuously for no benefit. Listeners that need those can subscribe to the registry directly.
    }

}
