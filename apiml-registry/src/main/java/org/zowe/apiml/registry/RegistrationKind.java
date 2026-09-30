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
 * How an instance came to be in the registry.
 * <p>
 * This distinction is the replacement for a cluster of workarounds in the Eureka-based implementation: APIML
 * tracked statically-defined services in a side set of instance ids, gave them a hand-rolled never-expiring lease
 * subclass, and nudged the renew-threshold counter through a ThreadLocal so they would not be counted as
 * heartbeat-sending clients. Making it an explicit property of the registration removes all of that - see
 * TASK-Discovery-Native-Registry.md section 4.3.
 */
public enum RegistrationKind {

    /** Registered by a running service that will send heartbeats. Counts towards the renew threshold. */
    DYNAMIC,

    /**
     * Registered from a static YAML definition. Never heartbeats, never expires, and is deliberately excluded
     * from the renew threshold so its presence cannot suppress eviction of real services.
     */
    STATIC,

    /** Arrived from a peer node. Counts as a real client but must not be replicated onward. */
    REPLICATED,

    /**
     * This node registering its own identity.
     * <p>
     * Distinct from {@link #STATIC} because it is not third-party input. The registration interceptors - notably
     * the domain allow list - exist to police what other services claim about themselves; applying them to our
     * own identity is both pointless and actively harmful. The allow list rejects {@code http://} URLs unless
     * AT-TLS is on, so a Discovery Service configured without TLS would refuse to register itself, and because
     * that now happens synchronously at startup it would fail to start at all.
     * <p>
     * Like {@link #STATIC} it gets a permanent lease and is excluded from the renew threshold - it does not
     * heartbeat to itself.
     */
    SELF;

    public boolean countsTowardsRenewThreshold() {
        return this != STATIC && this != SELF;
    }

    public boolean permanentLease() {
        return this == STATIC || this == SELF;
    }

    /** Whether the registration interceptors apply. See {@link #SELF}. */
    public boolean subjectToInterceptors() {
        return this != SELF;
    }

    public boolean fromPeer() {
        return this == REPLICATED;
    }

}
