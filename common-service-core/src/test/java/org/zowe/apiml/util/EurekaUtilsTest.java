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

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
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

    @Nested
    class GetServiceIdFromInstanceIdTests {

        @Test
        void givenStandardInstanceId_thenExtractServiceId() {
            assertEquals("abc", EurekaUtils.getServiceIdFromInstanceId("hostname:abc:123"));
            assertEquals("service", EurekaUtils.getServiceIdFromInstanceId("host:service:8080"));
            assertEquals("my-service", EurekaUtils.getServiceIdFromInstanceId("my.host.com:my-service:443"));
        }

        @Test
        void givenIPv4InstanceId_thenExtractServiceId() {
            assertEquals("gateway", EurekaUtils.getServiceIdFromInstanceId("192.168.1.1:gateway:8080"));
            assertEquals("api", EurekaUtils.getServiceIdFromInstanceId("10.0.0.1:api:443"));
            assertEquals("service", EurekaUtils.getServiceIdFromInstanceId("127.0.0.1:service:80"));
        }

        @Test
        void givenBracketedIPv6InstanceId_thenExtractServiceId() {
            // IPv6 address with brackets: [ipv6]:serviceId:port
            assertEquals("gateway", EurekaUtils.getServiceIdFromInstanceId("[2620:117:10:4300::55:28]:gateway:12314"));
            assertEquals("discovery", EurekaUtils.getServiceIdFromInstanceId("[::1]:discovery:8080"));
            assertEquals("service", EurekaUtils.getServiceIdFromInstanceId("[2001:db8::1]:service:443"));
            assertEquals("api", EurekaUtils.getServiceIdFromInstanceId("[fe80::1]:api:8080"));
            assertEquals("svc", EurekaUtils.getServiceIdFromInstanceId("[::]:svc:80"));  // unspecified address
        }

        @Test
        void givenUnbracketedIPv6InstanceId_thenExtractServiceIdFromEnd() {
            // Unbracketed IPv6 address: parse from the end (port is last, serviceId is second-to-last)
            assertEquals("gateway", EurekaUtils.getServiceIdFromInstanceId("2620:117:10:4300::55:28:gateway:12314"));
            assertEquals("apicatalog", EurekaUtils.getServiceIdFromInstanceId("2620:117:10:4300::55:28:apicatalog:12312"));
            assertEquals("discovery", EurekaUtils.getServiceIdFromInstanceId("::1:discovery:8080"));  // loopback
            assertEquals("service", EurekaUtils.getServiceIdFromInstanceId("2001:db8::1:service:443"));
            assertEquals("api", EurekaUtils.getServiceIdFromInstanceId("fe80::1:api:8080"));  // link-local
        }

        @Test
        void givenInvalidInstanceId_thenReturnNull() {
            assertNull(EurekaUtils.getServiceIdFromInstanceId("hostname:123:"));  // empty port
            assertNull(EurekaUtils.getServiceIdFromInstanceId("::"));
            assertNull(EurekaUtils.getServiceIdFromInstanceId(":"));
            assertNull(EurekaUtils.getServiceIdFromInstanceId(""));
            assertNull(EurekaUtils.getServiceIdFromInstanceId(null));
            assertNull(EurekaUtils.getServiceIdFromInstanceId("onlyhostname"));
            assertNull(EurekaUtils.getServiceIdFromInstanceId("hostname:service"));  // only 2 parts
        }

        @Test
        void givenEmptyServiceId_thenReturnNull() {
            assertNull(EurekaUtils.getServiceIdFromInstanceId("hostname::123"));  // empty serviceId
            assertNull(EurekaUtils.getServiceIdFromInstanceId("192.168.1.1::8080"));  // IPv4 with empty serviceId
        }

        @Test
        void givenMalformedBracketedIPv6_thenReturnNull() {
            assertNull(EurekaUtils.getServiceIdFromInstanceId("[2001:db8::1]"));  // no serviceId/port after brackets
            assertNull(EurekaUtils.getServiceIdFromInstanceId("[2001:db8::1]:"));  // missing serviceId and port
            assertNull(EurekaUtils.getServiceIdFromInstanceId("[2001:db8::1]:service"));  // missing port
            assertNull(EurekaUtils.getServiceIdFromInstanceId("[2001:db8::1]service:8080"));  // missing colon after bracket
            assertNull(EurekaUtils.getServiceIdFromInstanceId("[2001:db8::1]:svc:extra:8080"));  // extra colons
            assertNull(EurekaUtils.getServiceIdFromInstanceId("[]::8080"));  // empty brackets
        }

        @Test
        void givenEdgeCaseHostnames_thenExtractServiceId() {
            // Empty hostname is technically allowed (extracts serviceId correctly)
            assertEquals("service", EurekaUtils.getServiceIdFromInstanceId(":service:8080"));
            // Very long serviceId
            assertEquals("a".repeat(63), EurekaUtils.getServiceIdFromInstanceId("host:" + "a".repeat(63) + ":8080"));
        }
    }

    private InstanceInfo createInstanceInfo(String host, int port, int securePort, boolean isSecureEnabled) {
        InstanceInfo out = mock(InstanceInfo.class);
        when(out.getHostName()).thenReturn(host);
        when(out.getPort()).thenReturn(port);
        when(out.getSecurePort()).thenReturn(securePort);
        when(out.isPortEnabled(InstanceInfo.PortType.SECURE)).thenReturn(isSecureEnabled);
        return out;
    }

    @Test
    void testGetUrl() {
        InstanceInfo ii1 = createInstanceInfo("hostname1", 80, 0, false);
        InstanceInfo ii2 = createInstanceInfo("localhost", 80, 443, true);

        assertEquals("http://hostname1:80", EurekaUtils.getUrl(ii1));
        assertEquals("https://localhost:443", EurekaUtils.getUrl(ii2));
    }

    @Test
    void testGetUrlWithIPv6() {
        // IPv6 addresses should be wrapped in brackets in URLs
        InstanceInfo iiIPv6Http = createInstanceInfo("2620:117:10:4300::55:28", 8080, 0, false);
        InstanceInfo iiIPv6Https = createInstanceInfo("2001:db8::1", 80, 443, true);
        InstanceInfo iiIPv6Loopback = createInstanceInfo("::1", 8080, 0, false);

        assertEquals("http://[2620:117:10:4300::55:28]:8080", EurekaUtils.getUrl(iiIPv6Http));
        assertEquals("https://[2001:db8::1]:443", EurekaUtils.getUrl(iiIPv6Https));
        assertEquals("http://[::1]:8080", EurekaUtils.getUrl(iiIPv6Loopback));
    }

    @Nested
    class PrimaryAndSecondaryRegistration {

        private static final String PRIMARY = "primary";
        private static final String SECONDARY = "secondary";

        private DiscoveryClient discoveryClient;

        @BeforeEach
        void init() {
            discoveryClient = mock(DiscoveryClient.class);

            InstanceInfo instanceInfoPrimary = InstanceInfo.Builder.newBuilder()
                .setAppName(PRIMARY)
                .setInstanceId(String.format("x:%s:1", PRIMARY))
                .build();
            ServiceInstance serviceInstancePrimary = new EurekaServiceInstance(instanceInfoPrimary);
            doReturn(Collections.singletonList(serviceInstancePrimary)).when(discoveryClient).getInstances(PRIMARY);

            InstanceInfo instanceInfoSecondary = InstanceInfo.Builder.newBuilder()
                .setAppName(GATEWAY.getServiceId())
                .setInstanceId(String.format("x:%s:1", GATEWAY.getServiceId()))
                .setMetadata(Map.of(
                    APIML_ID, SECONDARY,
                    REGISTRATION_TYPE, ADDITIONAL.getValue()
                ))
                .build();
            ServiceInstance serviceInstanceSecondary = new EurekaServiceInstance(instanceInfoSecondary);
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
