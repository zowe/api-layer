/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.metrics.config;

import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.*;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthContributor;
import org.springframework.cloud.netflix.eureka.EurekaHealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Configuration
public class ActuatorConfig {

    @Bean
    public HealthCheckHandler healthCheckHandler(@Autowired StatusAggregator statusAggregator) {
        // one option is to implement EurekaHealthCheckHandler with the replacements for actuator classes, like HealthIndicatorRegistryFactory -> DefaultContributorRegistry
        // or, use a lambda with a hard coded status here - services have their own health indicators implemented, does this value matter?
        // it does show up in /eureka/apps, but is the status here used, or the health check URL?

        // I believe the goal is to make this health check a check for all discovery client health - all GW health or all DS health? Or both? Probably all GW?
        // 3.1.x implementation of EurekaHealthCheckHandler: https://github.com/spring-cloud/spring-cloud-netflix/blob/3.1.x/spring-cloud-netflix-eureka-client/src/main/java/org/springframework/cloud/netflix/eureka/EurekaHealthCheckHandler.java

        // seems discoveryClient.getInstances only returns instances with status up?


        // check original behaviour and see if matches - when mockzosmf shut down, auth status goes to DOWN, when DS killed, no changes,
        // if started with no other services running, DOWN and no services in discoveryClient parts
        return new GatewayHealthCheckHandler(statusAggregator);
    }

    /**
     * This class is a replacement for EurekaHealthCheckHandler in spring-cloud-netflix-eureka-client:2.2.10.RELEASE, which is incompatible with Spring Boot 2.5.
     * EurekaHealthCheckHandler in 2.2.10.RELEASE relies on a few classes that are replaced in Spring Boot 2.5. This class copies the code from
     * EurekaHealthCheckHandler and simply uses the replaced classes.
     */
    private static final class GatewayHealthCheckHandler implements HealthCheckHandler, ApplicationContextAware, InitializingBean, Ordered, Lifecycle {
        private static final Map<Status, InstanceInfo.InstanceStatus> STATUS_MAPPING = new HashMap<Status, InstanceInfo.InstanceStatus>() {
            {
                put(Status.UNKNOWN, InstanceInfo.InstanceStatus.UNKNOWN);
                put(Status.OUT_OF_SERVICE, InstanceInfo.InstanceStatus.OUT_OF_SERVICE);
                put(Status.DOWN, InstanceInfo.InstanceStatus.DOWN);
                put(Status.UP, InstanceInfo.InstanceStatus.UP);
            }
        };

        private final StatusAggregator statusAggregator;
        private ApplicationContext applicationContext;
        private final Map<String, HealthContributor> healthContributors = new HashMap<>();

        /**
         * {@code true} until the context is stopped.
         */
        private boolean running = true;

        private final Map<String, ReactiveHealthContributor> reactiveHealthContributors = new HashMap<>();

        public GatewayHealthCheckHandler(StatusAggregator statusAggregator) {
            this.statusAggregator = statusAggregator;
            Assert.notNull(statusAggregator, "StatusAggregator must not be null");

        }

        @Override
        public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
            this.applicationContext = applicationContext;
        }

        @Override
        public void afterPropertiesSet() {
            populateHealthContributors(applicationContext.getBeansOfType(HealthContributor.class));
            reactiveHealthContributors.putAll(applicationContext.getBeansOfType(ReactiveHealthContributor.class));
        }

        private void populateHealthContributors(Map<String, HealthContributor> healthContributors) {
            for (Map.Entry<String, HealthContributor> entry : healthContributors.entrySet()) {
                // ignore EurekaHealthIndicator and flatten the rest of the composite
                // otherwise there is a never ending cycle of down. See gh-643
                if (entry.getValue() instanceof DiscoveryCompositeHealthContributor) {
                    // Below is the only code that is changed (other than simple changes like making method private or variable final.
                    // This is because the copied EurekaHealthCheckHandler code relies on a newer version of spring-cloud-commons,
                    // so the getIndicators method does not exist in our project.
//                    DiscoveryCompositeHealthContributor indicator = (DiscoveryCompositeHealthContributor) entry.getValue();
//                    indicator.getIndicators().forEach((name, discoveryHealthIndicator) -> {
//                        if (!(discoveryHealthIndicator instanceof EurekaHealthIndicator)) {
//                            this.healthContributors.put(name, (HealthIndicator) discoveryHealthIndicator::health);
//                        }
//                    });

//                    DiscoveryCompositeHealthContributor indicator = (DiscoveryCompositeHealthContributor) entry.getValue();
//                    indicator.getIndicators().forEach((name, discoveryHealthIndicator) -> {
//                        if (!(discoveryHealthIndicator instanceof EurekaHealthIndicator)) {
//                            this.healthContributors.put(name, (HealthIndicator) discoveryHealthIndicator::health);
//                        }
//                    });

                    DiscoveryCompositeHealthContributor indicator = (DiscoveryCompositeHealthContributor) entry.getValue();
                    for (NamedContributor<HealthContributor> namedContributor : indicator) {
                        if (!(namedContributor.getContributor() instanceof EurekaHealthIndicator)) {
//                            this.healthContributors.put(namedContributor.getName(), ((HealthIndicator) namedContributor.getContributor()).health());
                            this.healthContributors.put(namedContributor.getName(), namedContributor.getContributor());
                        }
                    }

                } else {
                    this.healthContributors.put(entry.getKey(), entry.getValue());
                }
            }
        }

        @Override
        public InstanceInfo.InstanceStatus getStatus(InstanceInfo.InstanceStatus instanceStatus) {
            if (running) {
                return getHealthStatus();
            } else {
                // Return nothing if the context is not running, so the status held by the
                // InstanceInfo remains unchanged.
                // See gh-1571
                return null;
            }
        }

        private InstanceInfo.InstanceStatus getHealthStatus() {
            Status status = getStatus(statusAggregator);
            return mapToInstanceStatus(status);
        }

        private Status getStatus(StatusAggregator statusAggregator) {
            Set<Status> statusSet = new HashSet<>();
            for (HealthContributor contributor : healthContributors.values()) {
                processContributor(statusSet, contributor);
            }
            for (ReactiveHealthContributor contributor : reactiveHealthContributors.values()) {
                processContributor(statusSet, contributor);
            }
            return statusAggregator.getAggregateStatus(statusSet);
        }

        private void processContributor(Set<Status> statusSet, HealthContributor contributor) {
            if (contributor instanceof CompositeHealthContributor) {
                for (NamedContributor<HealthContributor> contrib : (CompositeHealthContributor) contributor) {
                    processContributor(statusSet, contrib.getContributor());
                }
            } else if (contributor instanceof HealthIndicator) {
                statusSet.add(((HealthIndicator) contributor).health().getStatus());
            }
        }

        private void processContributor(Set<Status> statusSet, ReactiveHealthContributor contributor) {
            if (contributor instanceof CompositeReactiveHealthContributor) {
                for (NamedContributor<ReactiveHealthContributor> contrib : (CompositeReactiveHealthContributor) contributor) {
                    processContributor(statusSet, contrib.getContributor());
                }
            } else if (contributor instanceof ReactiveHealthIndicator) {
                Health health = ((ReactiveHealthIndicator) contributor).health().block();
                if (health != null) {
                    statusSet.add(health.getStatus());
                }
            }
        }

        private InstanceInfo.InstanceStatus mapToInstanceStatus(Status status) {
            if (!STATUS_MAPPING.containsKey(status)) {
                return InstanceInfo.InstanceStatus.UNKNOWN;
            }
            return STATUS_MAPPING.get(status);
        }

        @Override
        public int getOrder() {
            // registered with a high order priority so the close() method is invoked early
            // and *BEFORE* EurekaAutoServiceRegistration
            // (must be in effect when the registration is closed and the eureka replication
            // triggered -> health check handler is
            // consulted at that moment)
            return Ordered.HIGHEST_PRECEDENCE;
        }

        @Override
        public void start() {
            running = true;
        }

        @Override
        public void stop() {
            running = false;
        }

        @Override
        public boolean isRunning() {
            return true;
        }
    }
}
