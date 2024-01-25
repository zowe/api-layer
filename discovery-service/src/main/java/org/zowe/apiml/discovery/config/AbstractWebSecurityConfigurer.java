/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.config;

import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.CsrfConfigurer;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;

public abstract class AbstractWebSecurityConfigurer {

    protected HttpSecurity baseConfigure(HttpSecurity http) throws Exception {
        return http.csrf(CsrfConfigurer::disable)    // NOSONAR
            .headers(httpSecurityHeadersConfigurer ->
                httpSecurityHeadersConfigurer.httpStrictTransportSecurity(HeadersConfigurer.HstsConfig::disable))
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
    }
}
