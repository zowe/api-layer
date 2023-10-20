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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.apache.commons.lang.StringUtils.isNotBlank;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdditionalRegistrationConfig {

    public static final String ADDITIONAL_REGISTRATION_INDEX_GROUP_NAME = "index";
    public static final Pattern DISCOVERYSERVICEURLS_PATTERN = Pattern.compile("^ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_(?<index>\\d+)_DISCOVERYSERVICEURLS$");
    public static final Pattern ROUTE_SERVICEURL_PATTERN = Pattern.compile("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_(?<index>\\d+)_ROUTES_(?<routeIndex>\\d+)_SERVICEURL");
    public static final Pattern ROUTE_GATEWAYURL_PATTERN = Pattern.compile("ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_(?<index>\\d+)_ROUTES_(?<routeIndex>\\d+)_GATEWAYURL");

    @Bean
    public List<AdditionalRegistration> additionalRegistration(StandardEnvironment environment) {
        List<AdditionalRegistration> additionalRegistrations = extractAdditionalRegistrations(System.getenv());
        log.debug("Parsed {} additional regs, \t first: {}", additionalRegistrations.size(), additionalRegistrations.stream().findFirst().orElse(null));
        return additionalRegistrations;
    }

    public static List<AdditionalRegistration> extractAdditionalRegistrations(Map<String, String> allProperties) {
        if (allProperties == null) {
            return new ArrayList<>();
        }
        final Map<Integer, AdditionalRegistration> map = new TreeMap<>();
        for (Map.Entry<String, String> entry : allProperties.entrySet()) {
            matchDiscoveryUrl(entry.getKey()).ifPresent(index -> putAdditionalRegistration(map, index, entry.getValue()));
        }
        for (Map.Entry<String, String> entry : allProperties.entrySet()) {
            parseServiceUrl(entry.getKey()).ifPresent(pair -> putRouteServiceUrl(map, pair, entry.getValue()));
        }

        for (Map.Entry<String, String> entry : allProperties.entrySet()) {
            parseGatewayUrl(entry.getKey()).ifPresent(pair -> putRouteGatewayUrl(map, pair, entry.getValue()));
        }
        map.values().forEach(registration -> registration.setRoutes(registration.getRoutes().stream()
            .filter(AdditionalRegistrationConfig::isRouteDefined).collect(Collectors.toList())));
        return new ArrayList<>(map.values());
    }

    private static void putRouteServiceUrl(Map<Integer, AdditionalRegistration> map, Pair<Integer, Long> pair, String value) {
        AdditionalRegistration registration = map.get(pair.getKey());
        if (registration != null && StringUtils.isNoneBlank(value)) {
            int routeIndex = pair.getValue().intValue();
            while (registration.getRoutes().size() <= routeIndex) {
                registration.getRoutes().add(new AdditionalRegistration.Route());
            }
            registration.getRoutes().get(routeIndex).setServiceUrl(value);
        }
    }

    private static void putRouteGatewayUrl(Map<Integer, AdditionalRegistration> map, Pair<Integer, Long> pair, String value) {
        AdditionalRegistration registration = map.get(pair.getKey());
        if (registration != null && StringUtils.isNoneBlank(value)) {
            int routeIndex = pair.getValue().intValue();
            while (registration.getRoutes().size() <= routeIndex) {
                registration.getRoutes().add(new AdditionalRegistration.Route());
            }
            registration.getRoutes().get(routeIndex).setGatewayUrl(value);
        }
    }

    private static void putAdditionalRegistration(Map<Integer, AdditionalRegistration> map, Integer index, String value) {
        if (StringUtils.isNoneBlank(value)) {
            map.put(index, AdditionalRegistration.builder().discoveryServiceUrls(value).routes(new ArrayList<>()).build());
        }
    }

    public static Optional<Integer> matchDiscoveryUrl(String line) {
        Matcher matcher = DISCOVERYSERVICEURLS_PATTERN.matcher(line);
        if (matcher.matches()) {
            String index = matcher.group(ADDITIONAL_REGISTRATION_INDEX_GROUP_NAME);
            return Optional.ofNullable(index).map(Integer::parseInt);
        }
        return Optional.empty();
    }

    public static Optional<Pair<Integer, Long>> parseServiceUrl(String line) {
        Matcher matcher = ROUTE_SERVICEURL_PATTERN.matcher(line);
        if (matcher.matches()) {

            String index = matcher.group(ADDITIONAL_REGISTRATION_INDEX_GROUP_NAME);
            String routeIndex = matcher.group("routeIndex");
            return Optional.of(Pair.of(Integer.decode(index), Long.decode(routeIndex)));
        }
        return Optional.empty();
    }

    public static Optional<Pair<Integer, Long>> parseGatewayUrl(String line) {
        Matcher matcher = ROUTE_GATEWAYURL_PATTERN.matcher(line);
        if (matcher.matches()) {
            String index = matcher.group(ADDITIONAL_REGISTRATION_INDEX_GROUP_NAME);
            String routeIndex = matcher.group("routeIndex");
            return Optional.of(Pair.of(Integer.decode(index), Long.decode(routeIndex)));
        }
        return Optional.empty();
    }

    private static boolean isRouteDefined(AdditionalRegistration.Route route) {
        return route != null && (isNotBlank(route.getGatewayUrl()) || isNotBlank(route.getServiceUrl()));
    }

}
