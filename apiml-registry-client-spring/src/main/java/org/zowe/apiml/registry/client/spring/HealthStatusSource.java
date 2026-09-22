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

import lombok.extern.slf4j.Slf4j;
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

import java.util.LinkedHashMap;
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
@Slf4j
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
        Map<String, Status> byContributor = new LinkedHashMap<>();
        for (Map.Entry<String, HealthContributor> entry : healthContributors.entrySet()) {
            collectNamed(byContributor, entry.getKey(), entry.getValue());
        }
        for (Map.Entry<String, ReactiveHealthContributor> entry : reactiveHealthContributors.entrySet()) {
            collectNamed(byContributor, entry.getKey(), entry.getValue());
        }

        Status aggregate = statusAggregator.getAggregateStatus(Set.copyOf(byContributor.values()));
        InstanceStatus result = toInstanceStatus(aggregate);
        if (result != InstanceStatus.UP) {
            // A service about to be advertised as unhealthy should say why, and which indicator decided it: the
            // aggregate alone is not enough to tell a genuine fault from a self-referential one, where a
            // service reports DOWN because it cannot yet see the registry it is reporting to.
            log.warn("Aggregate health is {}, so the registered status is {}. Contributors: {}",
                aggregate.getCode(), result, byContributor);
        }
        return result;
    }

    private void collectNamed(Map<String, Status> into, String name, HealthContributor contributor) {
        if (contributor instanceof CompositeHealthContributor composite) {
            for (NamedContributor<HealthContributor> child : composite) {
                collectNamed(into, name + "." + child.getName(), child.getContributor());
            }
        } else if (contributor instanceof HealthIndicator indicator) {
            into.put(name, indicator.health().getStatus());
        }
    }

    private void collectNamed(Map<String, Status> into, String name, ReactiveHealthContributor contributor) {
        if (contributor instanceof CompositeReactiveHealthContributor composite) {
            for (NamedContributor<ReactiveHealthContributor> child : composite) {
                collectNamed(into, name + "." + child.getName(), child.getContributor());
            }
        } else if (contributor instanceof ReactiveHealthIndicator indicator) {
            Health health = indicator.health().block();
            if (health != null) {
                into.put(name, health.getStatus());
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
