/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.service.discovery.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;

import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
@Order(1)
public class EurekaSecurityConfiguration extends WebSecurityConfigurerAdapter {
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    @Value("${apiml.service.id:#{null}}")
    private String serviceId;

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Value("${apiml.security.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Autowired
    private Environment environment;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(eurekaUserid).password("{noop}" + eurekaPassword).roles("EUREKA");
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
        http.headers().httpStrictTransportSecurity().disable();

        if (Arrays.asList(environment.getActiveProfiles()).contains("https")) {
            if (verifySslCertificatesOfServices) {
                http.antMatcher("/eureka/**").authorizeRequests().anyRequest().authenticated().and().x509().userDetailsService(x509UserDetailsService());
                http.antMatcher("/discovery/**").authorizeRequests().anyRequest().authenticated().and().x509().userDetailsService(x509UserDetailsService());
            }
            http.httpBasic().realmName(DISCOVERY_REALM).and().antMatcher("/*").authorizeRequests().anyRequest().authenticated();
        } else {
            http.httpBasic().realmName(DISCOVERY_REALM).and().antMatcher("/**").authorizeRequests().anyRequest().authenticated();
        }
    }

    public UserDetailsService x509UserDetailsService() {
        return new UserDetailsService() {
            @Override
            public UserDetails loadUserByUsername(String username) {
                return new User("eurekaClient", "", Collections.emptyList());
            }
        };
    }
}
