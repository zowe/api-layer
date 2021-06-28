/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.cache;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache for storing Load Balancer related information. The initial goal was to support user based instance load
 * balancing
 */
public class LoadBalancerCache {
    private final Map<String, String> cache;

    public LoadBalancerCache() {
        cache = new HashMap<>();
    }

    /**
     * Store information about instance the user is balanced towards.
     *
     * @param user     User being routed towards southbound service
     * @param service  Service towards which is the user routed
     * @param instance Selected instance
     * @return True if storing succeeded, otherwise false
     */
    public boolean store(String user, String service, String instance) {
        try {
            cache.put(user + ":" + service, instance);
            return true;
        } catch (UnsupportedOperationException | ClassCastException | IllegalArgumentException e) {
            return false;
        }
    }

    /**
     * Retrieve information about selected instance for combination of User and Service.
     *
     * @param user    User being routed towards southbound service
     * @param service Service towards which is the user routed
     * @return Retrieved instance to use for this user.
     */
    public String retrieve(String user, String service) {
        return cache.get(user + ":" + service);
    }
}
