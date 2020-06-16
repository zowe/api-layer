/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.filters.post;

import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.Debug;
import com.netflix.zuul.context.RequestContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.ribbon.RequestContextUtils;

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
@Slf4j
public class DebugHeaderFilter extends ZuulFilter {

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
        String debug2 = Debug.getRequestDebug().stream().collect(Collectors.joining("|"));
        String debug3 = convertToPrettyPrintString(Debug.getRequestDebug());
        String reqInfo = RequestContext.getCurrentContext().getFilterExecutionSummary().toString();
        String reqInfo2 = RequestContextUtils.getDebugInfo();
        String reqInfo3 = RequestContextUtils.getInstanceInfo().toString();
        log.info("\n\n\nBoy if this msg dont show up\n\n\n");
        log.debug("\n\n\nTest deubg statement\n\n\n");
        log.warn("\n\n\n---TIM---- Additional request info: \n********** \nreqInfo\n{} \nreqInfo2\n{} \nreqInfo3\n{}\n**********\n", reqInfo, reqInfo2, reqInfo3);
        log.warn("\n\n\n---TIM----Debug.getRoutingDebug(): \n********** \n{}\nDebug.getRequestDebug(): \n{}", debug, debug3);
        log.debug("RibbonRetryDebug: " + RequestContextUtils.getDebugInfo());
        RequestContext.getCurrentContext().addZuulResponseHeader(
            "ZuulFilterDebug", Debug.getRoutingDebug().stream().collect(Collectors.joining("|")));
        RequestContext.getCurrentContext().addZuulResponseHeader(
            "RibbonRetryDebug", RequestContextUtils.getDebugInfo());
        String reqDebug = Debug.getRequestDebug().stream().collect(Collectors.joining("|"));
        return null;
    }

    private String convertToPrettyPrintString(List<String> filterDebugList) {
        return filterDebugList.stream()
            .map(s -> s.startsWith("{") ? "\t" + s : s)
            .collect(Collectors.joining("\n"));
    }

}
