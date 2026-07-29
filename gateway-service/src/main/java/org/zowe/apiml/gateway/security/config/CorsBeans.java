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
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.zowe.apiml.util.CorsUtils;

import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Externalized configuration of CORS behavior
 */
@ConditionalOnProperty(name = "apiml.security.filterChainConfiguration", havingValue = "new", matchIfMissing = false)
@Configuration
@RequiredArgsConstructor
@Slf4j
public class CorsBeans implements InitializingBean {

    @Value("${apiml.service.corsEnabled:false}")
    private boolean gatewayCorsEnabled;

    @Value("${apiml.service.corsDefaultAllowedOrigins:#{null}}")
    private String corsDefaultAllowedOrigins;

    @Value("${apiml.service.corsDefaultAllowedHeaders:*}")
    private String corsDefaultAllowedHeaders;

    @Value("${apiml.service.ignoredHeadersWhenCorsEnabled}")
    private String ignoredHeadersWhenCorsEnabled;

    @Value("${apiml.service.corsAllowedEndpoints:/*/*/gateway/**,/gateway/*/*/**,/gateway/version}")
    private List<String> corsEnabledEndpoints;

    @Value("${apiml.service.corsAllowedMethods:GET,HEAD,POST,PATCH,DELETE,PUT,OPTIONS}")
    private List<String> corsAllowedMethods;

    @Value("${apiml.service.hostname:localhost}")
    private String hostname;

    @Value("${apiml.service.port}")
    private String port;

    private final ZuulProperties zuulProperties;

    @Override
    public void afterPropertiesSet() throws Exception {
        if (corsDefaultAllowedOrigins == null || corsDefaultAllowedOrigins.isEmpty()) {
            corsDefaultAllowedOrigins = "https://" + hostname + ":" + port;
        }
        if (corsDefaultAllowedHeaders == null || corsDefaultAllowedHeaders.isEmpty()) {
            corsDefaultAllowedHeaders = CorsConfiguration.ALL;
        }
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(CorsUtils corsUtils) {
        final UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (gatewayCorsEnabled) {
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
        List<String> externalDomains,
        String hostname,
        int port
    ) throws URISyntaxException {
        Set<String> defaultAllowedOrigins = new HashSet<>();
        if (corsDefaultAllowedOrigins != null) {
            defaultAllowedOrigins.addAll(Arrays.asList(corsDefaultAllowedOrigins.split(",")));
        }
        externalDomains.stream().filter(StringUtils::isNotBlank).forEach(defaultAllowedOrigins::add);

        return new ArrayList<>(defaultAllowedOrigins);
    }

    @Bean
    CorsUtils corsUtils(
        Environment environment,
        @Value("${apiml.service.externalUrl:}") String externalUrl,
        @Value("${server.hostname:${apiml.service.hostname}}") String hostname,
        @Value("${server.port}") int port
    ) throws URISyntaxException {
        return CorsUtils.builder()
            .gatewayCorsEnabled(gatewayCorsEnabled)
            .corsAllowedEndpoints(corsEnabledEndpoints)
            .defaultAllowedCorsHttpMethods(corsAllowedMethods)
            .defaultAllowedCorsHeaders(Arrays.asList(corsDefaultAllowedHeaders.split(",")))
            .defaultAllowedCorsOrigins(getDefaultAllowedOrigins(environment, new ArrayList<>(Arrays.asList(externalUrl)), hostname, port))
            .build();

    }

}
