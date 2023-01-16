/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.eureka.client;

import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.netflix.eureka.resources.ASGResource;

public abstract class AsgReplicationTask extends ReplicationTask {

    private final String asgName;
    private final ASGResource.ASGStatus newStatus;

    public AsgReplicationTask(String peerNodeName, PeerAwareInstanceRegistryImpl.Action action, String asgName, ASGResource.ASGStatus newStatus) {
        super(peerNodeName, action);
        this.asgName = asgName;
        this.newStatus = newStatus;
    }

    @Override
    public String getTaskName() {
        return asgName + ':' + action + '@' + peerNodeName;
    }

    public String getAsgName() {
        return asgName;
    }

    public ASGResource.ASGStatus getNewStatus() {
        return newStatus;
    }
}
