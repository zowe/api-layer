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

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.Test;
import org.junit.jupiter.api.Nested;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationContextInitializedEvent;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.CustomBean;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class ExtensionsLoaderTest {

    @Autowired
    private ApplicationEventPublisher publisher;

    @MockBean
    private ExtensionsLoader extensionsLoader;

    @MockBean
    private ExtensionConfigReader reader;

    @Test
    void onEvent_ContextIsBadType() {
        ConfigurableApplicationContext registry = mock(ConfigurableApplicationContext.class);
        SpringApplication application = mock(SpringApplication.class);

        ApplicationContextInitializedEvent event = new ApplicationContextInitializedEvent(application, new String[]{}, registry);

        doCallRealMethod().when(extensionsLoader).onApplicationEvent(event);
        publisher.publishEvent(event);

        verify(extensionsLoader).onApplicationEvent(event);
        verify(registry, never()).containsBeanDefinition(anyString());
    }

    @Test
    void onEvent_ContextIsRightType() {
        AnnotationConfigApplicationContext context = (AnnotationConfigApplicationContext) new TestContextManager(this.getClass()).getTestContext().getApplicationContext();
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
    @Configuration
    public static class ExtensionsConfigMock {

    }
}
