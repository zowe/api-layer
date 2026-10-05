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

/**
 * What a peer is being told to do.
 * <p>
 * The constant names are the wire representation and are mixed-case, matching Eureka's enum exactly - a peer
 * running the current implementation will not recognise {@code REGISTER} where it expects {@code Register}.
 */
public enum ReplicationAction {

    Heartbeat,
    Register,
    Cancel,
    StatusUpdate,
    DeleteStatusOverride;

    public static ReplicationAction fromWire(String value) {
        if (value == null) {
            return null;
        }
        for (ReplicationAction action : values()) {
            if (action.name().equalsIgnoreCase(value.trim())) {
                return action;
            }
        }
        return null;
    }

    /** Only a registration carries the whole instance; the rest identify it and let the peer look it up. */
    public boolean carriesInstance() {
        return this == Register;
    }

}
