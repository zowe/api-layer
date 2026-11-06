/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.DefaultEurekaServerConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.resources.ServerCodecs;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.discovery.config.EurekaConfig;
import org.zowe.apiml.product.eureka.web.MetadataFilterService;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.WrongMethodTypeException;
import java.util.Collections;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class ApimlInstanceRegistryTest {
    private ApimlInstanceRegistry apimlInstanceRegistry;

    private EurekaServerConfig serverConfig;
    private EurekaClientConfig clientConfig;
    private ServerCodecs serverCodecs;
    private EurekaClient eurekaClient;
    private InstanceRegistryProperties instanceRegistryProperties;
    private ApplicationContext appCntx;
    private InstanceInfo standardInstance;
    private EurekaConfig.Tuple tuple;
    private MetadataFilterService metadataFilterService;

    @BeforeEach
    void setUp() {
        standardInstance = getStandardInstance();
        serverConfig = new DefaultEurekaServerConfig();
        clientConfig = mock(EurekaClientConfig.class);
        serverCodecs = mock(ServerCodecs.class);
        eurekaClient = mock(DiscoveryClient.class);
        instanceRegistryProperties = mock(InstanceRegistryProperties.class);
        appCntx = mock(ApplicationContext.class);
        tuple = new EurekaConfig.Tuple("service*,hello");
        metadataFilterService = mock(MetadataFilterService.class);

        apimlInstanceRegistry = spy(new ApimlInstanceRegistry(
            serverConfig,
            clientConfig,
            serverCodecs,
            eurekaClient,
            instanceRegistryProperties,
            appCntx,
            tuple,
            metadataFilterService));

        ReflectionTestUtils.setField(apimlInstanceRegistry, "handleRegistrationMethod", constantHandle(null, 4));
        ReflectionTestUtils.setField(apimlInstanceRegistry, "register2ArgsMethodHandle", constantHandle(null, 3));
        ReflectionTestUtils.setField(apimlInstanceRegistry, "register3ArgsMethodHandle", constantHandle(null, 4));
        ReflectionTestUtils.setField(apimlInstanceRegistry, "handleCancelationMethod", constantHandle(null, 4));
    }

    @Nested
    class GivenReplacerTuple {
        @Nested
        class WhenChangeServiceId {
            @Test
            void thenChangeServicePrefix() {
                InstanceInfo info = apimlInstanceRegistry.changeServiceId(standardInstance);
                assertEquals("helloclient", info.getInstanceId());
                assertEquals("HELLOCLIENT", info.getAppName());
                assertEquals("helloclient", info.getVIPAddress());
                assertEquals("HELLOCLIENT", info.getAppGroupName());
                assertEquals("192.168.0.1", info.getIPAddr());
                assertEquals("localhost", info.getHostName());
                assertEquals(9090, info.getSecurePort());
                assertEquals("localhost", info.getSecureVipAddress());
            }
        }
    }
    private static Stream<Arguments> tuples() {
       return Stream.of(
           Arguments.of("service*,hello", "helloclient"),
           Arguments.of("service,hello", "helloclient"),
           Arguments.of("service*,hello*", "helloclient"),
           Arguments.of("service*,service", "serviceclient"),
           Arguments.of("service*", "serviceclient"),
           Arguments.of(",service", "serviceclient"),
           Arguments.of("service,", "serviceclient"),
           Arguments.of(null, "serviceclient"),
           Arguments.of("different*,hello", "serviceclient")
       );
    }

    @ParameterizedTest
    @MethodSource("tuples")
    void thenShouldRegister(String tuple, String expectedServiceIdInResult) {
        apimlInstanceRegistry = spy(new ApimlInstanceRegistry(
            serverConfig,
            clientConfig,
            serverCodecs,
            eurekaClient,
            instanceRegistryProperties,
            appCntx,
            new EurekaConfig.Tuple(tuple),
            metadataFilterService)
        );
        ReflectionTestUtils.setField(apimlInstanceRegistry, "register2ArgsMethodHandle", constantHandle(null, 3));
        ReflectionTestUtils.setField(apimlInstanceRegistry, "handleRegistrationMethod", constantHandle(null, 4));
        apimlInstanceRegistry.register(standardInstance, false);
        assertEquals(expectedServiceIdInResult, standardInstance.getInstanceId());
    }

    @ParameterizedTest
    @MethodSource("tuples")
    void thenShouldRegisterWithSecondMethod(String tuple, String expectedServiceIdInResult) {
        apimlInstanceRegistry = spy(new ApimlInstanceRegistry(
            serverConfig,
            clientConfig,
            serverCodecs,
            eurekaClient,
            instanceRegistryProperties,
            appCntx,
            new EurekaConfig.Tuple(tuple),
            metadataFilterService)
        );
        ReflectionTestUtils.setField(apimlInstanceRegistry, "register3ArgsMethodHandle", constantHandle(null, 4));
        ReflectionTestUtils.setField(apimlInstanceRegistry, "handleRegistrationMethod", constantHandle(null, 4));
        apimlInstanceRegistry.register(standardInstance, 1, false);
        assertEquals(expectedServiceIdInResult, standardInstance.getInstanceId());
    }


    @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenRegistrationMethodsFails {
            @ParameterizedTest
            @MethodSource("exceptions")
            void thenFirstMethodThrowsIllegalException(String tuple, Exception exception) throws Throwable {
                apimlInstanceRegistry = spy(new ApimlInstanceRegistry(
                    serverConfig,
                    clientConfig,
                    serverCodecs,
                    eurekaClient,
                    instanceRegistryProperties,
                    appCntx,
                    new EurekaConfig.Tuple(tuple),
                    metadataFilterService)
                );
                ReflectionTestUtils.setField(apimlInstanceRegistry, "register2ArgsMethodHandle",
                    throwingHandle(exception, 3));
                IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                    apimlInstanceRegistry.register(standardInstance, false);
                });
                assertSame(exception, thrown.getCause());
            }

            @Test
            void thenFirstMethodThrowRuntimeException() throws Throwable {
                apimlInstanceRegistry = spy(new ApimlInstanceRegistry(
                    serverConfig,
                    clientConfig,
                    serverCodecs,
                    eurekaClient,
                    instanceRegistryProperties,
                    appCntx,
                    new EurekaConfig.Tuple("service*,hello"),
                    metadataFilterService)
                );
                RuntimeException exception = new RuntimeException();
                ReflectionTestUtils.setField(apimlInstanceRegistry, "register2ArgsMethodHandle",
                    throwingHandle(exception, 3));
                RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                    apimlInstanceRegistry.register(standardInstance, false);
                });
                assertSame(exception, thrown);
            }

            @ParameterizedTest
            @MethodSource("exceptions")
            void thenSecondMethodThrowsIllegalException(String tuple, Exception exception) throws Throwable {
                apimlInstanceRegistry = spy(new ApimlInstanceRegistry(
                    serverConfig,
                    clientConfig,
                    serverCodecs,
                    eurekaClient,
                    instanceRegistryProperties,
                    appCntx,
                    new EurekaConfig.Tuple(tuple),
                    metadataFilterService)
                );
                ReflectionTestUtils.setField(apimlInstanceRegistry, "register3ArgsMethodHandle",
                    throwingHandle(exception, 4));
                IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                    apimlInstanceRegistry.register(standardInstance, 1, false);
                });
                assertSame(exception, thrown.getCause());
            }

            @Test
            void thenSecondMethodThrowRuntimeException() throws Throwable {
                apimlInstanceRegistry = spy(new ApimlInstanceRegistry(
                    serverConfig,
                    clientConfig,
                    serverCodecs,
                    eurekaClient,
                    instanceRegistryProperties,
                    appCntx,
                    new EurekaConfig.Tuple("service*,hello"),
                    metadataFilterService)
                );
                RuntimeException exception = new RuntimeException();
                ReflectionTestUtils.setField(apimlInstanceRegistry, "register3ArgsMethodHandle",
                    throwingHandle(exception, 4));
                RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                    apimlInstanceRegistry.register(standardInstance, 1, false);
                });
                assertSame(exception, thrown);
            }

            private Stream<Arguments> exceptions() {
                return Stream.of(
                    Arguments.of("service*,hello", new WrongMethodTypeException()),
                    Arguments.of("service*,hello", new Exception(new Throwable()))
                );
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenResolveInstanceRewrittenFails {
            @ParameterizedTest
            @MethodSource("exceptions")
            void thenThrowIllegalArgumentException(Exception exception) throws Throwable {
                ReflectionTestUtils.setField(apimlInstanceRegistry, "handlerResolveInstanceLeaseDurationMethod",
                    throwingHandle(exception, 2));
                IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                    apimlInstanceRegistry.resolveInstanceLeaseDurationRewritten(standardInstance);
                });
                assertSame(exception, thrown.getCause());
            }

            @Test
            void thenThrowRuntimeException() throws Throwable {
                RuntimeException exception = new RuntimeException();
                ReflectionTestUtils.setField(apimlInstanceRegistry, "handlerResolveInstanceLeaseDurationMethod",
                    throwingHandle(exception, 2));
                RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                    apimlInstanceRegistry.resolveInstanceLeaseDurationRewritten(standardInstance);
                });
                assertSame(exception, thrown);
            }

            private Stream<Arguments> exceptions() {
                return Stream.of(
                    Arguments.of(new WrongMethodTypeException()),
                    Arguments.of(new Exception(new Throwable()))
                );
            }
        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenCancelRegistration {
            @Test
            void thenIsSuccessful() throws Throwable {
                ReflectionTestUtils.setField(apimlInstanceRegistry, "cancelMethodHandle", constantHandle(true, 4));
                apimlInstanceRegistry.register(standardInstance, false);
                verify(apimlInstanceRegistry, times(1)).changeServiceId(any());
                boolean isCancelled = apimlInstanceRegistry.cancel("HELLO", "hello", false);
                assertTrue(isCancelled);
            }

            @ParameterizedTest
            @MethodSource("exceptions")
            void thenThrowIllegalArgumentException(Exception exception) throws Throwable {
                ReflectionTestUtils.setField(apimlInstanceRegistry, "cancelMethodHandle",
                    throwingHandle(exception, 4));
                IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () -> {
                    apimlInstanceRegistry.cancel("HELLO", "hello", false);
                });
                assertSame(exception, thrown.getCause());
            }

            @Test
            void thenThrowRuntimeException() throws Throwable {
                RuntimeException exception = new RuntimeException();
                ReflectionTestUtils.setField(apimlInstanceRegistry, "cancelMethodHandle",
                    throwingHandle(exception, 4));
                RuntimeException thrown = assertThrows(RuntimeException.class, () -> {
                    apimlInstanceRegistry.cancel("HELLO", "hello", false);
                });
                assertSame(exception, thrown);
            }

            private Stream<Arguments> exceptions() {
                return Stream.of(
                    Arguments.of(new WrongMethodTypeException()),
                    Arguments.of(new Exception(new Throwable()))
                );
            }
        }


    private static MethodHandle constantHandle(Object result, int argumentCount) {
        return MethodHandles.dropArguments(MethodHandles.constant(Object.class, result), 0,
            Collections.nCopies(argumentCount, Object.class));
    }

    private static MethodHandle throwingHandle(Exception exception, int argumentCount) {
        MethodHandle handle = MethodHandles.throwException(Object.class, Exception.class).bindTo(exception);
        return MethodHandles.dropArguments(handle, 0, Collections.nCopies(argumentCount, Object.class));
    }

    private InstanceInfo getStandardInstance() {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId("serviceclient")
            .setAppName("SERVICECLIENT")
            .setAppGroupName("SERVICECLIENT")
            .setIPAddr("192.168.0.1")
            .enablePort(InstanceInfo.PortType.SECURE, true)
            .setSecurePort(9090)
            .setHostName("localhost")
            .setSecureVIPAddress("localhost")
            .setVIPAddress("serviceclient")
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .build();
    }
}
