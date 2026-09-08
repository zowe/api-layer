/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry;

/**
 * Registry tunables.
 * <p>
 * The defaults are Eureka's, verified against {@code DefaultEurekaServerConfig} and Spring Cloud's
 * {@code EurekaServerConfigBean} 4.3.3, because an existing deployment that does not set these must keep behaving
 * as it does today. Changing a default here changes eviction timing on every upgraded system.
 *
 * @param selfPreservationEnabled       whether a suspiciously low renewal rate suspends eviction entirely
 * @param renewalPercentThreshold       fraction of expected renewals that must arrive for eviction to stay on
 * @param expectedClientRenewalIntervalSeconds how often a client is assumed to heartbeat
 * @param deltaRetentionMs              how long a change stays visible in the delta response
 * @param defaultLeaseDurationSecs      lease duration used when a registrant does not supply one
 * @param defaultRenewalIntervalSecs    renewal interval used when a registrant does not supply one
 */
public record RegistryConfig(
    boolean selfPreservationEnabled,
    double renewalPercentThreshold,
    int expectedClientRenewalIntervalSeconds,
    long deltaRetentionMs,
    int defaultLeaseDurationSecs,
    int defaultRenewalIntervalSecs
) {

    public static final double DEFAULT_RENEWAL_PERCENT_THRESHOLD = 0.85;
    public static final int DEFAULT_EXPECTED_CLIENT_RENEWAL_INTERVAL_SECONDS = 30;
    public static final long DEFAULT_DELTA_RETENTION_MS = 3 * 60 * 1000L;
    public static final int DEFAULT_LEASE_DURATION_SECS = 90;
    public static final int DEFAULT_RENEWAL_INTERVAL_SECS = 30;

    public static RegistryConfig defaults() {
        return new RegistryConfig(
            true,
            DEFAULT_RENEWAL_PERCENT_THRESHOLD,
            DEFAULT_EXPECTED_CLIENT_RENEWAL_INTERVAL_SECONDS,
            DEFAULT_DELTA_RETENTION_MS,
            DEFAULT_LEASE_DURATION_SECS,
            DEFAULT_RENEWAL_INTERVAL_SECS
        );
    }

    public RegistryConfig withSelfPreservation(boolean enabled) {
        return new RegistryConfig(enabled, renewalPercentThreshold, expectedClientRenewalIntervalSeconds,
            deltaRetentionMs, defaultLeaseDurationSecs, defaultRenewalIntervalSecs);
    }

    public RegistryConfig withDeltaRetentionMs(long retentionMs) {
        return new RegistryConfig(selfPreservationEnabled, renewalPercentThreshold,
            expectedClientRenewalIntervalSeconds, retentionMs, defaultLeaseDurationSecs, defaultRenewalIntervalSecs);
    }

}
