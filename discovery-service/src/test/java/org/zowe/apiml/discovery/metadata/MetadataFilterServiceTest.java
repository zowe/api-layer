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
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

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
        void setUp() throws Exception {
            ReflectionTestUtils.setField(metadataFilterService, "allowedDomains", "localhost, *.zowe.org");
            metadataFilterService.afterPropertiesSet();
            var allowedDomainsSet = ReflectionTestUtils.getField(metadataFilterService, "allowedDomainsSet");
            assertEquals(Set.of("localhost", "*.zowe.org", "www.ibm.com", "zowe.github.io", "www.zowe.org", "techdocs.broadcom.com"), allowedDomainsSet);
        }

        @ParameterizedTest(name = "Key: {0}, Value: {1} -> Allowed: {2}")
        @CsvSource({
            "apiml.externalUrl, https://localhost:8080, true",
            "apiml.externalUrl, https://localhost:8080, true",
            "apiml.externalUrl, https://example.com:8080, false",
            "apiml.swaggerUrl, https://example.com:8080, false",
            "apiml.swaggerUrl, https://sub.zowe.org:8080, true",
            "apiml.graphqlUrl, https://sub.zowe.org:8080, true",
            "apiml.documentationUrl, https://invalid.org:8080, false",
            "apiml.customKey, https://invalid.org:8080, true",
            "apiml.documentationUrl, invalid-url, false",
            "apiml.externalUrl, HTTPS://LOCALHOST:8080, true",
            "apiml.externalUrl, HTTPS://INVALID.ORG:8080, false",
            "apiml.externalUrl, https://invalid.org:8080null, false",
            "apiml.externalUrl, https://localhost:8080null, true"
        })
        void shouldVerifyMetadataKeysAndDomains(String metadataKey, String metadataValue, boolean isAllowed) throws Exception {
            Map<String, String> metadata = new HashMap<>();
            metadata.put(metadataKey, metadataValue);
            when(instanceInfo.getMetadata()).thenReturn(metadata);
            lenient().when(instanceInfo.getInstanceId()).thenReturn("test-instance");

            if (isAllowed) {
                metadataFilterService.verifyAllowedDomains(instanceInfo);
                verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq(metadataKey), eq(metadataValue), anyString());
            } else {
                assertThrows(MetadataValidationException.class, () -> {
                    metadataFilterService.verifyAllowedDomains(instanceInfo);
                });
                verify(apimlLogger).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq(metadataKey), eq(metadataValue), anyString());
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
            "localhost|127.0.0.1|false",
            "localhost|invalid#1|false"
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
            void whenUsingWildcard_thenWarningIsLogged() throws Exception {
                Map<String, String> metadata = new HashMap<>();
                metadata.put("apiml.corsAllowedOrigins", "*");
                when(instanceInfo.getMetadata()).thenReturn(metadata);

                metadataFilterService.verifyAllowedDomains(instanceInfo);

                verify(apimlLogger).log("org.zowe.apiml.common.patternNotRecommendedInCorsAllowedOrigins");
                verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.urlNotAllowed"), anyString(), anyString(), anyString());
            }

            @Test
            void whenUsingSpecificURL_thenNoWarningIsLogged() throws Exception {
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

                assertThrows(MetadataValidationException.class, () -> {
                    metadataFilterService.verifyAllowedDomains(instanceInfo);
                });

                verify(apimlLogger).log("org.zowe.apiml.common.urlNotAllowed", "API ML CORS Allowed Origin", "https://invalid.org:3000", "test-instance");
            }

        }

        @Nested
        class IpAddress {

            private static final String LOCALHOST = "localhost";

            @ParameterizedTest
            @EmptySource
            @NullSource
            void givenNoAddress_whenCheckIpAddress_thenReturnTrue(String emptyAddress) {
                var service = new MetadataFilterService();
                ReflectionTestUtils.setField(service,"allowedDomains", LOCALHOST);
                service.afterPropertiesSet();
                assertTrue(service.isAllowedIpAddress(emptyAddress, LOCALHOST));
            }

            @ParameterizedTest
            @CsvSource(delimiterString = "|", value = {
                "domain1,192.168.0.1",
                "192.168.000.001"
            })
            void givenIpAddressAllowedDomains_whenCheckIpAddress_thenReturnTrue(String allowedDomain) {
                var service = new MetadataFilterService();
                ReflectionTestUtils.setField(service,"allowedDomains", allowedDomain);
                service.afterPropertiesSet();
                assertTrue(service.isAllowedIpAddress("192.168.0.1", LOCALHOST));
            }

            @ParameterizedTest
            @CsvSource({
                "127.0.0.1",
                "0.0.0.0"
            })
            void givenLocalAddress_whenCheckIpAddress_thenReturnTrue(String address) {
                var service = new MetadataFilterService();
                ReflectionTestUtils.setField(service,"allowedDomains", "localhost");
                service.afterPropertiesSet();
                assertTrue(service.isAllowedIpAddress(address, LOCALHOST));
            }

            @Test
            void givenMatchingNonLocalAddress_whenCheckIpAddress_thenReturnTrue() throws UnknownHostException {
                var service = new MetadataFilterService();
                ReflectionTestUtils.setField(service,"allowedDomains", InetAddress.getLocalHost().getHostName());
                service.afterPropertiesSet();
                assertTrue(service.isAllowedIpAddress(InetAddress.getLocalHost().getHostAddress(), LOCALHOST));
            }

            @ParameterizedTest
            @CsvSource({
                "192.168.0.2",
                "10.0.1.100",
                "1.0.1.0"
            })
            void givenNotMatchingAddress_whenCheckIpAddress_thenReturnFalse(String address) {
                var service = new MetadataFilterService();
                ReflectionTestUtils.setField(service,"allowedDomains", "https://google.com,192.168.0.1,example.com,localhost");
                service.afterPropertiesSet();
                assertFalse(service.isAllowedIpAddress(address, LOCALHOST));
            }

            @ParameterizedTest(name = "Value: {0} -> Allowed: {1}")
            @CsvSource({
                "127.0.0.1,true",
                "1.0.1.0,false"
            })
            void givenIpAddress_whenOnboarding_thenVerify(String ipAddress, boolean isAllowed) {
                when(instanceInfo.getMetadata()).thenReturn(Collections.emptyMap());
                when(instanceInfo.getIPAddr()).thenReturn(ipAddress);
                lenient().when(instanceInfo.getInstanceId()).thenReturn("test-instance");

                if (isAllowed) {
                    metadataFilterService.verifyAllowedDomains(instanceInfo);
                    verify(apimlLogger, never()).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq("IP Address"), eq(ipAddress), anyString());
                } else {
                    assertThrows(MetadataValidationException.class, () -> {
                        metadataFilterService.verifyAllowedDomains(instanceInfo);
                    });
                    verify(apimlLogger).log(eq("org.zowe.apiml.common.urlNotAllowed"), eq("IP Address"), eq(ipAddress), anyString());
                }
            }

            @Test
            void givenMatchingWildcardWithHostName_whenCheckIpAddress_thenCheckAgainstHostName() throws UnknownHostException {
                var hostname = InetAddress.getLocalHost().getHostName().toLowerCase(Locale.ROOT);
                var mockWildcard = "*." + hostname;
                var service = new MetadataFilterService() {
                    @Override
                    boolean isMatchingWildCard(String hostname, String wildCard) {
                        // wildcard assume `*.` and then verify without first character, so the dot is problem for the test
                        return mockWildcard.equals(wildCard);
                    }
                };
                ReflectionTestUtils.setField(service,"allowedDomainsSet", Collections.singleton(mockWildcard));
                assertTrue(service.isAllowedIpAddress(InetAddress.getLocalHost().getHostAddress(), hostname));
            }

        }

    }

}
