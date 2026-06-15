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

import com.netflix.appinfo.*;
import com.netflix.discovery.EurekaClient;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.eureka.DefaultEurekaServerConfig;
import com.netflix.eureka.EurekaServerConfig;
import com.netflix.eureka.cluster.PeerEurekaNodes;
import com.netflix.eureka.lease.Lease;
import com.netflix.eureka.resources.ServerCodecs;
import com.netflix.eureka.transport.EurekaServerHttpClientFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.netflix.eureka.server.InstanceRegistryProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.discovery.config.EurekaConfig;
import org.zowe.apiml.discovery.metadata.MetadataFilterService;

import java.lang.invoke.MethodHandle;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApimlInstanceRegistryTest {

    private ApimlInstanceRegistry apimlInstanceRegistry;

    @Mock private EurekaClientConfig clientConfig;
    @Mock private ServerCodecs serverCodecs;
    @Mock private EurekaClient eurekaClient; // DiscoveryClient?
    @Mock private EurekaServerHttpClientFactory eurekaServerHttpClientFactory;
    @Mock private InstanceRegistryProperties instanceRegistryProperties;
    @Mock private ApplicationContext appCntx;
    @Mock private MetadataFilterService metadataFilterService;
    @Mock private EurekaInstanceConfig eurekaInstanceConfig;
    @Mock private PeerEurekaNodes peerEurekaNodes;
    private InstanceInfo standardInstance;

    private EurekaServerConfig serverConfig;

    @BeforeEach
    @SuppressWarnings("squid:S1874")
    void setUp() throws Exception {
        standardInstance = getStandardInstance("hostname:serviceclient:10010", "serviceclient");
        serverConfig = new DefaultEurekaServerConfig();

        apimlInstanceRegistry = spy(init(new ApimlInstanceRegistry(
            serverConfig,
            clientConfig,
            serverCodecs,
            eurekaClient,
            eurekaServerHttpClientFactory,
            instanceRegistryProperties,
            appCntx,
            new EurekaConfig.Tuple("service*,hello"), metadataFilterService)));

        doReturn("zowe").when(eurekaInstanceConfig).getNamespace();
        doReturn("discovery").when(eurekaInstanceConfig).getAppname();
        doReturn(new MyDataCenterInfo(DataCenterInfo.Name.MyOwn)).when(eurekaInstanceConfig).getDataCenterInfo();
        ApplicationInfoManager.getInstance().initComponent(eurekaInstanceConfig);
    }

    private ApimlInstanceRegistry init(ApimlInstanceRegistry apimlInstanceRegistry) throws Exception {
        apimlInstanceRegistry.initializedResponseCache();
        apimlInstanceRegistry.init(peerEurekaNodes);
        apimlInstanceRegistry.setApplicationContext(appCntx);
        return apimlInstanceRegistry;
    }

    @Nested
    class GivenInvalidServiceId {

        private static Stream<Arguments> instanceIds() {
            return Stream.of(
                Arguments.of( "hostname:service_client:10010", "Service-Client"),
                Arguments.of( "hostname:10010", "serviceclient"),
                Arguments.of( "hostname:ServiceClient:10010", "serviceClient"),
                Arguments.of( "hostname:serviceclient:10010", "serviceClient"),
                Arguments.of( "hostname:service_client:10010", "service_client"),
                Arguments.of( "hostname:service_client:10010", ""),
                Arguments.of( "hostname:10010", "service_client"),
                Arguments.of( "hostname:service_client:10010", "different_service_client"),
                Arguments.of( "hostname:-serviceclient:10010", "-serviceclient"),
                Arguments.of( "hostname:serviceclient-:10010", "serviceclient-"),
                Arguments.of( "hostname:invalidserviceidididididididididididdididididididididididdidididididididididid:10010", "invalidserviceidididididididididididdididididididididididdidididididididididid"),
                Arguments.of( null, "service")
            );
        }

        //we cannot fail for non-conformant services for backwards compatibility
        @ParameterizedTest
        @MethodSource("instanceIds")
        void thenOnboard(String instanceId, String appName) throws Exception {
            InstanceInfo wrongInstance = getStandardInstance(instanceId, appName);

            apimlInstanceRegistry = spy(init(new ApimlInstanceRegistry(
                serverConfig,
                clientConfig,
                serverCodecs,
                eurekaClient,
                eurekaServerHttpClientFactory,
                instanceRegistryProperties,
                appCntx,
                new EurekaConfig.Tuple(""), metadataFilterService)));
            assertDoesNotThrow( () ->
                apimlInstanceRegistry.register(wrongInstance, 1, false)
            );

        }
    }

    @Nested
    class GivenValidServiceIdWithDash {

        @Test
        void thenShouldRegister() throws Exception {
            standardInstance = getStandardInstance("hostname:service-client:10010", "service-client");
            apimlInstanceRegistry = spy(init(new ApimlInstanceRegistry(
                serverConfig,
                clientConfig,
                serverCodecs,
                eurekaClient,
                eurekaServerHttpClientFactory,
                instanceRegistryProperties,
                appCntx,
                new EurekaConfig.Tuple(null), metadataFilterService)));
            assertDoesNotThrow(() -> apimlInstanceRegistry.register(standardInstance, false));
        }

    }

    @Nested
    class GivenReplacerTuple {

        @Nested
        class WhenChangeServiceId {

            @Test
            void thenChangeServicePrefix() {
                InstanceInfo info = apimlInstanceRegistry.changeServiceId(standardInstance);
                assertEquals("hostname:helloclient:10010", info.getInstanceId());
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
           Arguments.of("service*,hello", "hostname:helloclient:10010"),
           Arguments.of("service,hello", "hostname:helloclient:10010"),
           Arguments.of("service*,hello*", "hostname:helloclient:10010"),
           Arguments.of("service*,service", "hostname:serviceclient:10010"),
           Arguments.of("service*", "hostname:serviceclient:10010"),
           Arguments.of(",service", "hostname:serviceclient:10010"),
           Arguments.of("service,", "hostname:serviceclient:10010"),
           Arguments.of(null, "hostname:serviceclient:10010"),
           Arguments.of("different*,hello", "hostname:serviceclient:10010")
       );
    }

    @ParameterizedTest
    @MethodSource("tuples")
    void thenShouldRegister(String tuple, String expectedServiceIdInResult) throws Exception {
        apimlInstanceRegistry = spy(init(new ApimlInstanceRegistry(
            serverConfig,
            clientConfig,
            serverCodecs,
            eurekaClient,
            eurekaServerHttpClientFactory,
            instanceRegistryProperties,
            appCntx,
            new EurekaConfig.Tuple(tuple), metadataFilterService)));
        apimlInstanceRegistry.register(standardInstance, false);
        assertEquals(expectedServiceIdInResult, standardInstance.getInstanceId());
    }

    @ParameterizedTest
    @MethodSource("tuples")
    void thenShouldRegisterWithSecondMethod(String tuple, String expectedServiceIdInResult) throws Exception {
        apimlInstanceRegistry = spy(init(new ApimlInstanceRegistry(
            serverConfig,
            clientConfig,
            serverCodecs,
            eurekaClient,
            eurekaServerHttpClientFactory,
            instanceRegistryProperties,
            appCntx,
            new EurekaConfig.Tuple(tuple), metadataFilterService)));
        apimlInstanceRegistry.register(standardInstance, 1, false);
        assertEquals(expectedServiceIdInResult, standardInstance.getInstanceId());
    }

    @Nested
    class WhenStaticallyRegistration {

        private ThreadLocal<Integer> renewCorrection;

        @BeforeEach
        void setUp() {
            renewCorrection = (ThreadLocal<Integer>) ReflectionTestUtils.getField(apimlInstanceRegistry, "RENEW_CORRECTION");
            renewCorrection.set(123);
            ReflectionTestUtils.setField(apimlInstanceRegistry, "expectedNumberOfClientsSendingRenews", 5);
        }

        @Test
        @SuppressWarnings("unchecked")
        void givenStaticRegistration_thenSuccessful() throws Throwable {
            var methodHandle = mock(MethodHandle.class);
            ReflectionTestUtils.setField(apimlInstanceRegistry, "replicateToPeersMethodHandle", methodHandle);
            var currentStaticIds = (Set<String>) ReflectionTestUtils.getField(apimlInstanceRegistry, "staticRegistrationIds");
            assertTrue(currentStaticIds.isEmpty());

            var registry = mock(ConcurrentHashMap.class);
            ReflectionTestUtils.setField(apimlInstanceRegistry, "registry", registry);

            Map<String, Lease<InstanceInfo>> leaseMap = new HashMap<>();
            when(registry.get(anyString())).thenReturn(leaseMap);
            doReturn(new Object()).when(methodHandle).invokeWithArguments(any(), any(), any(), any(), any(), any(), any());

            apimlInstanceRegistry.registerStatically(standardInstance, false, true);

            assertFalse(currentStaticIds.isEmpty());
            assertFalse(leaseMap.isEmpty());
        }

        @Test
        void givenStaticDefinition_whenSuccessRegistration_thenExpectedNumberOfClientsSendingRenewsDidntChange() {
            apimlInstanceRegistry.registerStatically(standardInstance, false, false);

            assertEquals(5, ReflectionTestUtils.getField(apimlInstanceRegistry, "expectedNumberOfClientsSendingRenews"));
            assertNull(renewCorrection.get());
        }

        @Test
        void givenStaticDefinition_whenFailRegistration_thenCorrectionAndExpectedNumberOfClientsSendingRenewsDidntChange() {
            doThrow(new RuntimeException("test")).when(apimlInstanceRegistry).register(any(), anyInt(), anyBoolean());
            assertThrows(IllegalStateException.class, () -> apimlInstanceRegistry.registerStatically(standardInstance, false, false));

            assertEquals(5, ReflectionTestUtils.getField(apimlInstanceRegistry, "expectedNumberOfClientsSendingRenews"));
            assertNull(renewCorrection.get());
        }

        @Test
        void givenStaticDefinition_whenSuccessCancellation_thenExpectedNumberOfClientsSendingRenewsDidntChange() {
            apimlInstanceRegistry.registerStatically(standardInstance, false, false);
            apimlInstanceRegistry.cancel(standardInstance.getAppName(), standardInstance.getInstanceId(), false);

            assertEquals(5, ReflectionTestUtils.getField(apimlInstanceRegistry, "expectedNumberOfClientsSendingRenews"));
            assertNull(renewCorrection.get());
        }

        @Test
        void givenNonStaticDefinition_whenSuccessCancellation_thenExpectedNumberOfClientsSendingRenewsChanged() {
            renewCorrection.remove();
            apimlInstanceRegistry.register(standardInstance, false); // it increases the number to 6
            ((Set<String>) ReflectionTestUtils.getField(apimlInstanceRegistry, "staticRegistrationIds")).clear();

            apimlInstanceRegistry.cancel(standardInstance.getAppName(), standardInstance.getInstanceId(), false);

            assertEquals(5, ReflectionTestUtils.getField(apimlInstanceRegistry, "expectedNumberOfClientsSendingRenews"));
            assertNull(renewCorrection.get());
        }

    }

    @Nested
    class HeartbeatPeerReplicate {

        @Test
        void givenPeerReplicaHeartbeat_thenSuccess() throws Throwable {
            var methodHandle = mock(MethodHandle.class);
            ReflectionTestUtils.setField(apimlInstanceRegistry, "replicateToPeersMethodHandle", methodHandle);
            var instance = mock(InstanceInfo.class);
            doReturn(new Object()).when(methodHandle).invokeWithArguments(any(), any(), any(), any(), any(), any(), any());
            apimlInstanceRegistry.peerAwareHeartbeat(instance);

            verify(methodHandle, times(1)).invokeWithArguments(any(), any(), any(), any(), any(), any(), any());
        }

    }

    private InstanceInfo getStandardInstance(String instanceId, String appName) {

        return InstanceInfo.Builder.newBuilder()
            .setInstanceId(instanceId)
            .setAppName(appName.toUpperCase())
            .setAppGroupName(appName.toUpperCase())
            .setIPAddr("192.168.0.1")
            .enablePort(InstanceInfo.PortType.SECURE, true)
            .setSecurePort(9090)
            .setHostName("localhost")
            .setSecureVIPAddress("localhost")
            .setVIPAddress(appName)
            .setStatus(InstanceInfo.InstanceStatus.UP)
            .build();
    }

    @Nested
    class UpdateRenewsPerMinThreshold {

        private ApimlInstanceRegistry registry;
        private ThreadLocal<Integer> renewCorrection;

        @BeforeEach
        void setUp() {
            registry = new ApimlInstanceRegistry(
                serverConfig, clientConfig, serverCodecs, eurekaClient,
                eurekaServerHttpClientFactory, instanceRegistryProperties, appCntx, new EurekaConfig.Tuple(""), metadataFilterService
            );
            renewCorrection = (ThreadLocal<Integer>) ReflectionTestUtils.getField(registry, "RENEW_CORRECTION");
        }

        @Test
        void givenCorrection_whenUpdateRenewsPerMinThreshold_thenProcessOnce() {
            ReflectionTestUtils.setField(registry, "expectedNumberOfClientsSendingRenews", 5);
            renewCorrection.set(-2);
            registry.updateRenewsPerMinThreshold();
            assertEquals(3, ReflectionTestUtils.getField(registry, "expectedNumberOfClientsSendingRenews"));
            assertNull(renewCorrection.get());
        }

        @Test
        void givenNoCorrection_whenUpdateRenewsPerMinThreshold_thenDoNothing() {
            ReflectionTestUtils.setField(registry, "expectedNumberOfClientsSendingRenews", 5);
            registry.updateRenewsPerMinThreshold();
            assertEquals(5, ReflectionTestUtils.getField(registry, "expectedNumberOfClientsSendingRenews"));
            assertNull(renewCorrection.get());
        }

    }

}
