/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer.predicate;

import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.gateway.ribbon.loadbalancer.*;

/**
 * Predicate that looks for a presence of header containing serviceId value
 * in the request and matching on that server.
 */

public class RequestHeaderPredicate extends RequestAwarePredicate {

    public static final String REQUEST_HEADER_NAME = LoadBalancerConstants.INSTANCE_HEADER_KEY;

    @Override
    public boolean apply(LoadBalancingContext context, DiscoveryEnabledServer server) {
        if (context.getRequestContext().getRequest() != null) {
            String targetServer = context.getRequestContext().getRequest().getHeader(REQUEST_HEADER_NAME);
            if (StringUtils.isEmpty(targetServer)) {
                return true;
            }
            return server.getInstanceInfo().getInstanceId().equalsIgnoreCase(targetServer);
        }
        return true;
    }

    @Override
    public String toString() {
        return "RequestHeaderPredicate (" + REQUEST_HEADER_NAME + ")";
    }

}
