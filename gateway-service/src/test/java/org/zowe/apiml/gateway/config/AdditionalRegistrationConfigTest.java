/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import org.apache.groovy.util.Maps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;


class AdditionalRegistrationConfigTest {
    @Nested
    class GivenInvalidEnvironmentPropertiesAreProvided {
        @Test
        void shouldParseEmptyListFromNullMap() {
            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(null);
            assertThat(registrations).isEmpty();
        }

        @Test
        void shouldParseEmptyListFromIrrelevantMap() {
            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(Maps.of("someKey", "someValue"));
            assertThat(registrations).isEmpty();
        }

        @Test
        void shouldParseEmptyListFromEmptyValues() {
            Map<String, String> allProperties = Maps.of(
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS", "",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_SERVICEURL", "",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_GATEWAYURL", "");
            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(allProperties);
            assertThat(registrations).isEmpty();
        }

        @Test
        void shouldParseBadRoutesIndexToEmptyRoutes() {
            Map<String, String> allProperties = Maps.of(
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS", "https://eureka",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_A_SERVICEURL", "/",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_A_GATEWAYURL", "/");
            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(allProperties);

            assertThat(registrations).containsExactly(AdditionalRegistration.builder().discoveryServiceUrls("https://eureka").routes(emptyList()).build());
        }

        @Test
        void shouldParseBadIndexToEmptyRegistrations() {
            Map<String, String> allProperties = Maps.of(
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION__DISCOVERYSERVICEURLS", "https://eureka");
            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(allProperties);

            assertThat(registrations).isEmpty();
        }
    }

    @Nested
    class GivenValidEnvironmentPropertiesAreProvided {

        private final Map<String, String> envProperties = new TreeMap<>();

        @BeforeEach
        void setUp() {
            envProperties.putAll(
                Maps.of(
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_DISCOVERYSERVICEURLS", "https://eureka",
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_SERVICEURL", "/",
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_0_ROUTES_0_GATEWAYURL", "/")
            );
        }

        @Test
        void shouldParseFirstAdditionalRegistration() {

            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(envProperties);

            AdditionalRegistration expectedRegistration = new AdditionalRegistration("https://eureka", singletonList(new AdditionalRegistration.Route("/", "/")));

            assertThat(registrations).hasSize(1);
            assertThat(registrations.get(0)).isEqualTo(expectedRegistration);
        }

        @Test
        void shouldParseAdditionalRegistrationWithoutRoutes() {

            envProperties.putAll(
                Maps.of(
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_DISCOVERYSERVICEURLS", "https://eureka-2",
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_SERVICEURL", "",
                    "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_GATEWAYURL", null)
            );

            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(envProperties);

            AdditionalRegistration expectedSecondRegistration = new AdditionalRegistration("https://eureka-2", Collections.emptyList());

            assertThat(registrations).hasSize(2);
            assertThat(registrations.get(1)).isEqualTo(expectedSecondRegistration);
        }

        @Test
        void shouldParseAdditionalRegistrationWithPartiallyDefinedRoutes() {
            Map<String, String> properties = Maps.of(
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_DISCOVERYSERVICEURLS", "https://eureka-2",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_SERVICEURL", "",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_0_GATEWAYURL", null,
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_1_SERVICEURL", "/serviceUrl",
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_1_GATEWAYURL", null,
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_2_SERVICEURL", null,
                "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_1_ROUTES_2_GATEWAYURL", "/gatewayUrl");

            List<AdditionalRegistration> registrations = AdditionalRegistrationConfig.extractAdditionalRegistrations(properties);

            AdditionalRegistration expectedSecondRegistration = new AdditionalRegistration("https://eureka-2", Arrays.asList(new AdditionalRegistration.Route(null, "/serviceUrl"), new AdditionalRegistration.Route("/gatewayUrl", null)));

            assertThat(registrations).hasSize(1);
            assertThat(registrations.get(0).getRoutes()).hasSize(2);
            assertThat(registrations.get(0)).isEqualTo(expectedSecondRegistration);
        }
    }
}
