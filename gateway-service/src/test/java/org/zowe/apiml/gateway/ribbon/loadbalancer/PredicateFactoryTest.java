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
import org.zowe.apiml.gateway.ribbon.loadbalancer.predicate.RequestHeaderPredicate;

import java.util.Arrays;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;


class PredicateFactoryTest {


    @RequiredArgsConstructor
    private static class TestSpec implements NamedContextFactory.Specification {

        private final String name;

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<?>[] getConfiguration() {
            return new Class[] {TestConfig.class};
        }
    }

    private static class TestConfig {

        @Bean
        @ConditionalOnProperty(name = "huahua", havingValue = "true")
        public RequestAwarePredicate headerPredicate() {
            return new RequestHeaderPredicate();
        }

        @Bean
        @ConditionalOnProperty(name = "huahua", havingValue = "false")
        public RequestAwarePredicate headerPredicate2() {
            return new RequestHeaderPredicate();
        }
    }

    @Nested
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.MOCK,
        properties = {"huahua=true"}
        )

    class GivenProperties {
        @Test
        void name(ApplicationContext context) {

            PredicateFactory<TestSpec> factory = new PredicateFactory( "aa", "aa");

            factory.setApplicationContext(context);
            factory.setConfigurations(Arrays.asList(new TestSpec("default.")));


            Map<String, RequestAwarePredicate> ctx = factory.getInstances("ctx", RequestAwarePredicate.class);
            assertThat(ctx.size(), is(1));
        }
    }


}
