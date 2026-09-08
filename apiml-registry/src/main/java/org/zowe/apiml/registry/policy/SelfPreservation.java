/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.policy;

import org.zowe.apiml.registry.RegistryConfig;

/**
 * The circuit breaker that stops the registry emptying itself during a network partition.
 * <p>
 * If renewals dry up, the likely cause is that the registry cannot be reached - not that every service died at
 * once. Eviction is therefore suspended while the observed renewal rate is below a threshold, so a partition
 * degrades into stale routing rather than into no routing at all. On a sysplex, getting this wrong means a
 * plex-wide outage, which is why it is modelled explicitly and tested directly.
 * <p>
 * Formulae verified against Eureka 2.0.6 ({@code AbstractInstanceRegistry.updateRenewsPerMinThreshold},
 * {@code PeerAwareInstanceRegistryImpl.isLeaseExpirationEnabled}, {@code AbstractInstanceRegistry.evict}).
 */
public final class SelfPreservation {

    private final RegistryConfig config;

    public SelfPreservation(RegistryConfig config) {
        this.config = config;
    }

    /**
     * Renewals per minute below which eviction stops.
     * <p>
     * {@code expected * (60 / renewalIntervalSeconds) * percentThreshold}, truncated to an int - the truncation is
     * Eureka's and is kept, because rounding instead would shift the threshold by one renewal on small registries
     * where that is proportionally significant.
     */
    public int renewalThresholdPerMinute(int expectedClientsSendingRenews) {
        return (int) (expectedClientsSendingRenews
            * (60.0 / config.expectedClientRenewalIntervalSeconds())
            * config.renewalPercentThreshold());
    }

    /**
     * Whether leases are allowed to expire right now.
     * <p>
     * Note the strict {@code >}: a rate exactly at the threshold suspends eviction. That is Eureka's behaviour and
     * it errs on the safe side, so it is preserved.
     */
    public boolean evictionAllowed(int expectedClientsSendingRenews, long renewalsInLastMinute) {
        if (!config.selfPreservationEnabled()) {
            return true;
        }
        int threshold = renewalThresholdPerMinute(expectedClientsSendingRenews);
        return threshold > 0 && renewalsInLastMinute > threshold;
    }

    /**
     * How many instances may be evicted in one pass.
     * <p>
     * A second, independent guard: even with eviction enabled, at most {@code (1 - percentThreshold)} of the
     * registry goes in a single sweep. This is what makes a slow partition bleed rather than flush, and it is
     * computed from the current registry size so that a clock jump or a long GC pause cannot wipe everything.
     */
    public int evictionLimit(int currentRegistrySize) {
        int protectedCount = (int) (currentRegistrySize * config.renewalPercentThreshold());
        return Math.max(0, currentRegistrySize - protectedCount);
    }

}
