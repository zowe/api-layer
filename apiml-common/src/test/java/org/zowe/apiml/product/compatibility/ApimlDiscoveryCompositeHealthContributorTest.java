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

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.NamedContributor;
import org.springframework.cloud.client.discovery.health.DiscoveryHealthIndicator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Copied from https://github.com/spring-cloud/spring-cloud-commons/blob/3.1.x/spring-cloud-commons/src/test/java/org/springframework/cloud/client/discovery/health/DiscoveryCompositeHealthContributorTests.java
 */
class ApimlDiscoveryCompositeHealthContributorTest {
    @Test
    public void createWhenIndicatorsAreNullThrowsException() {
        assertThatIllegalArgumentException().isThrownBy(() -> new ApimlDiscoveryCompositeHealthContributor(null))
            .withMessage("'indicators' must not be null");
    }

    @Test
    public void getContributorReturnsContributor() {
        TestDiscoveryHealthIndicator indicator = new TestDiscoveryHealthIndicator("test", Health.up().build());
        ApimlDiscoveryCompositeHealthContributor composite = new ApimlDiscoveryCompositeHealthContributor(
            Collections.singletonList(indicator));
        HealthIndicator adapted = (HealthIndicator) composite.getContributor("test");
        assertThat(adapted).isNotNull();
        assertThat(adapted.health()).isSameAs(indicator.health());
    }

    @Test
    public void getContributorWhenMissingReturnsNull() {
        TestDiscoveryHealthIndicator indicator = new TestDiscoveryHealthIndicator("test", Health.up().build());
        ApimlDiscoveryCompositeHealthContributor composite = new ApimlDiscoveryCompositeHealthContributor(
            Collections.singletonList(indicator));
        assertThat((HealthIndicator) composite.getContributor("missing")).isNull();
    }

    @Test
    public void iteratorIteratesNamedContributors() {
        TestDiscoveryHealthIndicator indicator1 = new TestDiscoveryHealthIndicator("test1", Health.up().build());
        TestDiscoveryHealthIndicator indicator2 = new TestDiscoveryHealthIndicator("test2", Health.down().build());
        ApimlDiscoveryCompositeHealthContributor composite = new ApimlDiscoveryCompositeHealthContributor(
            Arrays.asList(indicator1, indicator2));
        List<NamedContributor<HealthContributor>> contributors = new ArrayList<>();
        for (NamedContributor<HealthContributor> contributor : composite) {
            contributors.add(contributor);
        }
        assertThat(contributors).hasSize(2);
        assertThat(contributors).extracting("name").containsExactlyInAnyOrder("test1", "test2");
    }

    private static class TestDiscoveryHealthIndicator implements DiscoveryHealthIndicator {

        private final String name;
        private final Health health;

        TestDiscoveryHealthIndicator(String name, Health health) {
            super();
            this.name = name;
            this.health = health;
        }

        @Override
        public String getName() {
            return this.name;
        }

        @Override
        public Health health() {
            return this.health;
        }

    }
}
