/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.replication;

import org.zowe.apiml.registry.model.InstanceStatus;
import org.zowe.apiml.registry.model.ServiceInstance;

/**
 * One change to send to a peer.
 *
 * @param action           what happened
 * @param appName          the application, always present so the peer can route the change without the body
 * @param id               the instance id
 * @param lastDirtyTimestamp the registrant's own version stamp, used by the peer to reject a stale change
 * @param status           the instance status, as a string because an unrecognised value must survive the round trip
 * @param overriddenStatus the operator override, likewise
 * @param instance         the full instance, present only for {@link ReplicationAction#Register}
 */
public record ReplicationItem(
    ReplicationAction action,
    String appName,
    String id,
    Long lastDirtyTimestamp,
    String status,
    String overriddenStatus,
    ServiceInstance instance
) {

    public static ReplicationItem register(ServiceInstance instance) {
        return new ReplicationItem(
            ReplicationAction.Register,
            instance.appName(),
            instance.instanceId(),
            instance.lastDirtyTimestamp(),
            name(instance.status()),
            name(instance.overriddenStatus()),
            instance
        );
    }

    public static ReplicationItem heartbeat(ServiceInstance instance) {
        return identityOnly(ReplicationAction.Heartbeat, instance);
    }

    public static ReplicationItem cancel(ServiceInstance instance) {
        return identityOnly(ReplicationAction.Cancel, instance);
    }

    public static ReplicationItem statusUpdate(ServiceInstance instance) {
        return identityOnly(ReplicationAction.StatusUpdate, instance);
    }

    public static ReplicationItem deleteStatusOverride(ServiceInstance instance) {
        return identityOnly(ReplicationAction.DeleteStatusOverride, instance);
    }

    private static ReplicationItem identityOnly(ReplicationAction action, ServiceInstance instance) {
        return new ReplicationItem(
            action,
            instance.appName(),
            instance.instanceId(),
            instance.lastDirtyTimestamp(),
            name(instance.status()),
            name(instance.overriddenStatus()),
            null
        );
    }

    private static String name(InstanceStatus status) {
        return status == null ? null : status.name();
    }

    /** Identity used for de-duplication in the outbound queue: the newest change per instance per action wins. */
    public String dedupeKey() {
        return appName + '/' + id + '/' + action;
    }

}
