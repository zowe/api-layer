/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.registry.client.spring;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * How this service talks to the Discovery Service.
 * <p>
 * Bound from {@code eureka.client.*}; see {@link RegistryInstanceProperties} for why the prefix stayed. Only
 * the properties APIML actually sets are carried over - Eureka's client had upwards of forty, most of them
 * describing AWS regions, availability zones, DNS-based server discovery and backup registries that APIML has
 * never used.
 */
@Data
@ConfigurationProperties("eureka.client")
public class RegistryFetchProperties {

    /** Turns the whole client off: no registration, no fetching, no beans doing background work. */
    private boolean enabled = true;

    private boolean registerWithEureka = true;

    private boolean fetchRegistry = true;

    private int registryFetchIntervalSeconds = 30;

    /** How often the registration is renewed. Eureka called this instance info replication. */
    private int instanceInfoReplicationIntervalSeconds = 30;

    private int initialInstanceInfoReplicationIntervalSeconds = 40;

    private int eurekaServerConnectTimeoutSeconds = 5;

    private int eurekaServerReadTimeoutSeconds = 8;

    private boolean shouldUnregisterOnShutdown = true;

    /** {@code serviceUrl.defaultZone}, comma-separated. Zones beyond the default were never used. */
    private Map<String, String> serviceUrl = new LinkedHashMap<>();

    private Healthcheck healthcheck = new Healthcheck();

    @Data
    public static class Healthcheck {

        /**
         * When enabled the registered status follows Spring Boot's health endpoint, so a service that is up
         * but unhealthy is taken out of the routing table instead of receiving traffic.
         */
        private boolean enabled = false;

    }

    public List<String> discoveryServiceUrls() {
        List<String> urls = new ArrayList<>();
        for (String value : serviceUrl.values()) {
            if (value == null) {
                continue;
            }
            Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(url -> !url.isEmpty())
                .forEach(urls::add);
        }
        return urls;
    }

}
