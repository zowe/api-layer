/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.springframework.context.ApplicationEvent;
import org.zowe.apiml.registry.client.RegistryCache;

/**
 * Published after the cached view of the registry has been brought up to date.
 * <p>
 * The replacement for Netflix's {@code CacheRefreshedEvent}. It is an ordinary Spring event, which means
 * listeners are declared with {@code @EventListener} and are discovered by the container - no
 * {@code registerEventListener}/{@code unregisterEventListener} bookkeeping, which was easy to get wrong: a
 * listener that forgot to unregister kept a reference to a dead context alive.
 */
public class RegistryCacheRefreshedEvent extends ApplicationEvent {

    private static final long serialVersionUID = 1L;

    private final transient RegistryCache cache;

    public RegistryCacheRefreshedEvent(Object source, RegistryCache cache) {
        super(source);
        this.cache = cache;
    }

    public RegistryCache getCache() {
        return cache;
    }

}
