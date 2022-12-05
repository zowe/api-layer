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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.web.cors.*;
import org.zowe.apiml.util.CorsUtils;

import java.util.*;

/**
 * Externalized configuration of CORS behavior
 */
@ConditionalOnProperty(name = "apiml.security.filterChainConfiguration", havingValue = "new", matchIfMissing = false)
@Configuration
@RequiredArgsConstructor
public class CorsBeans {

    private static final List<String> CORS_ENABLED_ENDPOINTS = Arrays.asList("/*/*/gateway/**", "/gateway/*/*/**", "/gateway/version");

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

    @Bean
    CorsUtils corsUtils(){
        return new CorsUtils(corsEnabled);
    }
}
