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

import org.zowe.apiml.registry.model.ServiceInstance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A peer's per-item verdict on a batch, positionally matched to the request.
 * <p>
 * The status code matters beyond success or failure: a 404 on a replicated heartbeat is how a peer says "I have
 * never heard of this instance, send me the full registration". Losing that turns a transient partition into an
 * instance that is permanently missing from one node's registry.
 */
public final class ReplicationResponse {

    /**
     * One verdict.
     *
     * @param statusCode     the peer's HTTP-style status for this item
     * @param responseEntity the peer's own copy of the instance, when it chose to return one
     */
    public record Item(int statusCode, ServiceInstance responseEntity) {

        public boolean notFound() {
            return statusCode == 404;
        }

        public boolean success() {
            return statusCode >= 200 && statusCode < 300;
        }

    }

    private final List<Item> items;

    public ReplicationResponse(List<Item> items) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    public List<Item> items() {
        return items;
    }

    /** Whether the item at {@code index} needs the sender to follow up with a full registration. */
    public boolean requiresReRegistration(int index) {
        return index < items.size() && items.get(index).notFound();
    }

}
