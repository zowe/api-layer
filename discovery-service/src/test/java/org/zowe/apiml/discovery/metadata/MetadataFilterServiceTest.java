/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.metadata;

import com.netflix.appinfo.InstanceInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetadataFilterServiceTest {

    @Mock
    private ApimlLogger apimlLogger;
    @Mock
    private InstanceInfo instanceInfo;

    private MetadataFilterService metadataFilterService;

    @BeforeEach
    void setUp() {
        metadataFilterService = new MetadataFilterService();
        ReflectionTestUtils.setField(metadataFilterService, "apimlLogger", apimlLogger);
    }

    @Nested
    class GivenAllowedDomains {

        @BeforeEach
        void setUp() {
            ReflectionTestUtils.setField(metadataFilterService, "allowedDomains", "localhost, *.zowe.org");
            metadataFilterService.afterPropertiesSet();
            var allowedDomainsSet = ReflectionTestUtils.getField(metadataFilterService, "allowedDomainsSet");
            assertEquals(Set.of("localhost", "*.zowe.org", "www.ibm.com", "zowe.github.io", "www.zowe.org", "techdocs.broadcom.com"), allowedDomainsSet);
        }

        @ParameterizedTest(name = "Key: {0}, Value: {1} -> Allowed: {2}")
        @CsvSource({
            "apiml.externalUrl, https://localhost:8080, true, ''",
            "apiml.externalUrl, https://example.com:8080, false, org.zowe.apiml.common.urlNotAllowed",
            "apiml.swaggerUrl, https://example.com:8080, false, org.zowe.apiml.common.urlNotAllowed",
            "apiml.swaggerUrl, https://sub.zowe.org:8080, true, ''",
            "apiml.graphqlUrl, https://sub.zowe.org:8080, true, ''",
            "apiml.documentationUrl, https://invalid.org:8080, false, org.zowe.apiml.common.urlNotAllowed",
            "apiml.customKey, https://invalid.org:8080, true, ''",
            "apiml.documentationUrl, invalid-url, false, org.zowe.apiml.common.urlNotAllowed",
            "apiml.externalUrl, HTTPS://LOCALHOST:8080, true, ''",
            "apiml.externalUrl, HTTPS://INVALID.ORG:8080, false, org.zowe.apiml.common.urlNotAllowed",
            "apiml.externalUrl, httpsBlah://localhost:8080, false, org.zowe.apiml.common.urlNotAllowed",
            "apiml.externalUrl, http://localhost:8080, false, org.zowe.apiml.common.schemeNotAllowed",
            "apiml.externalUrl, https://invalid.org:8080null, false, org.zowe.apiml.common.urlNotAllowed",
            "apiml.externalUrl, https://localhost:8080null, true, ''"
        })
        void shouldVerifyMetadataKeysAndDomains(String metadataKey, String metadataValue, boolean isAllowed, String expectedLogKey) {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(metadataKey, metadataValue);
            when(instanceInfo.getMetadata()).thenReturn(metadata);
            lenient().when(instanceInfo.getInstanceId()).thenReturn("test-instance");

            if (isAllowed) {
                metadataFilterService.verifyAllowedDomains(instanceInfo);
                verify(apimlLogger, never()).log(anyString(), eq(metadataKey), eq(metadataValue), anyString());
            } else {
                assertThrows(MetadataValidationException.class, () -> metadataFilterService.verifyAllowedDomains(instanceInfo));
                verify(apimlLogger).log(eq(expectedLogKey), eq(metadataKey), eq(metadataValue), anyString());
            }
        }

        @ParameterizedTest(name = "Value: {0} -> Allowed: {1}")
        @CsvSource({
            "https://localhost:8080, true",
            "https://example.com:8080, false",
            "https://sub.zowe.org:8080, true",
            "https://invalid.org:8080, false",
            "invalid-url, false",
            "HTTPS://LOCALHOST:8080, true",
            "HTTPS://INVALID.ORG:8080, false"
        })
        void shouldVerifyHostname(String hostname, boolean isAllowed) {
            when(instanceInfo.getMetadata()).thenReturn(Collections.emptyMap());
            when(instanceInfo.getHostName()).thenReturn(hostname);
            lenient().when(instanceInfo.getInstanceId()).thenReturn("test-instance");

            if (isAllowed) {
                metadataFilterService.verifyAllowedDomains(instanceInfo);
                verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq("Instance Hostname"), eq(hostname), anyString());
            } else {
                assertThrows(MetadataValidationException.class, () -> {
                    metadataFilterService.verifyAllowedDomains(instanceInfo);
                });
                verify(apimlLogger).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq("Instance Hostname"), eq(hostname), anyString());
            }
        }

        @ParameterizedTest
        @CsvSource(delimiterString = "|", value = {
            "localhost,192.168.0.2,example.com|192.168.0.2|true",
            "localhost,192.168.0.2,example.com|192.168.0.1|false",
            "localhost|127.0.0.1|true",
            "localhost|invalid#1|false",
            "::1|::1|true",
            "::1|::2|false",
            "2001:db8::1|2001:db8::1|true",
            "2001:db8::1|2001:db8::2|false",
            "2001:db8::/32|2001:db8:abcd:1234::1|true",
            "2001:db8::/32|2001:db9::1|false",
            "fe80::/10|fe80::1234:5678:9abc|true",
            "fe80::/10|fc00::1|false",
            "2001:db8::/64|2001:db8:0:0:ffff:ffff:ffff:ffff|true",
            "2001:db8::/64|2001:db8:0:1::1|false",
            "localhost,192.168.0.0/24,2001:db8::/32|192.168.0.10|true",
            "localhost,192.168.0.0/24,2001:db8::/32|2001:db8::abcd|true",
            "localhost,192.168.0.0/24,2001:db8::/32|127.0.0.1|true",
            "localhost,192.168.0.0/24,2001:db8::/32|10.0.0.1|false",
            "localhost,192.168.0.0/24,2001:db8::/32|fe80::1|false",
            "192.168.0.2|192.168.0.2|true",
            "192.168.0.2|192.168.0.3|false",
            "192.168.0.0/24|192.168.0.55|true",
            "192.168.0.0/24|192.168.1.1|false",
            "10.0.0.0/8|10.255.255.255|true",
            "10.0.0.0/8|11.0.0.1|false",
            "172.16.0.0/12|172.31.255.254|true",
            "172.16.0.0/12|172.32.0.1|false",
            "192.168.0.0/32|192.168.0.0|true",
            "192.168.0.0/32|192.168.0.1|false",
            "192.168.0.0/abc|192.168.0.0|false"
        })
        void givenIpAddressInAllowedList_whenIsAllowedDomain_thenDecide(String allowList, String domain, boolean isAllowed) {
            var service = new MetadataFilterService();
            ReflectionTestUtils.setField(service,"allowedDomains", allowList);
            service.afterPropertiesSet();
            assertEquals(isAllowed, service.isAllowedDomain(domain));
        }

        @Nested
        class OnCors {

            @Test
            void whenUsingWildcard_thenWarningIsLogged() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("apiml.corsAllowedOrigins", "*");
                when(instanceInfo.getMetadata()).thenReturn(metadata);

                metadataFilterService.verifyAllowedDomains(instanceInfo);

                verify(apimlLogger).log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
                verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.urlNotAllowed"), anyString(), anyString(), anyString());
            }

            @Test
            void whenUsingSpecificURL_thenNoWarningIsLogged() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("apiml.corsAllowedOrigins", "https://localhost:3000");
                when(instanceInfo.getMetadata()).thenReturn(metadata);

                metadataFilterService.verifyAllowedDomains(instanceInfo);

                verify(apimlLogger, never()).log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
                verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.urlNotAllowed"), anyString(), anyString(), anyString());
            }

            @Test
            void whenUsingInvalidURL_thenExceptionIsThrownAndLogged() {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("apiml.corsAllowedOrigins", "https://invalid.org:3000");
                when(instanceInfo.getMetadata()).thenReturn(metadata);
                when(instanceInfo.getInstanceId()).thenReturn("test-instance");

                assertThrows(MetadataValidationException.class, () -> metadataFilterService.verifyAllowedDomains(instanceInfo));

                verify(apimlLogger).log("org.zowe.apiml.common.urlNotAllowed", "API ML CORS Allowed Origin", "https://invalid.org:3000", "test-instance");
            }

            @Test
            void whenUsingHttpCorsWithoutAttls_thenSchemeNotAllowedIsLogged() {
                ReflectionTestUtils.setField(metadataFilterService, "isClientAttlsEnabled", false);
                Map<String, String> metadata = new HashMap<>();
                metadata.put("apiml.corsAllowedOrigins", "http://localhost:3000");
                when(instanceInfo.getMetadata()).thenReturn(metadata);
                when(instanceInfo.getInstanceId()).thenReturn("test-instance");

                assertThrows(MetadataValidationException.class, () -> metadataFilterService.verifyAllowedDomains(instanceInfo));

                verify(apimlLogger).log("org.zowe.apiml.common.schemeNotAllowed", "API ML CORS Allowed Origin", "http://localhost:3000", "test-instance");
            }

        }

        @Nested
        class OnSchemeValidation {

            @Test
            void whenHttpAndAttlsDisabled_thenSchemeNotAllowedLoggedAndExceptionThrown() {
                ReflectionTestUtils.setField(metadataFilterService, "isClientAttlsEnabled", false);
                when(instanceInfo.getHomePageUrl()).thenReturn("http://localhost:8080");
                when(instanceInfo.getInstanceId()).thenReturn("test-instance");

                assertThrows(MetadataValidationException.class, () -> metadataFilterService.verifyAllowedDomains(instanceInfo));

                verify(apimlLogger).log("org.zowe.apiml.common.schemeNotAllowed", "Home Page URL", "http://localhost:8080", "test-instance");
            }

            @Test
            void whenHttpAndAttlsEnabled_thenAllowed() {
                ReflectionTestUtils.setField(metadataFilterService, "isClientAttlsEnabled", true);
                when(instanceInfo.getHomePageUrl()).thenReturn("http://localhost:8080");

                metadataFilterService.verifyAllowedDomains(instanceInfo);

                verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.schemeNotAllowed"), anyString(), anyString(), anyString());
            }

            @Test
            void whenHttpsAndAttlsDisabled_thenAllowed() {
                ReflectionTestUtils.setField(metadataFilterService, "isClientAttlsEnabled", false);
                when(instanceInfo.getHomePageUrl()).thenReturn("https://localhost:8080");

                metadataFilterService.verifyAllowedDomains(instanceInfo);

                verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.schemeNotAllowed"), anyString(), anyString(), anyString());
            }

        }

        @Nested
        class IpAddress {

            @ParameterizedTest(name = "Value: {0} -> Allowed: {1}")
            @CsvSource({
                "127.0.0.1,true",
                "1.0.1.0,false"
            })
            void givenIpAddress_whenOnboarding_thenVerify(String ipAddress, boolean isAllowed) {
                var ii = InstanceInfo.Builder.newBuilder()
                    .setInstanceId("test")
                    .setAppName("test")
                    .setIPAddr(ipAddress)
                    .setHostName("localhost")
                    .build();

                ii = metadataFilterService.verifyAllowedDomains(ii);
                if (isAllowed) {
                    verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq("IP Address"), eq(ipAddress), anyString());
                } else {
                    verify(apimlLogger).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq("IP Address"), eq(ipAddress), anyString());
                }
                assertEquals("127.0.0.1", ii.getIPAddr());
            }

            @ParameterizedTest(name = "Given local address {0} when validate then fail")
            @MethodSource("localIpAddresses")
            void givenLocalIpAddress_whenValidate_itIsAccepted(String ipAddress) {
                ReflectionTestUtils.setField(metadataFilterService, "allowedDomains", "non-local");
                metadataFilterService.afterPropertiesSet();

                InstanceInfo ii = InstanceInfo.Builder.newBuilder()
                    .setIPAddr(ipAddress)
                    .setAppName("service")
                    .setInstanceId("test-instance")
                    .build();

                metadataFilterService.verifyAllowedDomains(ii);
                verify(apimlLogger).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq("IP Address"), eq(ipAddress), anyString());
            }

            static Stream<Arguments> localIpAddresses() throws SocketException {
                return NetworkInterface.networkInterfaces()
                    .flatMap(NetworkInterface::inetAddresses)
                    .map(InetAddress::getHostAddress)
                    .map(Arguments::of);
            }

            @Test
            void givenLocalHost_whenGetIpAddress_theReturnLoopback() {
                assertEquals("127.0.0.1", metadataFilterService.getIpAddress("localhost"));
            }

            @Test
            void givenUnknownDomain_whenGetIpAddress_theReturnNull() {
                assertNull(metadataFilterService.getIpAddress("absolutellyunknowndomainatall"));
            }

            @Test
            void givenInvalidIpAddress_whenValidate_thenReturnUpdatedInstanceInfo() {
                var ii = InstanceInfo.Builder.newBuilder()
                    .setInstanceId("testNotAllowedIpAddress")
                    .setAppName("test-service")
                    .setIPAddr("1.2.3.4")
                    .setHostName("localhost")
                    .build();

                ii = metadataFilterService.verifyAllowedDomains(ii);
                assertEquals("127.0.0.1", ii.getIPAddr());
                assertEquals("localhost", ii.getHostName());
            }

            @ParameterizedTest
            @EmptySource
            @NullSource
            void givenNoHostname_whenGetIpAddress_thenReturnNull(String hostname) {
                assertNull(metadataFilterService.getIpAddress(hostname));
            }

        }

    }

}
