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

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.zowe.apiml.gateway.ribbon.loadbalancer.predicate.RequestHeaderPredicate;

import java.util.*;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
class ConfigurableNamedContextFactoryTest {

    //This is here to load context fast by creating just this bean
    @Configuration
    public static class MainContextConfiguration {
        @Bean
        public Date dateBean() {
            return new Date();
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
    class GivenProperties {
        @Test
        void beansConditionalConstructWorks(ApplicationContext context) {

            ConfigurableNamedContextFactory<NamedContextFactory.Specification> factory = new ConfigurableNamedContextFactory( TestConfig.class,"aa", "aa");

            factory.addInitializer("InstanceData", ctx -> {
                ctx.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("InstanceData", Collections.singletonMap("variable", "false"))
                );
            });

            factory.setApplicationContext(context);

            Map<String, RequestAwarePredicate> beans = factory.getInstances("ctx", RequestAwarePredicate.class);
            assertThat(beans.size(), is(1));
            assertThat(beans.keySet(), contains("headerPredicate2"));
        }
    }

    private static class WiringConfig {

        @Bean
        public RequestAwarePredicate headerPredicate1() {
            return new RequestHeaderPredicate();
        }

        @Bean
        public BeanToWireTo getBeanToWireTo(RequestAwarePredicate p, Date s) {
            return new BeanToWireTo(p, s);
        }

        @RequiredArgsConstructor
        @Getter
        class BeanToWireTo{
            private final RequestAwarePredicate predicate;
            private final Date date;
        }
    }

    @Nested
    //This has to be here so the nested class sees the parent context's configuration
    @ContextConfiguration(classes = ConfigurableNamedContextFactoryTest.MainContextConfiguration.class)
    class givenDependingBeans {

        @Test
        void wiringWorksInContextAndFromParentContext(ApplicationContext context) {

            ConfigurableNamedContextFactory<NamedContextFactory.Specification> factory = new ConfigurableNamedContextFactory( WiringConfig.class,"aa", "aa");

            factory.setApplicationContext(context);

            Map<String, WiringConfig.BeanToWireTo> ctx = factory.getInstances("ctx", WiringConfig.BeanToWireTo.class);
            WiringConfig.BeanToWireTo bean = factory.getInstance("ctx", WiringConfig.BeanToWireTo.class);
            assertThat(bean, is(not(nullValue())));
            assertThat(bean.getDate(), is(not(nullValue())));
            assertThat(bean.getPredicate(), is(not(nullValue())));

        }
    }


}
