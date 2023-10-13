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

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static java.util.Collections.singletonList;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class AdditionalRegistrationConfig {

    public static final String PREFIX = "ZWE_CONFIGS_APIML_SERVICE_ADDITIONALREGISTRATION_";

    @Bean
    public List<AdditionalRegistration> additionalRegistration() {
        List<AdditionalRegistration> additionalRegistrations = extractAdditionalRegistrations(System.getenv());
        log.debug("Parsed {} additional regs, \t first: {}", additionalRegistrations.size(), additionalRegistrations.stream().findFirst().orElse(null));
        return additionalRegistrations;
    }

    static List<AdditionalRegistration> extractAdditionalRegistrations(Map<String, String> allProperties) {

        Map<String, String> additionalProperties = allProperties.entrySet().stream().filter(entry -> entry.getKey().startsWith(PREFIX))
            .map(entry -> new AbstractMap.SimpleEntry<String, String>(entry.getKey().replace(PREFIX, ""), entry.getValue()))
            .collect(Collectors.toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (previous, current) -> current, TreeMap::new));

        for (Map.Entry<String, String> prp : additionalProperties.entrySet()) {
            log.debug("Additional property: {}={}", prp.getKey(), prp.getValue());
        }

        Integer listSize = additionalProperties.keySet().stream().map(key -> parsePropertyName(key).getKey())
            .max(Integer::compareTo).map(maxIndex -> maxIndex + 1).orElse(0);

        List<AdditionalRegistration> additionalRegistrations = IntStream.range(0, listSize)
            .mapToObj(index -> AdditionalRegistration.builder().routes(singletonList(new AdditionalRegistration.Route())).build())
            .collect(Collectors.toList());

        mapProperties(additionalRegistrations, additionalProperties);


        return additionalRegistrations.stream()
            .filter(registration -> StringUtils.isNotBlank(registration.getDiscoveryServiceUrls())).collect(Collectors.toList());
    }

    private static void mapProperties(List<AdditionalRegistration> additionalRegistrations, Map<String, String> properties) {
        for (Map.Entry<String, String> entry : properties.entrySet()) {
            Pair<Integer, String> property = parsePropertyName(entry.getKey());
            String propertyValue = entry.getValue();
            switch (property.getValue()) {
                case "DISCOVERYSERVICEURLS":
                    additionalRegistrations.get(property.getKey()).setDiscoveryServiceUrls(propertyValue);
                    break;
                case "GATEWAYURL":
                    additionalRegistrations.get(property.getKey()).getRoutes().get(0).setGatewayUrl(propertyValue);
                    break;
                case "SERVICEURL":
                    additionalRegistrations.get(property.getKey()).getRoutes().get(0).setServiceUrl(propertyValue);
                    break;
                default:
                    break;
            }
        }
    }

    private static Pair<Integer, String> parsePropertyName(String fullPropertyName) {
        String[] parts = StringUtils.split(fullPropertyName, "_");
        if (StringUtils.isNumeric(parts[0])) {
            return Pair.of(Integer.parseInt(parts[0]), parts[parts.length - 1]);
        }
        return Pair.of(-1, null);
    }
}
