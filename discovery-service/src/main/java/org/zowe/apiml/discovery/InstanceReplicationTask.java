/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;

public abstract class InstanceReplicationTask extends ReplicationTask {

    /**
     * For cancel request there may be no InstanceInfo object available so we need to store app/id pair
     * explicitly.
     */
    private final String appName;
    private final String id;

    private final InstanceInfo instanceInfo;
    private final InstanceInfo.InstanceStatus overriddenStatus;

    private final boolean replicateInstanceInfo;

    protected InstanceReplicationTask(String peerNodeName, PeerAwareInstanceRegistryImpl.Action action, String appName, String id) {
        super(peerNodeName, action);
        this.appName = appName;
        this.id = id;
        this.instanceInfo = null;
        this.overriddenStatus = null;
        this.replicateInstanceInfo = false;
    }

    protected InstanceReplicationTask(String peerNodeName,
                                      PeerAwareInstanceRegistryImpl.Action action,
                                      InstanceInfo instanceInfo,
                                      InstanceInfo.InstanceStatus overriddenStatus,
                                      boolean replicateInstanceInfo) {
        super(peerNodeName, action);
        this.appName = instanceInfo.getAppName();
        this.id = instanceInfo.getId();
        this.instanceInfo = instanceInfo;
        this.overriddenStatus = overriddenStatus;
        this.replicateInstanceInfo = replicateInstanceInfo;
    }

    public String getTaskName() {
        return appName + '/' + id + ':' + action + '@' + peerNodeName;
    }

    public String getAppName() {
        return appName;
    }

    public String getId() {
        return id;
    }

    public InstanceInfo getInstanceInfo() {
        return instanceInfo;
    }

    public InstanceInfo.InstanceStatus getOverriddenStatus() {
        return overriddenStatus;
    }

    public boolean shouldReplicateInstanceInfo() {
        return replicateInstanceInfo;
    }
}
