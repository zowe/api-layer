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

import com.ca.apiml.security.client.config.MessageServiceConfig;
import com.ca.apiml.security.common.config.HandlerInitializer;
import com.ca.apiml.security.common.content.BasicContentFilter;
import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Main class configuring Spring security for Discovery Service
 *
 * This configuration is applied if "https" Spring profile is not active
 */
@Configuration
@ComponentScan({
    "com.ca.apiml.security.common",
})
@Import(MessageServiceConfig.class)
@EnableWebSecurity
@RequiredArgsConstructor
@Profile("!https")
public class HttpWebSecurityConfig extends AbstractWebSecurityConfigurer {
    private static final String DISCOVERY_REALM = "API Mediation Discovery Service realm";

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    @Value("${apiml.discovery.userid:eureka}")
    private String eurekaUserid;

    @Value("${apiml.discovery.password:password}")
    private String eurekaPassword;

    @Autowired
    public void configureGlobal(AuthenticationManagerBuilder auth) throws Exception {
        auth.inMemoryAuthentication().withUser(eurekaUserid).password("{noop}" + eurekaPassword).roles("EUREKA");
    }
    private final HandlerInitializer handlerInitializer;

    @Override
    public void configure(WebSecurity web) {
        String[] noSecurityAntMatchers = {
            "/favicon.ico",
            "/eureka/css/**",
            "/eureka/js/**",
            "/eureka/fonts/**",
            "/eureka/images/**"
        };
        web.ignoring().antMatchers(noSecurityAntMatchers);
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        apimlLog.log("com.ca.mfaas.discovery.http");
        baseConfigure(http)
            .addFilterBefore(basicFilter(authenticationManager()), UsernamePasswordAuthenticationFilter.class)
            .httpBasic().realmName(DISCOVERY_REALM)
            .and()
            .authorizeRequests()
            .antMatchers("/application/info", "/application/health").permitAll()
            .antMatchers("/**").authenticated();
    }

    private BasicContentFilter basicFilter(AuthenticationManager authenticationManager) {
        return new BasicContentFilter(authenticationManager, handlerInitializer.getAuthenticationFailureHandler(), handlerInitializer.getResourceAccessExceptionHandler());
    }

}
