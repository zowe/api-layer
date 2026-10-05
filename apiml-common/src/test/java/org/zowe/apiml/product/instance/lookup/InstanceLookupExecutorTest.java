/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.instance.lookup;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.constants.EurekaMetadataDefinition;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.instance.InstanceInitializationException;
import org.zowe.apiml.product.instance.InstanceNotFoundException;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static java.time.Duration.ofMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTimeout;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.REGISTRATION_TYPE;

class InstanceLookupExecutorTest {

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UsageInApiCatalog {

        private static final String SERVICE_ID = CoreService.API_CATALOG.getServiceId();

        @Mock private DiscoveryClient discoveryClient;

        private InstanceLookupExecutor instanceLookupExecutor;
        private List<ServiceInstance> instances;

        private Exception lastException;
        private ServiceInstance lastInstanceInfo;
        private CountDownLatch latch;

        private ServiceInstance getInstance(String serviceId) {
            return createInstance(serviceId, serviceId, new HashMap<>());
        }

        ServiceInstance createInstance(String serviceId, String instanceId, HashMap<String, String> metadata) {
            return new DefaultServiceInstance(instanceId, serviceId, "hostname", 9090, true, metadata);
        }

        @BeforeEach
        void setUp() {
            instanceLookupExecutor = new InstanceLookupExecutor(discoveryClient);
            instances = Collections.singletonList(
                getInstance(SERVICE_ID));
            latch = new CountDownLatch(1);
        }

        @Test
        void testRun_whenNoApplicationRegisteredInDiscovery() {
            assertTimeout(ofMillis(2000), () -> {
                instanceLookupExecutor.run(
                    SERVICE_ID, null,
                    (exception, isStopped) -> {
                        lastException = exception;
                        latch.countDown();
                    }
                );

                latch.await();
            });

            assertNotNull(lastException);
            assertInstanceOf(InstanceNotFoundException.class, lastException);
            assertEquals("Service '" + SERVICE_ID + "' is not registered to Discovery Service",
                lastException.getMessage());
        }

        @Test
        void testRun_whenNoInstancesExistInDiscovery() {
            assertTimeout(ofMillis(2000), () -> {
                when(discoveryClient.getServices()).thenReturn(List.of(SERVICE_ID));
                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(List.of());

                instanceLookupExecutor.run(
                    SERVICE_ID, null,
                    (exception, isStopped) -> {
                        lastException = exception;
                        latch.countDown();
                    }
                );

                latch.await();
            });

            assertNotNull(lastException);
            assertInstanceOf(InstanceNotFoundException.class, lastException);
            assertEquals("'" + SERVICE_ID + "' has no running instances registered to Discovery Service",
                lastException.getMessage());
        }

        @Test
        void testRun_whenUnexpectedExceptionHappened() {
            assertTimeout(ofMillis(2000), () -> {
                when(discoveryClient.getServices()).thenReturn(List.of(SERVICE_ID));
                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenThrow(new InstanceInitializationException("Unexpected Exception"));

                instanceLookupExecutor.run(
                    SERVICE_ID, null,
                    (exception, isStopped) -> {
                        lastException = exception;
                        latch.countDown();
                    }
                );

                latch.await();
            });

            assertNotNull(lastException);
            assertInstanceOf(InstanceInitializationException.class, lastException);
        }

        @Test
        void testRun_whenInstanceExistInDiscovery() {
            assertTimeout(ofMillis(2000), () -> {
                when(discoveryClient.getServices()).thenReturn(List.of(SERVICE_ID));
                when(discoveryClient.getInstances(SERVICE_ID))
                    .thenReturn(instances);

                instanceLookupExecutor.run(
                    SERVICE_ID,
                    instanceInfo -> {
                        lastInstanceInfo = instanceInfo;
                        latch.countDown();
                    }, null
                );

                latch.await();
            });

            assertNull(lastException);
            assertNotNull(lastInstanceInfo);
            assertEquals(instances.get(0), lastInstanceInfo);
        }

    }

    @Nested
    class MultiTenancy {

        private final DiscoveryClient discoveryClient = mock(DiscoveryClient.class);
        private final InstanceLookupExecutor instanceLookupExecutor = new InstanceLookupExecutor(discoveryClient);

        @SuppressWarnings("unchecked")
        private final Consumer<ServiceInstance> successHandler = mock(Consumer.class);
        @SuppressWarnings("unchecked")
        private final BiConsumer<Exception, Boolean> failureHandler = mock(BiConsumer.class);

        ServiceInstance mockGateway(EurekaMetadataDefinition.RegistrationType registrationType) {
            Map<String, String> metadata = new HashMap<>();
            if (registrationType != null) {
                metadata.put(REGISTRATION_TYPE, registrationType.getValue());
            }
            ServiceInstance instanceInfo = new DefaultServiceInstance(
                CoreService.GATEWAY.getServiceId() + ":localhost:" + (1 + new Random().nextInt() % 65535),
                CoreService.GATEWAY.getServiceId(), "localhost", 10010, true, metadata);

            when(discoveryClient.getServices()).thenReturn(List.of(CoreService.GATEWAY.getServiceId()));
            when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
                List.of(instanceInfo)
            );
            return instanceInfo;
        }

        private void invokeRun() {
            instanceLookupExecutor.run(CoreService.GATEWAY.getServiceId(), successHandler, failureHandler);
        }

        @Test
        void givenNoInstance_whenRun_thenInvokeFailureHandler() {
            invokeRun();
            verify(successHandler, never()).accept(any());
            verify(failureHandler).accept(any(), eq(false));
        }

        @Test
        void givenOnlyAdditionalInstances_whenRun_thenInvokeFailureHandler() {
            mockGateway(EurekaMetadataDefinition.RegistrationType.ADDITIONAL);
            invokeRun();
            verify(successHandler, never()).accept(any());
            verify(failureHandler).accept(any(), eq(false));
        }

        @Test
        void givenOnlyPrimaryInstances_whenRun_thenInvokeSuccessHandler() {
            var primary = mockGateway(EurekaMetadataDefinition.RegistrationType.PRIMARY);
            invokeRun();
            verify(successHandler).accept(primary);
            verify(failureHandler, never()).accept(any(), anyBoolean());
        }

        @Test
        void givenMultipleInstances_whenRun_thenInvokeSuccessHandler() {
            var primary = mockGateway(EurekaMetadataDefinition.RegistrationType.PRIMARY);
            var additional = mockGateway(EurekaMetadataDefinition.RegistrationType.ADDITIONAL);
            when(discoveryClient.getInstances(CoreService.GATEWAY.getServiceId())).thenReturn(
                List.of(primary, additional)
            );
            invokeRun();
            verify(successHandler).accept(primary);
            verify(failureHandler, never()).accept(any(), anyBoolean());
        }

    }

}
