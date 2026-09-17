/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import org.springframework.boot.actuate.health.CompositeHealthContributor;
import org.springframework.boot.actuate.health.CompositeReactiveHealthContributor;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.boot.actuate.health.ReactiveHealthContributor;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.boot.actuate.health.Status;
import org.springframework.boot.actuate.health.StatusAggregator;
import org.zowe.apiml.registry.model.InstanceStatus;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Turns Spring Boot's aggregated health into a registered instance status.
 * <p>
 * This is what {@code eureka.client.healthcheck.enabled: true} asked for: a service that is running but
 * unhealthy is registered {@code DOWN} and drops out of the routing table, rather than accepting traffic it
 * cannot serve. Replaces {@code ApimlHealthCheckHandler}, which was itself a copy of Spring Cloud's
 * {@code EurekaHealthCheckHandler} carried for a Spring Boot upgrade years ago.
 * <p>
 * Reactive contributors are blocked on, as they were before. That is safe here because the caller is the
 * registration scheduler's own thread, never an event-loop thread.
 */
public class HealthStatusSource {

    private final StatusAggregator statusAggregator;
    private final Map<String, HealthContributor> healthContributors;
    private final Map<String, ReactiveHealthContributor> reactiveHealthContributors;

    public HealthStatusSource(
        StatusAggregator statusAggregator,
        Map<String, HealthContributor> healthContributors,
        Map<String, ReactiveHealthContributor> reactiveHealthContributors
    ) {
        this.statusAggregator = statusAggregator;
        this.healthContributors = healthContributors;
        this.reactiveHealthContributors = reactiveHealthContributors;
    }

    public InstanceStatus currentStatus() {
        Set<Status> statuses = new HashSet<>();
        for (HealthContributor contributor : healthContributors.values()) {
            collect(statuses, contributor);
        }
        for (ReactiveHealthContributor contributor : reactiveHealthContributors.values()) {
            collect(statuses, contributor);
        }
        return toInstanceStatus(statusAggregator.getAggregateStatus(statuses));
    }

    private void collect(Set<Status> statuses, HealthContributor contributor) {
        if (contributor instanceof CompositeHealthContributor composite) {
            for (NamedContributor<HealthContributor> child : composite) {
                collect(statuses, child.getContributor());
            }
        } else if (contributor instanceof HealthIndicator indicator) {
            statuses.add(indicator.health().getStatus());
        }
    }

    private void collect(Set<Status> statuses, ReactiveHealthContributor contributor) {
        if (contributor instanceof CompositeReactiveHealthContributor composite) {
            for (NamedContributor<ReactiveHealthContributor> child : composite) {
                collect(statuses, child.getContributor());
            }
        } else if (contributor instanceof ReactiveHealthIndicator indicator) {
            Health health = indicator.health().block();
            if (health != null) {
                statuses.add(health.getStatus());
            }
        }
    }

    static InstanceStatus toInstanceStatus(Status status) {
        if (Status.UP.equals(status)) {
            return InstanceStatus.UP;
        }
        if (Status.DOWN.equals(status)) {
            return InstanceStatus.DOWN;
        }
        if (Status.OUT_OF_SERVICE.equals(status)) {
            return InstanceStatus.OUT_OF_SERVICE;
        }
        return InstanceStatus.UNKNOWN;
    }

}
