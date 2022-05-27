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

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.*;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryHealthIndicator;
import org.springframework.cloud.netflix.eureka.EurekaHealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Copied from https://github.com/spring-cloud/spring-cloud-netflix/blob/3.1.x/spring-cloud-netflix-eureka-client/src/test/java/org/springframework/cloud/netflix/eureka/EurekaHealthCheckHandlerTests.java
 */
class ApimlHealthCheckHandlerTest {
    private ApimlHealthCheckHandler healthCheckHandler;

    @BeforeEach
    void setUp() {

        healthCheckHandler = new ApimlHealthCheckHandler(new SimpleStatusAggregator());
    }

    @Test
    void testNoHealthCheckRegistered() {

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.UNKNOWN);
    }

    @Test
    void testAllUp() {
        initialize(UpHealthConfiguration.class, ReactiveUpHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    void testHealthCheckNotReturnedWhenStopped() {
        initialize(UpHealthConfiguration.class);

        healthCheckHandler.stop();
        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);

        assertThat(status).isNull();
        healthCheckHandler.start();
        InstanceInfo.InstanceStatus newStatus = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(newStatus).isEqualTo(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    void testDownWithBlockingIndicators() {
        initialize(UpHealthConfiguration.class, DownHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.DOWN);
    }

    @Test
    void testDownWithReactiveIndicators() {
        initialize(UpHealthConfiguration.class, ReactiveDownHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.DOWN);
    }

    @Test
    void testDownWhenBlockingIndicatorUpAndReactiveDown() {
        initialize(ReactiveUpHealthConfiguration.class, DownHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = this.healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.DOWN);
    }

    @Test
    void testDownWhenBlockingIndicatorDownAndReactiveUp() {
        initialize(ReactiveUpHealthConfiguration.class, ReactiveDownHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = this.healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.DOWN);
    }

    @Test
    void testUnknown() {
        initialize(FatalHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UNKNOWN);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.UNKNOWN);
    }

    @Test
    void testEurekaIgnored() {
        initialize(EurekaDownHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UP);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    void testCompositeComponentsDown() {
        initialize(CompositeComponentsDownHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UP);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.DOWN);
    }

    @Test
    void testCompositeComponentsUp() {
        initialize(CompositeComponentsUpHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UP);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.UP);
    }

    @Test
    void testCompositeComponentsOneDown() {
        initialize(CompositeComponentsOneDownHealthConfiguration.class);

        InstanceInfo.InstanceStatus status = healthCheckHandler.getStatus(InstanceInfo.InstanceStatus.UP);
        assertThat(status).isEqualTo(InstanceInfo.InstanceStatus.DOWN);
    }

    private void initialize(Class<?>... configurations) {
        ApplicationContext applicationContext = new AnnotationConfigApplicationContext(configurations);
        healthCheckHandler.setApplicationContext(applicationContext);
        healthCheckHandler.afterPropertiesSet();
    }

    public static class UpHealthConfiguration {

        @Bean
        public HealthIndicator healthIndicator() {
            return new AbstractHealthIndicator() {
                @Override
                protected void doHealthCheck(Health.Builder builder) {
                    builder.up();
                }
            };
        }

    }

    public static class DownHealthConfiguration {

        @Bean
        public HealthIndicator healthIndicator() {
            return new AbstractHealthIndicator() {
                @Override
                protected void doHealthCheck(Health.Builder builder) {
                    builder.down();
                }
            };
        }

    }

    public static class FatalHealthConfiguration {

        @Bean
        public HealthIndicator healthIndicator() {
            return new AbstractHealthIndicator() {
                @Override
                protected void doHealthCheck(Health.Builder builder) {
                    builder.status("fatal");
                }
            };
        }

    }

    public static class ReactiveUpHealthConfiguration {

        @Bean
        public ReactiveHealthIndicator reactiveHealthIndicator() {
            return new AbstractReactiveHealthIndicator() {
                @Override
                protected Mono<Health> doHealthCheck(Health.Builder builder) {
                    return Mono.just(builder.up().build());
                }
            };
        }

    }

    public static class ReactiveDownHealthConfiguration {

        @Bean
        public ReactiveHealthIndicator reactiveHealthIndicator() {
            return new AbstractReactiveHealthIndicator() {
                @Override
                protected Mono<Health> doHealthCheck(Health.Builder builder) {
                    return Mono.just(builder.down().build());
                }
            };
        }

    }

    public static class EurekaDownHealthConfiguration {

        @Bean
        public DiscoveryHealthIndicator discoveryHealthIndicator() {
            return new DiscoveryClientHealthIndicator(null, null) {
                @Override
                public Health health() {
                    return Health.up().build();
                }
            };
        }

        @Bean
        public DiscoveryHealthIndicator eurekaHealthIndicator() {
            return new EurekaHealthIndicator(null, null, null) {
                @Override
                public Health health() {
                    return Health.down().build();
                }
            };
        }

        @Bean
        public ApimlDiscoveryCompositeHealthContributor discoveryCompositeHealthContributor(
            List<DiscoveryHealthIndicator> indicators) {
            return new ApimlDiscoveryCompositeHealthContributor(indicators);
        }

    }

    protected static class CompositeComponentsDownHealthConfiguration {

        @Bean
        public CompositeHealthContributor compositeHealthContributor() {
            return new TestCompositeHealthContributor(InstanceInfo.InstanceStatus.DOWN, InstanceInfo.InstanceStatus.DOWN);
        }

    }

    protected static class CompositeComponentsUpHealthConfiguration {

        @Bean
        public CompositeHealthContributor compositeHealthContributor() {
            return new TestCompositeHealthContributor(InstanceInfo.InstanceStatus.UP, InstanceInfo.InstanceStatus.UP);
        }

    }

    protected static class CompositeComponentsOneDownHealthConfiguration {

        @Bean
        public CompositeHealthContributor compositeHealthContributor() {
            return new TestCompositeHealthContributor(InstanceInfo.InstanceStatus.UP, InstanceInfo.InstanceStatus.DOWN);
        }

    }

    static class TestCompositeHealthContributor implements CompositeHealthContributor {

        private final Map<String, HealthContributor> contributorMap = new HashMap<>();

        TestCompositeHealthContributor(InstanceInfo.InstanceStatus firstContributorStatus, InstanceInfo.InstanceStatus secondContributorStatus) {
            contributorMap.put("first", new AbstractHealthIndicator() {
                @Override
                protected void doHealthCheck(Health.Builder builder) {
                    builder.status(firstContributorStatus.name());
                }
            });
            contributorMap.put("second", new AbstractHealthIndicator() {
                @Override
                protected void doHealthCheck(Health.Builder builder) {
                    builder.status(secondContributorStatus.name());
                }
            });
        }

        @Override
        public HealthContributor getContributor(String name) {
            return contributorMap.get(name);
        }

        @Override
        public Iterator<NamedContributor<HealthContributor>> iterator() {
            Iterator<Map.Entry<String, HealthContributor>> iterator = contributorMap.entrySet().iterator();
            return new Iterator<NamedContributor<HealthContributor>>() {

                @Override
                public boolean hasNext() {
                    return iterator.hasNext();
                }

                @Override
                public NamedContributor<HealthContributor> next() {
                    Map.Entry<String, HealthContributor> entry = iterator.next();
                    return NamedContributor.of(entry.getKey(), entry.getValue());
                }

            };
        }

    }
}
