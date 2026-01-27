/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.Status;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaHealthIndicator;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * This class is replacement of org.springframework.cloud.netflix.eureka.EurekaHealthIndicator, because it is using
 * a different Eureka client
 */
@Primary
@Component("eurekaHealthIndicator")
public class EurekaHealthIndicatorApiml extends EurekaHealthIndicator {

    private final DiscoveryClient discoveryClient;

    public EurekaHealthIndicatorApiml(DiscoveryClient discoveryClient) {
        super(null, null, null);
        this.discoveryClient = discoveryClient;
    }

    @Override
    public String getName() {
        return "eureka";
    }

    @Override
    public Health health() {
        Health.Builder builder = Health.unknown();
        Status status = getStatus(builder);
        return builder.status(status).withDetail("applications", getApplications()).build();
    }

    private Status getStatus(Health.Builder builder) {
        if (discoveryClient.getServices().isEmpty()) {
            return new Status("UP", "Eureka registry is not available at the moment");
        }
        return new Status("UP", "Eureka is ready to use");
    }

    private Map<String, Object> getApplications() {
        return discoveryClient.getServices().stream()
            .collect(Collectors.toMap(
                String::toLowerCase,
                serviceId -> discoveryClient.getInstances(serviceId).size())
            );
    }

}
