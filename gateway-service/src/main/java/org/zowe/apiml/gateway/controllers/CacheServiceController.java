/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;
import org.zowe.apiml.gateway.security.service.ServiceCacheEvict;

import java.util.List;

/**
 * This controller allows control the caches about services. The main purpose is to evict cached data
 * about services when a update happened in discovery service. Discovery service notifies about any
 * change to be sure that cache on gateway is still valid.
 */
@AllArgsConstructor
@RestController
@RequestMapping(CacheServiceController.CONTROLLER_PATH)
public class CacheServiceController {

    public static final String CONTROLLER_PATH = "/gateway/cache/services";  // NOSONAR: URL is always using / to separate path segments

    private final List<ServiceCacheEvict> toEvict;
    private final ApimlDiscoveryClient discoveryClient;

    @DeleteMapping(path = "")
    public void evictAll() {
        toEvict.forEach(ServiceCacheEvict::evictCacheAllService);
        discoveryClient.fetchRegistry();
    }

    @DeleteMapping(path = "/{serviceId}")
    public void evict(@PathVariable("serviceId") String serviceId) {
        toEvict.forEach(s -> s.evictCacheService(serviceId));
        discoveryClient.fetchRegistry();
    }

}
