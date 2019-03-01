/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.gateway.filters.post;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.Debug;
import org.slf4j.Logger;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.POST_TYPE;
import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SEND_RESPONSE_FILTER_ORDER;

/**
 * Log Zuul header debug information
 *
 * @author Dave King
 */
@Component
public class DebugHeaderFilter extends ZuulFilter {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(DebugHeaderFilter.class);

    @Override
    public String filterType() {
        return POST_TYPE;
    }

    @Override
    public int filterOrder() {
        return SEND_RESPONSE_FILTER_ORDER - 1;
    }

    @Override
    public boolean shouldFilter() {
        return Debug.debugRouting();
    }

    @Override
    public Object run() {
        String debug = convertToPrettyPrintString(Debug.getRoutingDebug());
        log.debug("Filter Debug Info = \n{}", debug);
        return null;
    }

    private String convertToPrettyPrintString(List<String> filterDebugList) {
        return filterDebugList.stream()
            .map(s -> s.startsWith("{") ? "\t" + s : s)
            .collect(Collectors.joining("\n"));
    }

}
