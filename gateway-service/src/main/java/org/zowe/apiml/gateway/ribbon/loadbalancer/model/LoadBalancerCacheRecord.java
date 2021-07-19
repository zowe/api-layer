/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * Data POJO that represents entry in load balancing service cache
 */
@Data
public class LoadBalancerCacheRecord {
    private final String instanceId;
    private final LocalDateTime creationTime;

    public LoadBalancerCacheRecord(String instanceId) {
        this(instanceId, LocalDateTime.now());
    }

    @JsonCreator
    public LoadBalancerCacheRecord(@JsonProperty("instanceId") String instanceId, @JsonProperty("creationTime") LocalDateTime creationTime) {
        this.instanceId = instanceId;
        this.creationTime = creationTime;
    }
}
