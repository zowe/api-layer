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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.exception.MetadataValidationException;
import org.zowe.apiml.message.log.ApimlLogger;

import java.util.HashMap;
import java.util.Map;

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
        void setUp() throws Exception {
            ReflectionTestUtils.setField(metadataFilterService, "allowedDomains", "localhost, *.zowe.org");
            metadataFilterService.afterPropertiesSet();
        }

        @ParameterizedTest(name = "Key: {0}, Value: {1} -> Allowed: {2}")
        @CsvSource({
            "apiml.gatewayUrl, https://localhost:8080, true",
            "apiml.gateway-url, https://localhost:8080, true",
            "apiml.serviceUrl, https://example.com:8080, false",
            "apiml.service-url, https://example.com:8080, false",
            "apiml.swaggerUrl, https://sub.zowe.org:8080, true",
            "apiml.graphqlUrl, https://sub.zowe.org:8080, true",
            "apiml.documentationUrl, https://invalid.org:8080, false",
            "apiml.customKey, https://invalid.org:8080, true",
            "apiml.gatewayUrl, invalid-url, true"
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

    }

}
