/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.web;

import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaClient;
import jakarta.annotation.PostConstruct;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import static jakarta.ws.rs.core.MediaType.APPLICATION_JSON;

@Component
@RequiredArgsConstructor
@Endpoint(id = "eurekaversion")
@Slf4j
public class EurekaRegistryVersionEndpoint {

    private Long version = -1L;

    private final EurekaClient eurekaClient;

    @PostConstruct
    void registerListener() {
        eurekaClient.registerEventListener(event -> {
            if (event instanceof CacheRefreshedEvent eventCacheRefreshedEvent) {
                onRegistryUpdate(eventCacheRefreshedEvent);
            }
        });
    }

    @EventListener
    void onRegistryUpdate(CacheRefreshedEvent event) {
        this.version = eurekaClient.getApplications().getVersion();
        log.debug("New Eureka registry version: {}", this.version);
    }

    @ReadOperation(produces = APPLICATION_JSON)
    public VersionDto status() {
        return VersionDto.builder().version(this.version).build();
    }

    @Builder
    @Value
    static class VersionDto {

        private Long version;

    }

}
