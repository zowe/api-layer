/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.FilterDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
public class RoutingConfig {

    @Value("${apiml.service.ignoredHeadersWhenCorsEnabled:-}")
    private String ignoredHeadersWhenCorsEnabled;

    @Value("${apiml.service.forwardClientCertEnabled:false}")
    private String forwardingEnabled;

    @Bean
    public List<FilterDefinition> filters() {
        FilterDefinition circuitBreakerFilter = new FilterDefinition();
        circuitBreakerFilter.setName("CircuitBreaker");

        FilterDefinition retryFilter = new FilterDefinition();
        retryFilter.setName("Retry");
        retryFilter.addArg("retries", "5");
        retryFilter.addArg("statuses", "SERVICE_UNAVAILABLE");

        FilterDefinition clientCertFilter = new FilterDefinition();
        clientCertFilter.setName("ClientCertFilterFactory");
        clientCertFilter.addArg("forwardingEnabled", forwardingEnabled);

        List<FilterDefinition> filters = new ArrayList<>();
        filters.add(circuitBreakerFilter);
        filters.add(retryFilter);
        filters.add(clientCertFilter);
        for (String headerName : ignoredHeadersWhenCorsEnabled.split(",")) {
            FilterDefinition removeHeaders = new FilterDefinition();
            removeHeaders.setName("RemoveRequestHeader");
            Map<String, String> args = new HashMap<>();
            args.put("name", headerName);
            removeHeaders.setArgs(args);
            filters.add(removeHeaders);
        }
        return filters;
    }
}
