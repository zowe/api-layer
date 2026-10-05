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
 * Lifecycle status of a registered instance.
 * <p>
 * The enum constant names are the wire representation - they are serialised verbatim - so they must not be
 * renamed. See the wire-contract fixtures under apiml-registry/src/test/resources/wire-contract.
 */
public enum InstanceStatus {

    UP,
    DOWN,
    STARTING,
    OUT_OF_SERVICE,
    UNKNOWN;

    /**
     * Lenient parse: anything unrecognised becomes {@link #UNKNOWN} rather than throwing, because a status we do
     * not recognise arriving from a peer or an older enabler must not fail the whole registry response.
     */
    public static InstanceStatus fromWire(String value) {
        if (value == null) {
            return UNKNOWN;
        }
        try {
            return valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return UNKNOWN;
        }
    }

}
