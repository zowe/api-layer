/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.security;

import com.ca.mfaas.security.token.CookieConfiguration;
import com.ca.mfaas.security.token.TokenServiceConfiguration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class ComponentsConfiguration {

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public TokenServiceConfiguration tokenServiceConfiguration() {
        String secret = "secret";
        long expirationInSeconds = 60 * 60 * 12L; // 12 hours
        String issuer = "api-catalog-services";

        return TokenServiceConfiguration.builder()
            .secret(secret)
            .expirationInSeconds(expirationInSeconds)
            .issuer(issuer)
            .shortTtlUsername("expire")
            .shortTtlExpiration(1)
            .build();
    }

    @Bean
    public CookieConfiguration cookieConfiguration(@Value("${security.cookie.maxAge:86400}") Integer maxAge,
                                                   @Value("${security.cookie.secure:true}") Boolean secure) {
        return CookieConfiguration.builder()
            .name("apimlAuthenticationToken")
            .secure(secure)
            .path("/")
            .comment("API Catalog security token")
            .maxAge(maxAge)
            .build();
    }
}
