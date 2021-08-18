/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.requests.ha;

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.util.requests.ApiCatalogRequests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zowe.apiml.util.config.ConfigReader.environmentConfiguration;

/**
 * Basically wrapper around the requests to multiple instances. Understand the context of the multiple API Catalog services
 * and provide methods to check the amount of instances, their health and the capability to shutdown them
 */
@Slf4j
public class HAApiCatalogRequests {
    public List<ApiCatalogRequests> apiCatalogServices = new ArrayList<>();

    public HAApiCatalogRequests() {
        String[] apiCatalogHosts = environmentConfiguration().getApiCatalogServiceConfiguration().getHost().split(",");
        for (String host: apiCatalogHosts) {
            apiCatalogServices.add(new ApiCatalogRequests(host));
        }
        log.info("Created HAApiCatalogRequests");
    }

    /**
     * Return the number of instances.
     */
    public int existing() {
        return apiCatalogServices.size();
    }

    /**
     * Check whether a specific instance is UP.
     * @param instance the specific instance
     * @return true if UP
     */
    public boolean up(int instance) {
        return apiCatalogServices.get(instance).isUp();
    }

    /**
     * Check whether all the instances are UP.
     * @return true if UP
     */
    public boolean up() {
        AtomicBoolean allUp = new AtomicBoolean(true);

        apiCatalogServices.parallelStream().forEach(service -> {
            if (!service.isUp()) {
                allUp.set(false);
            }
        });
        return allUp.get();
    }

    /**
     * Shutdown a specific instance of the service.
     * @param instance the specific instance
     */
    public void shutdown(int instance) {
        apiCatalogServices.get(instance).shutdown();
    }

    /**
     * Shutdown all the instances of the service.
     */
    public void shutdown() {
        apiCatalogServices.parallelStream()
            .forEach(ApiCatalogRequests::shutdown);
    }

}
