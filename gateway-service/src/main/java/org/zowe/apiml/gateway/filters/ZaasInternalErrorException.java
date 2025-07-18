/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.filters;

import lombok.Getter;
import org.springframework.cloud.client.ServiceInstance;

/**
 * Represents an error during the authentication process or during obtaining an authentication scheme token
 */
public class ZaasInternalErrorException extends Exception {

    @Getter
    private final String instanceId;

    public ZaasInternalErrorException(ServiceInstance zaasInstance, String message) {
        this(zaasInstance.getInstanceId(), message);
    }

    public ZaasInternalErrorException(String instanceId, String message) {
        super(message);
        this.instanceId = instanceId;
    }

}
