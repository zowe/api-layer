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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** A batch of changes posted to a peer as {@code POST /eureka/peerreplication/batch/}. */
public final class ReplicationBatch {

    private final List<ReplicationItem> items;

    public ReplicationBatch(List<ReplicationItem> items) {
        this.items = Collections.unmodifiableList(new ArrayList<>(items));
    }

    public List<ReplicationItem> items() {
        return items;
    }

    public int size() {
        return items.size();
    }

    public boolean isEmpty() {
        return items.isEmpty();
    }

}
