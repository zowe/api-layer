/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.metadata.service;

import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
import lombok.AllArgsConstructor;
import org.springframework.cloud.netflix.zuul.filters.RefreshableRouteLocator;
import org.springframework.cloud.netflix.zuul.web.ZuulHandlerMapping;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@DependsOn({"loadBalancerEventListener"})
@AllArgsConstructor
public class RefreshRouteListener implements EurekaEventListener {

    private final List<RefreshableRouteLocator> refreshableRouteLocators;
    private final ZuulHandlerMapping zuulHandlerMapping;

    @Override
    public void onEvent(EurekaEvent event) {
        if (event instanceof CacheRefreshedEvent) {
            refreshableRouteLocators.forEach(RefreshableRouteLocator::refresh);
            zuulHandlerMapping.setDirty(true);
        }
    }
}
