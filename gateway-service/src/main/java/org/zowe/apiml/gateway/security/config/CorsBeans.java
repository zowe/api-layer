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
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class CorsBeans {

    @Value("${apiml.service.corsEnabled:false}")
    private boolean corsEnabled;
    @Value("${apiml.service.corsAllowedMethods:GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS}")
    private List<String> corsAllowedMethods;
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

    List<String> getDefaultAllowedOrigins( // TODO: this method is a hotfix for AT-TLS, but it could be a breaking change, verify no-ATTLS configuration in v3
        Environment environment,
        List<String> externalUrls,
        String hostname,
        int port
    ) throws URISyntaxException {
        boolean isClientAttlsEnabled = Arrays.asList(environment.getActiveProfiles()).contains("attlsClient");
        if (corsEnabled || !isClientAttlsEnabled) {
            return null;
        }

        Set<String> gatewayOrigins = new HashSet<>();
        externalUrls.stream().filter(StringUtils::isNotBlank).forEach(gatewayOrigins::add);
        gatewayOrigins.add(new URIBuilder()
            .setScheme("https")
            .setHost(hostname)
            .setPort(port)
            .build().toString()
        );

        return new ArrayList<>(gatewayOrigins);
    }
    @Bean
    CorsUtils corsUtils(
        Environment environment,
        @Value("${apiml.service.externalUrl:}") String externalUrl, // FIXME Should support multiple external URLs
        @Value("${server.hostname:${apiml.service.hostname}}") String hostname,
        @Value("${server.port}") int port
    ) throws URISyntaxException {
        return new CorsUtils(
            corsEnabled,
            corsAllowedMethods,
            getDefaultAllowedOrigins(environment, new ArrayList<>(Arrays.asList(externalUrl)),
            hostname,
            port));
    }

}
