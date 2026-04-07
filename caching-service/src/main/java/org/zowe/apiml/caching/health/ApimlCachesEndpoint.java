/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.health;

import lombok.experimental.Delegate;
import org.springframework.boot.actuate.cache.CachesEndpoint;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.cache.CacheManager;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Map;

@Component
@Endpoint(id = "caches")
public class ApimlCachesEndpoint extends CachesEndpoint {

    @Delegate
    private CachesEndpoint cachesEndpoint = new CachesEndpoint(Collections.emptyMap());

    public ApimlCachesEndpoint() {
        super(Collections.emptyMap());
    }

    @EventListener
    public void onApplicationEvent(ApplicationReadyEvent event) {
        var context = event.getApplicationContext();
        Map<String, CacheManager> current = context.getBeansOfType(CacheManager.class);
        this.cachesEndpoint = new CachesEndpoint(current);
    }

}
