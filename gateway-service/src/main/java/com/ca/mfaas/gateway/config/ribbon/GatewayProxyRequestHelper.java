/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.ribbon;

import com.netflix.zuul.context.RequestContext;
import org.apache.commons.lang3.StringUtils;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpServletRequest;

@Component
@Primary
public class GatewayProxyRequestHelper extends ProxyRequestHelper {

    @Override
    public String buildZuulRequestURI(HttpServletRequest request) {
        RequestContext context = RequestContext.getCurrentContext();
        String contextURI = (String) context.get("requestURI");
        if (StringUtils.isNotEmpty(contextURI)) {
            return contextURI;
        } else {
            return super.buildZuulRequestURI(request);
        }
    }

}
