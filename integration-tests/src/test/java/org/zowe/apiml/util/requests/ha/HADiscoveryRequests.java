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
import org.zowe.apiml.util.requests.DiscoveryRequests;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.zowe.apiml.util.config.ConfigReader.environmentConfiguration;

/**
 * Basically wrapper around the requests to multiple instances. Understand the context of the multiple Discovery services.
 */
@Slf4j
public class HADiscoveryRequests {
    public List<DiscoveryRequests> discoveryServices = new ArrayList<>();

    public HADiscoveryRequests() {
        discoveryServices.add(new DiscoveryRequests(environmentConfiguration().getDiscoveryServiceConfiguration().getHost()));
        discoveryServices.add(new DiscoveryRequests(environmentConfiguration().getDiscoveryServiceConfiguration().getAdditionalHost()));
        log.info("Created HADiscoveryRequests");
    }

    public int existing() {
        return discoveryServices.size();
    }

    public boolean up(int instance) {
        return discoveryServices.get(instance).isUp();
    }

    public boolean up() {
        AtomicBoolean allUp = new AtomicBoolean(true);

        discoveryServices.parallelStream().forEach(service -> {
            if (!service.isUp()) {
                allUp.set(false);
            }
        });

        return allUp.get();
    }

    public void shutdown(int instance) {
        discoveryServices.get(instance).shutdown();
    }

    public void shutdown() {
        discoveryServices.parallelStream()
            .forEach(DiscoveryRequests::shutdown);
    }

    public boolean isApplicationRegistered(int instance, String appName) {
        return discoveryServices.get(instance).isApplicationRegistered(appName);
    }

    /**
     * True if registered on all available discovery services.
     *
     * @param appName Name of the app to lookup
     */
    public boolean isApplicationRegistered(String appName) {
        AtomicBoolean allRegistered = new AtomicBoolean(true);

        discoveryServices.parallelStream().forEach(service -> {
            if (!service.isApplicationRegistered(appName)) {
                allRegistered.set(false);
            }
        });

        return allRegistered.get();
    }

    public int getAmountOfRegisteredInstancesForService(int instance, String appName) {
        return discoveryServices.get(instance).getAmountOfRegisteredInstancesForService(appName);
    }

    public List<Integer> getAmountOfRegisteredInstancesForService(String appName) {
        List<Integer> amountOfRegisteredPerInstance = new ArrayList<>();

        discoveryServices.parallelStream().forEach(service -> {
            amountOfRegisteredPerInstance.add(service.getAmountOfRegisteredInstancesForService(appName));
        });

        return amountOfRegisteredPerInstance;
    }
}
