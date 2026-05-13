/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.staticdef;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.eureka.EurekaServerContext;
import com.netflix.eureka.EurekaServerContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.discovery.metadata.MetadataDefaultsService;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.discovery.StaticRegistrationResult;

import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticServicesRegistrationServiceTest {

    @Mock(strictness = Mock.Strictness.LENIENT)
    private ApimlInstanceRegistry mockRegistry;

    @Mock(strictness = Mock.Strictness.LENIENT)
    EurekaServerContext mockEurekaServerContext;

    @BeforeEach
    void setUp() {
        when(mockEurekaServerContext.getRegistry()).thenReturn(mockRegistry);
        EurekaServerContextHolder.initialize(mockEurekaServerContext);
    }

    @Nested
    class Loading {

        @Test
        void testFindServicesInDirectoryNoFiles() throws URISyntaxException {
            EurekaServerContextHolder.initialize(mockEurekaServerContext);
            ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

            StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
            String apiDefsDirectory = Paths.get(ClassLoader.getSystemResource("api-defs-empty/").toURI()).toAbsolutePath().toString();
            StaticRegistrationResult result = registrationService.registerServices(apiDefsDirectory);
            assertEquals(0, result.getInstances().size());
        }

        @Test
        void testFindServicesInDirectoryOneFile() throws URISyntaxException {
            ServiceDefinitionProcessor serviceDefinitionProcessor = new ServiceDefinitionProcessor();

            StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
            String apiDefsDirectory = Paths.get(ClassLoader.getSystemResource("api-defs/").toURI()).toAbsolutePath().toString();
            StaticRegistrationResult result = registrationService.registerServices(apiDefsDirectory);

            assertEquals(4, result.getInstances().size());
        }

    }

    @Nested
    class Registration {

        private StaticRegistrationResult createResult(InstanceInfo... instances) {
            StaticRegistrationResult out = new StaticRegistrationResult();
            out.getInstances().addAll(Arrays.asList(instances));
            return out;
        }

        @Test
        void testGetStaticInstances() {
            ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
            StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());

            List<InstanceInfo> instances = registrationService.getStaticInstances();

            assertEquals(0, instances.size());
            verify(serviceDefinitionProcessor, times(0)).findStaticServicesData(any(String.class));
        }

        @Test
        void testGetStaticInstancesAfterRegister() {
            String directory = "directory";
            String service = "service";
            ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
            when(serviceDefinitionProcessor.findStaticServicesData(directory)).thenReturn(createResult(
                InstanceInfo.Builder.newBuilder().setAppName(service).build()));

            StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
            registrationService.registerServices(directory);
            List<InstanceInfo> instances = registrationService.getStaticInstances();

            assertEquals(1, instances.size());
            assertEquals(service.toUpperCase(), instances.get(0).getAppName());
            verify(serviceDefinitionProcessor, times(1)).findStaticServicesData(directory);
        }

        @Test
        void testReloadServicesWithUnregisteringService() {
            String service = "service";
            ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
            InstanceInfo instance = InstanceInfo.Builder.newBuilder().setInstanceId(service).setAppName(service).build();

            when(serviceDefinitionProcessor.findStaticServicesData(null))
                .thenReturn(createResult(instance))
                .thenReturn(createResult());

            StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
            registrationService.reloadServices();
            StaticRegistrationResult result = registrationService.reloadServices();

            assertThat(result.getRegisteredServices().contains(service), is(false));
            verify(serviceDefinitionProcessor, times(2)).findStaticServicesData(null);
            verify(mockRegistry, times(1)).cancel(instance.getAppName(), instance.getId(), false);
        }

        @Test
        void testReloadServicesWithAddingNewService() {
            String serviceA = "serviceA";
            String serviceB = "serviceB";
            ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
            InstanceInfo instanceA = InstanceInfo.Builder.newBuilder().setInstanceId(serviceA).setAppName(serviceA).build();
            InstanceInfo instanceB = InstanceInfo.Builder.newBuilder().setInstanceId(serviceB).setAppName(serviceB).build();
            when(serviceDefinitionProcessor.findStaticServicesData(null))
                .thenReturn(createResult(instanceA))
                .thenReturn(createResult(instanceA, instanceB));

            StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
            registrationService.reloadServices();
            StaticRegistrationResult result = registrationService.reloadServices();

            assertThat(result.getRegisteredServices().contains(serviceA), is(true));
            assertThat(result.getRegisteredServices().contains(serviceB), is(true));
            verify(serviceDefinitionProcessor, times(2)).findStaticServicesData(null);
            verify(mockRegistry, times(0)).cancel(any(String.class), any(String.class), eq(false));
        }

        @Test
        void testRenewInstances() {
            String directory = "directory";
            String service = "service";
            InstanceInfo instance = InstanceInfo.Builder.newBuilder().setInstanceId(service).setAppName(service).build();
            ServiceDefinitionProcessor serviceDefinitionProcessor = mock(ServiceDefinitionProcessor.class);
            when(serviceDefinitionProcessor.findStaticServicesData(directory)).thenReturn(createResult(instance));

            StaticServicesRegistrationService registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
            registrationService.registerServices(directory);

            verify(mockRegistry, times(1)).registerStatically(instance, false, false);
        }

    }

    @Nested
    class EurekaErrors {

        @Mock
        private ApimlLogger apimlLogger;

        @Mock
        private Message testMessage;

        @Mock
        private ServiceDefinitionProcessor serviceDefinitionProcessor;

        private StaticServicesRegistrationService service;

        @BeforeEach
        void setUp() {
            service = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
            ReflectionTestUtils.setField(service, "apimlLog", apimlLogger);
        }

        @Test
        void givenError_whenRegister_thenStoreItInTheResult() {
            // simulate new findings
            var sourceSearchResult = new StaticRegistrationResult();
            sourceSearchResult.getInstances().add(mock(InstanceInfo.class));
            doReturn(sourceSearchResult).when(serviceDefinitionProcessor).findStaticServicesData(any());

            doThrow(new RuntimeException("CannotCancelRegistration")).when(mockRegistry).registerStatically(any(), anyBoolean(), anyBoolean());
            doReturn(testMessage).when(apimlLogger).log(eq("org.zowe.apiml.discovery.staticDefinitionRegistration"), any(), any());

            var result = service.registerServices(null);
            assertEquals(1, result.getErrors().size());
            assertSame(testMessage, result.getErrors().get(0));
        }

        @Test
        void givenError_whenCancel_thenStoreItInTheResult() {
            service.getStaticInstances().add(mock(InstanceInfo.class)); // simulate a previous registration

            doReturn(testMessage).when(apimlLogger).log(eq("org.zowe.apiml.discovery.staticDefinitionRegistration"), any(), any());
            service = spy(service);
            doReturn(new StaticRegistrationResult()).when(service).registerServices(any());
            doThrow(new RuntimeException("CannotCancelRegistration")).when(mockRegistry).cancel(any(), any(), anyBoolean());

            var result = service.reloadServices();
            assertEquals(1, result.getErrors().size());
            assertSame(testMessage, result.getErrors().get(0));
        }

    }

    @Nested
    class UnexpectedErrors {

        @Mock
        private ServiceDefinitionProcessor serviceDefinitionProcessor;

        @Nested
        class CatchingException {

            @Test
            void givenUnexpectedError_whenRegisterServices_thenStoreInContextAndReturn() {
                var registrationService = new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService());
                doThrow(new RuntimeException("UnexpectedError")).when(serviceDefinitionProcessor).findStaticServicesData(any());
                var result = registrationService.registerServices(null);
                assertFalse(result.getErrors().isEmpty());
            }

        }

        @Nested
        class Logging {

            private Logger logger;

            @Mock
            private Appender<ILoggingEvent> mockedAppender;

            @Captor
            private ArgumentCaptor<LoggingEvent> loggingCaptor;

            private StaticServicesRegistrationService registrationService;

            @BeforeEach
            void setUp() {
                logger = (Logger) LoggerFactory.getLogger(StaticServicesRegistrationService.class);
                logger.detachAndStopAllAppenders();
                logger.getLoggerContext().resetTurboFilterList();
                logger.addAppender(mockedAppender);
                logger.setLevel(Level.TRACE);

                registrationService = spy(new StaticServicesRegistrationService(serviceDefinitionProcessor, new MetadataDefaultsService()));
            }

            @AfterEach
            void tearDown() {
                logger.detachAppender(mockedAppender);
            }

            @Test
            void givenUnexpectedError_whenRegisterServices_thenLog() {
                doThrow(new RuntimeException("UnexpectedError")).when(registrationService).registerServices(any());
                registrationService.registerServices();

                verify(mockedAppender, atLeast(1)).doAppend(loggingCaptor.capture());
                assertTrue(loggingCaptor.getValue().getThrowableProxy().getMessage().contains("UnexpectedError"));
            }

            @Test
            void givenError_whenRegisterServices_thenLogWithErrorLevel() {
                var result = new StaticRegistrationResult();
                result.getErrors().add(null);
                doReturn(result).when(registrationService).registerServices(any());

                registrationService.registerServices();

                verify(mockedAppender, atLeast(1)).doAppend(loggingCaptor.capture());
                assertEquals(Level.ERROR, loggingCaptor.getValue().getLevel());
            }

            @Test
            void givenSuccessfulResult_whenRegisterServices_thenLogWithDebugLevel() {
                doReturn(new StaticRegistrationResult()).when(registrationService).registerServices(any());

                registrationService.registerServices();

                verify(mockedAppender, atLeast(1)).doAppend(loggingCaptor.capture());
                assertEquals(Level.DEBUG, loggingCaptor.getValue().getLevel());
            }

        }

    }

}
