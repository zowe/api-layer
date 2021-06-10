/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon.loadbalancer;

import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.MapPropertySource;
import org.zowe.apiml.gateway.ribbon.loadbalancer.predicate.RequestHeaderPredicate;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


class ConfigurableNamedContextFactoryTest {

    @RequiredArgsConstructor
    private static class DummySpec implements NamedContextFactory.Specification {

        private final String name;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<?>[] getConfiguration() {
            return new Class[0];
        }
    }

    private static class TestConfig {

        @Bean
        @ConditionalOnProperty(name = "variable", havingValue = "true")
        public RequestAwarePredicate headerPredicate1() {
            return new RequestHeaderPredicate();
        }

        @Bean
        @ConditionalOnProperty(name = "variable", havingValue = "false")
        public RequestAwarePredicate headerPredicate2() {
            return new RequestHeaderPredicate();
        }

        @Bean
        @ConditionalOnProperty(name = "missing.variable", havingValue = "false")
        public RequestAwarePredicate headerPredicate3() {
            return new RequestHeaderPredicate();
        }
    }

    @Nested
    @SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
    class GivenProperties {
        @Test
        void beansConditionalConstructWorks(ApplicationContext context) {

            ConfigurableNamedContextFactory<DummySpec> factory = new ConfigurableNamedContextFactory( TestConfig.class,"aa", "aa");

            factory.addInitializer("InstanceData", ctx -> {
                ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("InstanceData", Collections.singletonMap("variable", "false"))
                );
            });

            factory.setApplicationContext(context);

            Map<String, RequestAwarePredicate> ctx = factory.getInstances("ctx", RequestAwarePredicate.class);
            assertThat(ctx.size(), is(1));
        }
    }


}
