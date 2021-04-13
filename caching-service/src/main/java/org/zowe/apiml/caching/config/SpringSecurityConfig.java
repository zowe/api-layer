/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifyCertificates;

    @Override
    public void configure(WebSecurity web) throws Exception {
        String[] noSecurityAntMatchers = {
            "/application/health",
            "/application/info",
            "/v2/api-docs"
        };
        web.ignoring().antMatchers(noSecurityAntMatchers);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable()   // NOSONAR
            .headers().httpStrictTransportSecurity().disable()
            .and().sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        if (verifyCertificates) {
            http.authorizeRequests().anyRequest().authenticated().and()
                .x509().userDetailsService(x509UserDetailsService());
        } else {
            http.authorizeRequests().anyRequest().permitAll();
        }

        }

    private UserDetailsService x509UserDetailsService() {
        return username -> new User("cachingUser", "", Collections.emptyList());
    }
}
