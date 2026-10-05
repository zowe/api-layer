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

import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.Map;
import java.util.Optional;

/**
 * Decides the status an instance is stored with, which is not always the status it asked for.
 * <p>
 * Reproduces Eureka's rule chain, in order, first match wins:
 * <ol>
 *     <li><b>Believe bad news.</b> A registrant reporting anything other than UP or OUT_OF_SERVICE is taken at its
 *         word - if a service says it is DOWN or STARTING, it knows better than the registry.</li>
 *     <li><b>An explicit override wins.</b> Set by an operator through the status endpoint.</li>
 *     <li><b>An existing lease wins over a fresh claim of UP.</b> Only for non-replicated registrations, and only
 *         when the stored status is UP or OUT_OF_SERVICE. This is what stops a restarting instance from
 *         resurrecting itself as UP while an operator has taken it out of service.</li>
 *     <li><b>Otherwise</b> use what the registrant said.</li>
 * </ol>
 * The ordering is load-bearing: swapping rules 2 and 3 would let a restarting service silently override an
 * operator's OUT_OF_SERVICE decision.
 */
public final class StatusOverridePolicy {

    /**
     * @param instance      the registrant
     * @param existing      the instance already held under this id, if any
     * @param fromPeer      whether this registration arrived by peer replication
     * @param overrides     operator-set overrides, keyed by instance id
     * @return the status to store
     */
    public InstanceStatus resolve(
        ServiceInstance instance,
        ServiceInstance existing,
        boolean fromPeer,
        Map<String, InstanceStatus> overrides
    ) {
        InstanceStatus claimed = instance.status() == null ? InstanceStatus.UNKNOWN : instance.status();

        // 1. Trust a registrant that reports itself unhealthy.
        if (claimed != InstanceStatus.UP && claimed != InstanceStatus.OUT_OF_SERVICE) {
            return claimed;
        }

        // 2. An explicit operator override beats everything below it.
        InstanceStatus override = overrides.get(instance.instanceId());
        if (override != null) {
            return override;
        }

        // 3. A locally-held UP or OUT_OF_SERVICE beats a fresh registration's claim. Skipped for replication so a
        //    peer's view is not overridden by our own stale copy.
        if (!fromPeer && existing != null) {
            InstanceStatus stored = existing.status();
            if (stored == InstanceStatus.UP || stored == InstanceStatus.OUT_OF_SERVICE) {
                return stored;
            }
        }

        // 4. Take the registrant at its word.
        return claimed;
    }

    /**
     * Whether an instance is currently routable.
     * <p>
     * Provided here rather than on the model because it is a policy question, and the Gateway asks it on every
     * route rebuild.
     */
    public boolean routable(ServiceInstance instance) {
        return Optional.ofNullable(instance)
            .map(ServiceInstance::effectiveStatus)
            .filter(InstanceStatus.UP::equals)
            .isPresent();
    }

}
