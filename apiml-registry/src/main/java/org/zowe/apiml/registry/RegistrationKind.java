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
    REPLICATED;

    public boolean countsTowardsRenewThreshold() {
        return this != STATIC;
    }

    public boolean permanentLease() {
        return this == STATIC;
    }

    public boolean fromPeer() {
        return this == REPLICATED;
    }

}
