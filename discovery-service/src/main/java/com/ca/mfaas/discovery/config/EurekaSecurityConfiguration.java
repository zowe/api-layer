/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.discovery.config;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@Order(1)
public class EurekaSecurityConfiguration extends WebSecurityConfigurerAdapter {

    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    private final MFaaSConfigPropertiesContainer propertiesContainer;

    @Autowired
    public EurekaSecurityConfiguration(MFaaSConfigPropertiesContainer propertiesContainer) {
        this.propertiesContainer = propertiesContainer;
    }

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        // for inMemory Authentication  {noop} for plain text
        auth.inMemoryAuthentication()
            .withUser(propertiesContainer.getDiscovery().getEurekaUserName())
            .password("{noop}" + propertiesContainer.getDiscovery().getEurekaUserPassword())
            .roles("EUREKA");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .httpBasic()
            .realmName(DISCOVERY_REALM)
            .and()
            .antMatcher("/**")
            .authorizeRequests()
            .anyRequest().authenticated();
        http.csrf().disable();
    }
}
