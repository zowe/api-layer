/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.Getter;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Provides information about the request to individual load balancing predicates
 * so they can decide which server to select
 */
@Getter
public class LoadBalancingContext {
    private final String serviceId;
    private final InstanceInfo instanceInfo;
    private RequestContext requestContext;
    private SecurityContext securityContext;

    public LoadBalancingContext(String serviceId, InstanceInfo instanceInfo) {
        this.serviceId = serviceId;
        this.instanceInfo = instanceInfo;
        this.requestContext = RequestContext.getCurrentContext();
        this.securityContext = SecurityContextHolder.getContext();
    }

    public String getPath() {
        return requestContext.getRequest() == null ? "" : requestContext.getRequest().getServletPath();
    }
}
