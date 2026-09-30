/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * A registry snapshot: the response body of GET /eureka/apps and GET /eureka/apps/delta.
 * <p>
 * {@code appsHashCode} is not merely a cache key. Clients compare it to decide whether their view is stale, and
 * APIML additionally parses a counter out of it to publish a registry version on the {@code eurekaversion}
 * actuator endpoint. Its format is therefore part of the contract - see {@link #computeHashCode}.
 */
public final class Applications {

    private final List<Application> applications;
    private final Long version;
    private final String appsHashCode;

    public Applications(List<Application> applications, Long version, String appsHashCode) {
        this.applications = Collections.unmodifiableList(new ArrayList<>(applications));
        this.version = version;
        this.appsHashCode = appsHashCode;
    }

    public List<Application> applications() {
        return applications;
    }

    public Long version() {
        return version;
    }

    public String appsHashCode() {
        return appsHashCode;
    }

    public boolean empty() {
        return applications.isEmpty();
    }

    /**
     * Builds the {@code apps__hashcode} string: for each status present, in status-name order,
     * {@code <STATUS>_<count>_} concatenated.
     * <p>
     * Verified against the wire-contract fixtures - a registry holding three UP instances yields {@code "UP_3_"}.
     * The trailing underscore and the ordering both matter: APIML's own registry-version endpoint matches
     * {@code UP_(\d+)_} against this value, and clients compare it byte-for-byte to detect drift.
     */
    public static String computeHashCode(List<Application> applications) {
        // Keyed by the status NAME in a TreeMap, so ordering is alphabetical - DOWN, OUT_OF_SERVICE, STARTING,
        // UNKNOWN, UP - and not the enum declaration order. Verified against Eureka 2.0.6, whose
        // Applications.getReconcileHashCode() builds a `new TreeMap<String, AtomicInteger>()` and concatenates
        // entrySet() in iteration order. Using the enum's natural order here would silently produce a different
        // hash for any registry holding more than one status, which clients read as "your view is stale".
        Map<String, Integer> counts = new TreeMap<>();
        for (Application application : applications) {
            for (ServiceInstance instance : application.instances()) {
                InstanceStatus status = instance.status() == null ? InstanceStatus.UNKNOWN : instance.status();
                counts.merge(status.name(), 1, Integer::sum);
            }
        }
        StringBuilder result = new StringBuilder();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            result.append(entry.getKey()).append('_').append(entry.getValue()).append('_');
        }
        return result.toString();
    }

    @Override
    public String toString() {
        return "Applications[" + applications.size() + " apps, hash=" + appsHashCode + "]";
    }

}
