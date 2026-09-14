/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.zowe.apiml.exception.MetadataValidationException;

import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.APIML_ID;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.REGISTRATION_TYPE;
import static org.zowe.apiml.constants.EurekaMetadataDefinition.RegistrationType.ADDITIONAL;
import static org.zowe.apiml.product.constants.CoreService.GATEWAY;

class EurekaUtilsTest {

    @Test
    void test() {
        assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("123:abc:def"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId("123:abc:def:::::xyz"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId("hostname:123:"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId("::"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId("123::def"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(":"));
        assertNull(EurekaUtils.getServiceIdFromInstanceId(""));
    }

    @Nested
    class PrimaryAndSecondaryRegistration {

        private static final String PRIMARY = "primary";
        private static final String SECONDARY = "secondary";

        private DiscoveryClient discoveryClient;

        @BeforeEach
        void init() {
            discoveryClient = mock(DiscoveryClient.class);

            // getInstanceInfo works off Spring Cloud's ServiceInstance and its metadata, so the test builds
            // those directly rather than wrapping Netflix InstanceInfo objects in EurekaServiceInstance.
            ServiceInstance serviceInstancePrimary = new DefaultServiceInstance(
                String.format("x:%s:1", PRIMARY), PRIMARY, "localhost", 10010, true, Map.of());
            doReturn(Collections.singletonList(serviceInstancePrimary)).when(discoveryClient).getInstances(PRIMARY);

            ServiceInstance serviceInstanceSecondary = new DefaultServiceInstance(
                String.format("x:%s:1", GATEWAY.getServiceId()), GATEWAY.getServiceId(), "localhost", 10010, true,
                Map.of(APIML_ID, SECONDARY, REGISTRATION_TYPE, ADDITIONAL.getValue()));
            doReturn(Collections.singletonList(serviceInstanceSecondary)).when(discoveryClient).getInstances(GATEWAY.getServiceId());
        }

        @Test
        void givenPrimaryRegistration_whenGetInstanceInfo_thenReturnInstanceInfo() {
            var instance = EurekaUtils.getInstanceInfo(discoveryClient, PRIMARY);
            assertTrue(instance.isPresent());
            assertEquals(PRIMARY, instance.get().getServiceId().toLowerCase());
        }

        @Test
        void givenSecondaryRegistration_whenGetInstanceInfo_thenReturnInstanceInfo() {
            var instance = EurekaUtils.getInstanceInfo(discoveryClient, SECONDARY);
            assertTrue(instance.isPresent());
            assertEquals(GATEWAY.getServiceId(), instance.get().getServiceId().toLowerCase());
        }

        @Test
        void givenUnknownServiceId_whenGetInstanceInfo_thenReturnEmptyOptional() {
            var instance = EurekaUtils.getInstanceInfo(discoveryClient, "unknown");
            assertTrue(instance.isEmpty());
        }

    }

    @Nested
    class WhenValidatingServiceId {

        private static Stream<Arguments> validServiceIds() {
            return Stream.of(
                Arguments.of("valid-service-id"),
                Arguments.of("a".repeat(63))
            );
        }

        private static Stream<Arguments> invalidServiceIds() {
            return Stream.of(
                Arguments.of("service_id"),
                Arguments.of(""),
                Arguments.of(" "),
                Arguments.of("Invalid@ServiceId"),
                Arguments.of("a".repeat(64))
            );
        }

        @ParameterizedTest
        @MethodSource("invalidServiceIds")
        void givenServiceIdWithUnderscore_thenThrowMetadataValidationException(String serviceId) {
            assertThrows(MetadataValidationException.class, () -> EurekaUtils.validateServiceId(serviceId));
        }

        @ParameterizedTest
        @MethodSource("validServiceIds")
        void testValidateServiceId_thenDoNotThrowException(String serviceId) {
            assertDoesNotThrow(() -> EurekaUtils.validateServiceId(serviceId));
        }
    }

}
