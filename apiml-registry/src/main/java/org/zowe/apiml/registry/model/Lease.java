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

/**
 * The lease held by a registered instance: how often it promises to renew, how long the registry waits before
 * considering it gone, and the timestamps of the lease lifecycle.
 * <p>
 * A lease may be {@link Kind#PERMANENT}, which is how statically-defined services are represented. Eureka had no
 * such concept and APIML faked it by subclassing its Lease with an {@code isExpired()} that always returned false,
 * plus a {@code durationInSecs} of {@code Integer.MAX_VALUE / 1000} to dodge an int-overflow bug. Here it is a
 * first-class kind - see TASK-Discovery-Native-Registry.md section 4.3.
 */
public final class Lease {

    /**
     * The duration Eureka's overflow workaround produced. Statically-registered instances are still serialised
     * with this value so that the representation on the wire is unchanged for existing clients.
     */
    public static final int PERMANENT_DURATION_SECS = Integer.MAX_VALUE / 1000;

    public enum Kind {
        /** Renewed by heartbeats; expires if they stop. */
        RENEWABLE,
        /** Never expires and is never counted towards the renew threshold. */
        PERMANENT
    }

    private final Kind kind;
    private final int renewalIntervalSecs;
    private final int durationSecs;
    private final long registrationTimestamp;
    private final long lastRenewalTimestamp;
    private final long evictionTimestamp;
    private final long serviceUpTimestamp;

    private Lease(Builder builder) {
        this.kind = builder.kind;
        this.renewalIntervalSecs = builder.renewalIntervalSecs;
        this.durationSecs = builder.durationSecs;
        this.registrationTimestamp = builder.registrationTimestamp;
        this.lastRenewalTimestamp = builder.lastRenewalTimestamp;
        this.evictionTimestamp = builder.evictionTimestamp;
        this.serviceUpTimestamp = builder.serviceUpTimestamp;
    }

    public Kind kind() {
        return kind;
    }

    public int renewalIntervalSecs() {
        return renewalIntervalSecs;
    }

    public int durationSecs() {
        return durationSecs;
    }

    public long registrationTimestamp() {
        return registrationTimestamp;
    }

    public long lastRenewalTimestamp() {
        return lastRenewalTimestamp;
    }

    public long evictionTimestamp() {
        return evictionTimestamp;
    }

    public long serviceUpTimestamp() {
        return serviceUpTimestamp;
    }

    public boolean permanent() {
        return kind == Kind.PERMANENT;
    }

    /**
     * Whether this lease has lapsed at {@code now}, allowing {@code additionalLeaseMs} of slack.
     * <p>
     * Note the doubled duration - it is deliberate, and it is not what Eureka's configuration appears to say.
     * Verified against {@code com.netflix.eureka.lease.Lease} 2.0.6: {@code renew()} stored
     * {@code System.currentTimeMillis() + duration} into its {@code lastUpdateTimestamp}, and
     * {@code isExpired(additionalLeaseMs)} then compared {@code now} against
     * {@code lastUpdateTimestamp + duration + additionalLeaseMs}. Duration is therefore applied twice, so a lease
     * survived roughly <em>twice</em> its configured duration after the last heartbeat.
     * <p>
     * That is reproduced rather than corrected. Fixing it would halve the effective eviction window for every
     * deployed service simultaneously - precisely the kind of silent timing change that turns into a routing
     * outage. Correcting it is a separate, deliberate decision needing its own migration and release note.
     * <p>
     * Unlike Eureka this class keeps {@code lastRenewalTimestamp} as the real renewal instant, because that value
     * is also published on the wire in {@code leaseInfo}; the doubling lives here in the comparison instead of
     * being hidden in the stored field.
     */
    public boolean expired(long now, long additionalLeaseMs) {
        if (permanent()) {
            return false;
        }
        if (evictionTimestamp > 0) {
            return true;
        }
        return now > lastRenewalTimestamp + (2L * durationSecs * 1000L) + additionalLeaseMs;
    }

    public boolean expired(long now) {
        return expired(now, 0L);
    }

    public Lease renewedAt(long now) {
        return toBuilder().lastRenewalTimestamp(now).build();
    }

    public Lease evictedAt(long now) {
        return toBuilder().evictionTimestamp(now).build();
    }

    public Builder toBuilder() {
        return new Builder()
            .kind(kind)
            .renewalIntervalSecs(renewalIntervalSecs)
            .durationSecs(durationSecs)
            .registrationTimestamp(registrationTimestamp)
            .lastRenewalTimestamp(lastRenewalTimestamp)
            .evictionTimestamp(evictionTimestamp)
            .serviceUpTimestamp(serviceUpTimestamp);
    }

    public static Builder builder() {
        return new Builder();
    }

    /** A renewable lease starting now, with Eureka's default cadence. */
    public static Lease renewable(int renewalIntervalSecs, int durationSecs, long now) {
        return builder()
            .kind(Kind.RENEWABLE)
            .renewalIntervalSecs(renewalIntervalSecs)
            .durationSecs(durationSecs)
            .registrationTimestamp(now)
            .lastRenewalTimestamp(now)
            .serviceUpTimestamp(now)
            .build();
    }

    /** A lease for a statically-defined service: never expires, never counted for self-preservation. */
    public static Lease permanent(long now) {
        return builder()
            .kind(Kind.PERMANENT)
            .renewalIntervalSecs(PERMANENT_DURATION_SECS)
            .durationSecs(PERMANENT_DURATION_SECS)
            .registrationTimestamp(now)
            .lastRenewalTimestamp(now)
            .serviceUpTimestamp(now)
            .build();
    }

    public static final class Builder {

        private Kind kind = Kind.RENEWABLE;
        private int renewalIntervalSecs = 30;
        private int durationSecs = 90;
        private long registrationTimestamp;
        private long lastRenewalTimestamp;
        private long evictionTimestamp;
        private long serviceUpTimestamp;

        public Builder kind(Kind kind) {
            this.kind = kind;
            return this;
        }

        public Builder renewalIntervalSecs(int renewalIntervalSecs) {
            this.renewalIntervalSecs = renewalIntervalSecs;
            return this;
        }

        public Builder durationSecs(int durationSecs) {
            this.durationSecs = durationSecs;
            return this;
        }

        public Builder registrationTimestamp(long registrationTimestamp) {
            this.registrationTimestamp = registrationTimestamp;
            return this;
        }

        public Builder lastRenewalTimestamp(long lastRenewalTimestamp) {
            this.lastRenewalTimestamp = lastRenewalTimestamp;
            return this;
        }

        public Builder evictionTimestamp(long evictionTimestamp) {
            this.evictionTimestamp = evictionTimestamp;
            return this;
        }

        public Builder serviceUpTimestamp(long serviceUpTimestamp) {
            this.serviceUpTimestamp = serviceUpTimestamp;
            return this;
        }

        public Lease build() {
            return new Lease(this);
        }

    }

}
