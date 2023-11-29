/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.util.CorsUtils;

import java.net.URISyntaxException;
import java.util.*;

/**
 * Externalized configuration of CORS behavior
 */
@ConditionalOnProperty(name = "apiml.security.filterChainConfiguration", havingValue = "new", matchIfMissing = false)
@Configuration
@RequiredArgsConstructor
public class CorsBeans {

    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;
    @Value("${apiml.service.ignoredHeadersWhenCorsEnabled}")
    private String ignoredHeadersWhenCorsEnabled;

    private final ZuulProperties zuulProperties;

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsUtils corsUtils) {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (corsEnabled) {
            addCorsRelatedIgnoredHeaders();
        }
        corsUtils.registerDefaultCorsConfiguration(source::registerCorsConfiguration);
        return source;
    }

    private void addCorsRelatedIgnoredHeaders() {
        zuulProperties.setIgnoredHeaders(new HashSet<>(
            Arrays.asList((ignoredHeadersWhenCorsEnabled).split(","))
        ));
    }

    List<String> getDefaultAllowedOrigins(
        Environment environment,
        boolean ssl,
        String externalUrl,
        String hostname,
        int port
    ) throws URISyntaxException {
        if (corsEnabled) return null;

        boolean attls = Arrays.asList(environment.getActiveProfiles()).contains("attls");
        if (!attls) {
            // TODO: this method is a hotfix for AT-TLS, but it could be a breaking change, verify no-ATTLS configuration in v3
            return null;
        }

        Set<String> gatewayOrigins = new HashSet<>();
        if (StringUtils.isNotBlank(externalUrl)) {
            gatewayOrigins.add(externalUrl);
        }
        gatewayOrigins.add(new URIBuilder()
            .setScheme(attls || ssl ? "https" : "http")
            .setHost(hostname)
            .setPort(port)
            .build().toString()
        );

        return new ArrayList<>(gatewayOrigins);
    }
    @Bean
    CorsUtils corsUtils(
        Environment environment,
        @Value("${server.ssl.enabled}") boolean ssl,
        @Value("${apiml.service.externalUrl:}") String externalUrl,
        @Value("${server.hostname:${apiml.service.hostname}}") String hostname,
        @Value("${server.port}") int port
    ) throws URISyntaxException {

        return new CorsUtils(corsEnabled, getDefaultAllowedOrigins(environment, ssl, externalUrl, hostname, port));
    }
}
