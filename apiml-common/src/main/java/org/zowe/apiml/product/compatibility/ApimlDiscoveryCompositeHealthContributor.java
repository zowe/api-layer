/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.compatibility;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.cloud.client.discovery.health.DiscoveryHealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * This class is a replacement for DiscoveryCompositeHealthContributor in spring-cloud-cloud-commons:2.2.9.RELEASE in order
 * to work with ApimlHealthCheckHandler more effectively.
 * <p>
 * This code is copied from the 3.1.x version of spring-cloud-commons.
 * https://github.com/spring-cloud/spring-cloud-commons/blob/3.1.x/spring-cloud-commons/src/main/java/org/springframework/cloud/client/discovery/health/DiscoveryCompositeHealthContributor.java
 * <p>
 * NOTE: This should be removed when the APIML upgrades to Spring Cloud 3.x.
 */
@Component
public class ApimlDiscoveryCompositeHealthContributor implements CompositeHealthContributor {
    private final Map<String, DiscoveryHealthIndicator> indicators;

    public ApimlDiscoveryCompositeHealthContributor(Collection<DiscoveryHealthIndicator> indicators) {
        Assert.notNull(indicators, "'indicators' must not be null");
        this.indicators = indicators.stream()
            .collect(Collectors.toMap(DiscoveryHealthIndicator::getName, Function.identity()));
    }

    @Override
    public HealthContributor getContributor(String name) {
        return asHealthIndicator(this.indicators.get(name));
    }

    @Override
    public Iterator<NamedContributor<HealthContributor>> iterator() {
        return this.indicators.values().stream().map(this::asNamedContributor).iterator();
    }

    private NamedContributor<HealthContributor> asNamedContributor(DiscoveryHealthIndicator indicator) {
        return new NamedContributor<HealthContributor>() {

            @Override
            public String getName() {
                return indicator.getName();
            }

            @Override
            public HealthIndicator getContributor() {
                return asHealthIndicator(indicator);
            }

        };
    }

    private HealthIndicator asHealthIndicator(DiscoveryHealthIndicator indicator) {
        return (indicator != null) ? indicator::health : null;
    }

    public Map<String, DiscoveryHealthIndicator> getIndicators() {
        return Collections.unmodifiableMap(indicators);
    }
}
