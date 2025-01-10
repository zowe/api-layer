/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.extension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.CustomBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration
@ActiveProfiles("test")
class ExtensionsLoaderTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @MockBean
    private ExtensionsLoader extensionsLoader;

    @MockBean
    private ExtensionConfigReader reader;

    @AfterEach
    void tearDown() {
        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) new TestContextManager(this.getClass()).getTestContext().getApplicationContext();
        if (context.containsBeanDefinition("customBean")) {
            context.removeBeanDefinition("customBean");
        }
    }

    @Nested
    class WhenLoadingExtensions {
        @Nested
        class GivenContextIsBadType {

            @Test
            void itDoesntModifyContext() {
                ConfigurableApplicationContext registry = mock(ConfigurableApplicationContext.class);
                SpringApplication application = mock(SpringApplication.class);

                ApplicationContextInitializedEvent event = new ApplicationContextInitializedEvent(application, new String[]{}, registry);

                doCallRealMethod().when(extensionsLoader).onApplicationEvent(event);
                publisher.publishEvent(event);

                verify(extensionsLoader).onApplicationEvent(event);
                verify(registry, never()).containsBeanDefinition(anyString());
            }
        }

        @Nested
        class GivenContextIsRightType {
            @Nested
            class AndNoExtensionsAreConfigured {

                @Test
                void itDoesntModifyContext() {
                    ConfigurableApplicationContext registry = mock(ConfigurableApplicationContext.class);
                    when(reader.getBasePackages()).thenReturn(new String[]{ });
                    ReflectionTestUtils.setField(extensionsLoader, "configReader", reader);
                    SpringApplication application = mock(SpringApplication.class);
                    ApplicationContextInitializedEvent event = new ApplicationContextInitializedEvent(application, new String[]{}, registry);
                    doCallRealMethod().when(extensionsLoader).onApplicationEvent(event);
                    publisher.publishEvent(event);
                    verify(registry, never()).containsBeanDefinition(anyString());
                }
            }

            @Nested
            class AndExtensionIsConfigured {

                @Test
                void itRegistersNewBeans() {
                    AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) new TestContextManager(ExtensionsLoaderTest.this.getClass()).getTestContext().getApplicationContext();
                    ReflectionTestUtils.setField(extensionsLoader, "configReader", reader);

                    try {
                        assertNull(context.getBean(CustomBean.class));
                    } catch (Exception e) {
                        // ignored, it's expected that this bean does not exist at this point
                    }

                    SpringApplication application = mock(SpringApplication.class);
                    ApplicationContextInitializedEvent event = new ApplicationContextInitializedEvent(application, new String[]{}, context);

                    when(reader.getBasePackages()).thenReturn(new String[]{ "org.zowe" });
                    doCallRealMethod().when(extensionsLoader).onApplicationEvent(event);
                    publisher.publishEvent(event);
                    verify(extensionsLoader).onApplicationEvent(event);

                    try {
                        assertNotNull(context.getBean(CustomBean.class));
                    } catch (Exception e) {
                        fail("Expected bean registration", e);
                    }
                }

                @Nested
                class AndBeanIsAlreadyRegisteredInContext {

                    @Test
                    void itDoesntReplaceIt() {
                        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) new TestContextManager(ExtensionsLoaderTest.this.getClass()).getTestContext().getApplicationContext();
                        BeanDefinitionRegistry registry = spy(context);
                        ReflectionTestUtils.setField(extensionsLoader, "configReader", reader);
                        context.registerBean(CustomBean.class);
                        SpringApplication application = mock(SpringApplication.class);
                        ApplicationContextInitializedEvent event = new ApplicationContextInitializedEvent(application, new String[]{}, context);
                        when(reader.getBasePackages()).thenReturn(new String[]{ "org.zowe" });
                        doCallRealMethod().when(extensionsLoader).onApplicationEvent(event);
                        publisher.publishEvent(event);
                        verify(extensionsLoader).onApplicationEvent(event);
                        verify(registry, never()).registerBeanDefinition(anyString(), any());
                    }
                }
            }
        }
    }

    @Profile("test")
    @Configuration
    public static class ExtensionsConfigMock {

    }

}
