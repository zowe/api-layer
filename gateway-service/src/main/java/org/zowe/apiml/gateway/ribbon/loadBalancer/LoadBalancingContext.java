/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadBalancer;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.zuul.context.RequestContext;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

@Getter
@RequiredArgsConstructor
public class LoadBalancingContext {
    private final String key;
    private final InstanceInfo instanceInfo;
    private RequestContext requestContext;
    private SecurityContext securityContext;

    {
        requestContext = RequestContext.getCurrentContext();
        securityContext = SecurityContextHolder.getContext();
    }

    public String getPath() {
        return requestContext.getRequest() == null ? "" : requestContext.getRequest().getServletPath();
    }
}
