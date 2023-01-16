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

import com.netflix.discovery.shared.transport.EurekaHttpResponse;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class ReplicationTask {

    private static final Logger logger = LoggerFactory.getLogger(ReplicationTask.class);

    protected final String peerNodeName;
    protected final PeerAwareInstanceRegistryImpl.Action action;

    public ReplicationTask(String peerNodeName, PeerAwareInstanceRegistryImpl.Action action) {
        this.peerNodeName = peerNodeName;
        this.action = action;
    }

    public abstract String getTaskName();

    public PeerAwareInstanceRegistryImpl.Action getAction() {
        return action;
    }

    public abstract EurekaHttpResponse<?> execute() throws Throwable;

    public void handleSuccess() {
    }

    public void handleFailure(int statusCode, Object responseEntity) throws Throwable {
        logger.warn("The replication of task {} failed with response code {}", getTaskName(), statusCode);
    }
}
