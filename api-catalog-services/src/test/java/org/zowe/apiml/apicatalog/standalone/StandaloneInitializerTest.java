/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.standalone;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@ActiveProfiles("test")
class StandaloneInitializerTest {

    @Mock
    SpringApplication application;

    @Mock
    ConfigurableApplicationContext registry;

    @Autowired
    private StandaloneLoaderService standaloneLoaderService;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Test
    void testEventIsCalledOnlyOnce() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        mockContext(event, true);

        publisher.publishEvent(event);
        publisher.publishEvent(event);

        verify(standaloneLoaderService, times(1)).initializeCache();
    }

    @Test
    void notCalledIfStandaloneDisabled() {
        ApplicationReadyEvent event = mock(ApplicationReadyEvent.class);
        mockContext(event, false);

        publisher.publishEvent(event);

        verifyNoInteractions(standaloneLoaderService);
    }

    private ConfigurableApplicationContext mockContext(ApplicationReadyEvent event, boolean isStandalone) {
        ConfigurableApplicationContext ctx = mock(ConfigurableApplicationContext.class);
        ConfigurableEnvironment env = mock(ConfigurableEnvironment.class);
        when(event.getApplicationContext()).thenReturn(ctx);
        when(env.getProperty("apiml.catalog.standalone.enabled", "false")).thenReturn(String.valueOf(isStandalone));
        when(ctx.getEnvironment()).thenReturn(env);
        return ctx;
    }

    @Configuration
    @Profile("test")
    public static class TestConfiguration {

        @Bean
        public StandaloneLoaderService standaloneLoaderService() {
            return mock(StandaloneLoaderService.class);
        }

        @Bean
        public StandaloneInitializer getStandaloneInitializer(StandaloneLoaderService standaloneLoaderService) {
            return new StandaloneInitializer(standaloneLoaderService);
        }

    }

}
