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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.util.CollectionUtils;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdditionalRegistrationConfig {

    public static final String DISCOVERY_SERVICE_URLS_KEY = "DISCOVERYSERVICEURLS";
    public static final String GATEWAY_URL_KEY = "GATEWAYURL";
    public static final String COMMON_PREFIX = "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_";
    public static final String SERVICE_URL_KEY = "SERVICEURL";
    private static final int EXPECTED_ROUTE_PART_INDEX = 2;

    @Bean
    public List<AdditionalRegistration> additionalRegistration(StandardEnvironment environment) {
        List<AdditionalRegistration> additionalRegistrations = extractAdditionalRegistrations(System.getenv());
        log.debug("Parsed {} additional regs, \t first: {}", additionalRegistrations.size(), additionalRegistrations.stream().findFirst().orElse(null));
        return additionalRegistrations;
    }

    static List<AdditionalRegistration> extractAdditionalRegistrations(Map<String, String> allProperties) {

        if (CollectionUtils.isEmpty(allProperties)) {
            return Collections.emptyList();
        }

        Map<String, String> additionalProperties = allProperties.entrySet().stream().filter(entry -> entry.getKey().startsWith(COMMON_PREFIX))
            .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
            .map(entry -> new AbstractMap.SimpleEntry<>(entry.getKey().replace(COMMON_PREFIX, ""), entry.getValue()))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (previous, current) -> current, TreeMap::new));

        for (Map.Entry<String, String> prp : additionalProperties.entrySet()) {
            log.debug("Additional property: {}={}", prp.getKey(), prp.getValue());
        }

        Integer listSize = additionalProperties.keySet().stream().map(key -> parsePropertyName(key).getKey())
            .max(Integer::compareTo).map(maxIndex -> maxIndex + 1).orElse(0);

        List<AdditionalRegistration> additionalRegistrations = IntStream.range(0, listSize)
            .mapToObj(index -> AdditionalRegistration.builder().routes(new ArrayList<>()).build())
            .collect(Collectors.toList());

        mapProperties(additionalRegistrations, additionalProperties);
        for (AdditionalRegistration registration : additionalRegistrations) {
            List<AdditionalRegistration.Route> definedRoutes = registration.getRoutes().stream().filter(route -> !AdditionalRegistrationConfig.isRouteDefined(route)).collect(Collectors.toList());
            registration.setRoutes(definedRoutes);
        }

        return additionalRegistrations.stream()
            .filter(registration -> StringUtils.isNotBlank(registration.getDiscoveryServiceUrls()))
            .collect(Collectors.toList());
    }

    private static void mapProperties(List<AdditionalRegistration> additionalRegistrations, Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            String propertyKey = entry.getKey();
            Pair<Integer, String> property = parsePropertyName(propertyKey);
            if (property.getKey() == -1) {
                continue;
            }
            final String propertyValue = entry.getValue();
            switch (StringUtils.upperCase(property.getValue())) {
                case DISCOVERY_SERVICE_URLS_KEY:
                    additionalRegistrations.get(property.getKey()).setDiscoveryServiceUrls(propertyValue);
                    break;
                case GATEWAY_URL_KEY:
                    setRouteProperty(additionalRegistrations.get(property.getKey()), parseRouteIndex(propertyKey), (AdditionalRegistration.Route route) -> route.setGatewayUrl(propertyValue));
                    break;
                case SERVICE_URL_KEY:
                    setRouteProperty(additionalRegistrations.get(property.getKey()), parseRouteIndex(propertyKey), (AdditionalRegistration.Route route) -> route.setServiceUrl(propertyValue));
                    break;
                default:
                    break;
            }
        }
    }

    private static void setRouteProperty(AdditionalRegistration registration, int routeIndex, Consumer<AdditionalRegistration.Route> setter) {
        if (routeIndex > -1) {
            if (registration.getRoutes().size() <= routeIndex) {
                registration.getRoutes().add(new AdditionalRegistration.Route());
                setRouteProperty(registration, routeIndex, setter);
            }
            setter.accept(registration.getRoutes().get(routeIndex));
        }
    }

    private static int parseRouteIndex(String propertyName) {
        String[] parts = StringUtils.split(propertyName, "_.");
        if (parts.length > EXPECTED_ROUTE_PART_INDEX && StringUtils.isNumeric(parts[EXPECTED_ROUTE_PART_INDEX])) {
            return Integer.parseInt(parts[EXPECTED_ROUTE_PART_INDEX]);
        }
        return -1;
    }

    private static Pair<Integer, String> parsePropertyName(String fullPropertyName) {
        String[] parts = StringUtils.split(fullPropertyName, "_");
        if (StringUtils.isNumeric(parts[0])) {
            return Pair.of(Integer.parseInt(parts[0]), parts[parts.length - 1]);
        }
        return Pair.of(-1, null);
    }

    private static boolean isRouteDefined(AdditionalRegistration.Route route) {
        return route == null || StringUtils.isBlank(route.getGatewayUrl()) && StringUtils.isBlank(route.getServiceUrl());
    }
}
