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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.context.named.NamedContextFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.ResolvableType;
import org.springframework.core.env.MapPropertySource;
import org.springframework.test.context.ContextConfiguration;

import java.time.Duration;
import java.util.*;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

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

    private static class ConditionalBeanConfig {

        public class BeanClass{}

        @Bean
        @ConditionalOnProperty(name = "variable", havingValue = "true")
        public BeanClass beanClass1() {
            return new BeanClass();
        }

        @Bean
        @ConditionalOnProperty(name = "variable", havingValue = "false")
        public BeanClass beanClass2() {
            return new BeanClass();
        }

        @Bean
        @ConditionalOnProperty(name = "missing.variable", havingValue = "false")
        public BeanClass beanClass3() {
            return new BeanClass();
        }
    }

    @Nested
    class GivenProperties {
        @Test
        void beansConditionalConstructWorks() {

            ConfigurableNamedContextFactory<NamedContextFactory.Specification> underTest = new ConfigurableNamedContextFactory( ConditionalBeanConfig.class,"aa", "aa");

            underTest.addInitializer("ctx", context -> {
                context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("PropertySouceName", Collections.singletonMap("variable", "false"))
                );
            });

            Map<String, ConditionalBeanConfig.BeanClass> beans = underTest.getInstances("ctx", ConditionalBeanConfig.BeanClass.class);
            assertThat(beans.size(), is(1));
            assertThat(beans.keySet(), contains("beanClass2"));

            underTest.addInitializer("ctx2", context -> {
                context.getEnvironment().getPropertySources().addFirst(
                    new MapPropertySource("PropertySouceName", Collections.singletonMap("missing.variable", "false"))
                );
            });
            Map<String, ConditionalBeanConfig.BeanClass> beans2 = underTest.getInstances("ctx2", ConditionalBeanConfig.BeanClass.class);
            assertThat(beans2.size(), is(1));
            assertThat(beans2.keySet(), contains("beanClass3"));
        }
    }

    private static class SpecConfigDefault {
        @Bean
        public Duration getDay(){
            return Duration.ofDays(1);
        }
    }

    private static class SpecConfigSpecific {
        @Bean
        public Duration get2Days(){
            return Duration.ofDays(2);
        }
    }

    @Nested
    class givenSpecifications{
        @Test
        void defaultConfigBehaviorPlusSpecificTakesPrecedence() {
            NamedContextFactory.Specification spec1 = mock(NamedContextFactory.Specification.class);
            doReturn("default.").when(spec1).getName();
            doReturn(new Class[] {SpecConfigDefault.class}).when(spec1).getConfiguration();

            NamedContextFactory.Specification spec2 = mock(NamedContextFactory.Specification.class);
            doReturn("specificContext").when(spec2).getName();
            doReturn(new Class[] {SpecConfigSpecific.class}).when(spec2).getConfiguration();

            ConfigurableNamedContextFactory<NamedContextFactory.Specification> underTest = new ConfigurableNamedContextFactory( null,"aa", "aa");
            underTest.setConfigurations(Arrays.asList(spec1, spec2));

            Duration someRandomContextDuration = underTest.getInstance("someRandomContext", Duration.class);
            assertThat(someRandomContextDuration.toDays(),is(1L));

            //TODO this needs some resolution
            assertThat(underTest.getInstance("specificContext", ResolvableType.forType(Duration.class)), is(not(nullValue())));
            //assertThat(underTest.getInstance("specificContext", Duration.class), is(not(nullValue())));

            Map<String, Duration> beans = underTest.getInstances("specificContext", Duration.class);
            assertThat(beans.size(), is(2));
            assertThat(beans.keySet(), contains("getDay", "get2Days"));
        }
    }

    private static class ContextProbe {

        public class BeanClass{
            @Getter
            @Value("${context.name}")
            private String name;
        }

        @Bean
        public Date canAutowirePropertyResolver(PropertyPlaceholderAutoConfiguration ppac) {
            return new Date();
        }

        @Bean
        public BeanClass beanThatCanAutowireConfiguredContextName() {
            return new BeanClass();
        }
    }

    @Nested
    class AssertContextProperties {

        @Test
        void contextHasAllRequiredElements() {
            ConfigurableNamedContextFactory<NamedContextFactory.Specification> underTest = new ConfigurableNamedContextFactory( ContextProbe.class,"contextConfig", "context.name");
            ContextProbe.BeanClass bean = underTest.getInstance("ctx", ContextProbe.BeanClass.class);
            assertThat(bean.getName(), is("ctx"));
        }
    }

    private static class WiringConfig {

        public class BeanClass{}

        @Bean
        public BeanClass getBeanClass() {
            return new BeanClass();
        }

        @Bean
        public BeanToWireTo getBeanToWireTo(BeanClass beanFromContext, Date beanFromParentContext) {
            return new BeanToWireTo(beanFromContext, beanFromParentContext);
        }

        @RequiredArgsConstructor
        @Getter
        class BeanToWireTo{
            private final BeanClass bean;
            private final Date date;
        }
    }

    @Nested
    //This has to be here so the nested class sees the parent context's configuration
    @ContextConfiguration(classes = ConfigurableNamedContextFactoryTest.MainContextConfiguration.class)
    class givenDependingBeans {

        @Test
        void wiringWorksInContextAndFromParentContext(ApplicationContext context) {

            ConfigurableNamedContextFactory<NamedContextFactory.Specification> underTest = new ConfigurableNamedContextFactory( WiringConfig.class,"aa", "aa");
            underTest.setApplicationContext(context);

            WiringConfig.BeanToWireTo bean = underTest.getInstance("ctx", WiringConfig.BeanToWireTo.class);
            assertThat(bean, is(not(nullValue())));
            assertThat(bean.getDate(), is(not(nullValue())));
            assertThat(bean.getBean(), is(not(nullValue())));
        }
    }


}
