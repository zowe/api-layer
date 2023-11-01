/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config;


import com.google.common.collect.ImmutableMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class AdditionalRegistrationParserTest {

    private final AdditionalRegistrationParser parser = new AdditionalRegistrationParser();

    @Nested
    class GivenInvalidEnvironmentPropertiesAreProvided {
        @Test
        void shouldParseEmptyListFromNullMap() {
            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(null);
            assertThat(registrations).isEmpty();
        }

        @Test
        void shouldParseEmptyListFromIrrelevantMap() {

            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(ImmutableMap.of("someKey", "someValue"));
            assertThat(registrations).isEmpty();
        }

        @Test
        void shouldParseEmptyListFromEmptyValues() {
            Map<String, String> allProperties = ImmutableMap.of(
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS", "",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_SERVICEURL", "",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_GATEWAYURL", "");
            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(allProperties);
            assertThat(registrations).isEmpty();
        }

        @Test
        void shouldParseBadRoutesIndexToEmptyRoutes() {
            Map<String, String> allProperties = ImmutableMap.of(
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS", "https://eureka",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_A_SERVICEURL", "/",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_A_GATEWAYURL", "/");
            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(allProperties);

            assertThat(registrations).containsExactly(AdditionalRegistration.builder().discoveryServiceUrls("https://eureka").routes(emptyList()).build());
        }

        @Test
        void shouldParseBadIndexToEmptyRegistrations() {
            Map<String, String> allProperties = ImmutableMap.of(
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION__DISCOVERYSERVICEURLS", "https://eureka");
            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(allProperties);

            assertThat(registrations).isEmpty();
        }
    }

    @Nested
    class GivenValidEnvironmentPropertiesAreProvided {

        private final Map<String, String> envProperties = new TreeMap<>();

        @BeforeEach
        void setUp() {
            envProperties.putAll(
                ImmutableMap.of(
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS", "https://eureka",
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_SERVICEURL", "/",
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_GATEWAYURL", "/")
            );
        }

        @Test
        void shouldParseFirstAdditionalRegistration() {

            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(envProperties);

            AdditionalRegistration expectedRegistration = new AdditionalRegistration("https://eureka", singletonList(new AdditionalRegistration.Route("/", "/")));

            assertThat(registrations).hasSize(1);
            assertThat(registrations.get(0)).isEqualTo(expectedRegistration);
        }

        @Test
        void shouldParseAdditionalRegistrationWithoutRoutes() {
            envProperties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_DISCOVERYSERVICEURLS", "https://eureka-2");
            envProperties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_SERVICEURL", "");
            envProperties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_GATEWAYURL", null);

            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(envProperties);

            AdditionalRegistration expectedSecondRegistration = new AdditionalRegistration("https://eureka-2", Collections.emptyList());

            assertThat(registrations).hasSize(2);
            assertThat(registrations.get(1)).isEqualTo(expectedSecondRegistration);
        }

        @Test
        void shouldParseAdditionalRegistrationWithPartiallyDefinedRoutes() {
            Map<String, String> properties = new HashMap<>();
            properties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_DISCOVERYSERVICEURLS", "https://eureka-2");
            properties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_SERVICEURL", "");
            properties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_GATEWAYURL", null);
            properties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_1_SERVICEURL", "/serviceUrl");
            properties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_1_GATEWAYURL", null);
            properties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_2_SERVICEURL", null);
            properties.put("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_2_GATEWAYURL", "/gatewayUrl");

            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(properties);

            AdditionalRegistration expectedSecondRegistration = new AdditionalRegistration("https://eureka-2", Arrays.asList(new AdditionalRegistration.Route(null, "/serviceUrl"), new AdditionalRegistration.Route("/gatewayUrl", null)));

            assertThat(registrations).hasSize(1);
            assertThat(registrations.get(0).getRoutes()).hasSize(2);
            assertThat(registrations.get(0)).isEqualTo(expectedSecondRegistration);
        }

        @Test
        void shouldParseAdditionalRegistrationCaseInsensitively() {
            Map<String, String> properties = new HashMap<>();
            properties.put("ZWE_configs_apiml_service_additionalRegistration_0_discoveryServiceUrls", "https://eureka-service");
            properties.put("ZWE_configs_apiml_service_additionalRegistration_0_routes_0_serviceUrl", "/serviceUrl");
            properties.put("ZWE_configs_apiml_service_additionalRegistration_0_routes_0_gatewayUrl", "/gatewayUrl");

            List<AdditionalRegistration> registrations = parser.extractAdditionalRegistrations(properties);

            AdditionalRegistration expectedSecondRegistration = new AdditionalRegistration("https://eureka-service", singletonList(new AdditionalRegistration.Route("/gatewayUrl", "/serviceUrl")));
            assertThat(registrations).hasSize(1);
            assertThat(registrations.get(0).getRoutes()).hasSize(1);
            assertThat(registrations.get(0)).isEqualTo(expectedSecondRegistration);
        }
    }
}
