package com.ca.mfaas.gateway.security;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;

@Configuration
@EnableWebSecurity
@ComponentScan(value = { "io.apiml.security.gateway", "io.apiml.security.common" })
public class GatewaySecurityConfiguration extends WebSecurityConfigurerAdapter {
    @Override
    public void configure(WebSecurity web) {
        String[] ignoreSecurity = { "/**" };
        web.ignoring().antMatchers(ignoreSecurity);
    }
}
