/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.securedservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiml.security.service.configuration.ServiceSpringSecurityConfiguration;
import io.apiml.security.service.login.filter.ServiceLoginFilter;
import io.apiml.security.service.login.handler.ServiceLoginFailureHandler;
import io.apiml.security.service.login.handler.ServiceLoginSuccessfulHandler;
import io.apiml.security.service.login.service.GatewayUserService;
import io.apiml.security.service.login.service.UserService;
import io.apiml.security.service.query.GatewayQueryService;
import io.apiml.security.service.query.ServiceCookieTokenFilter;
import io.apiml.security.service.query.ServiceHeaderTokenFilter;
import io.apiml.security.service.query.ServiceTokenQueryFailureHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.AuthenticationManager;

@Configuration
@ComponentScan(value = {"io.apiml.security.service", "io.apiml.security.common"})
@Import(ServiceSpringSecurityConfiguration.class)
public class ServiceSpringConfiguration {
    private final String LOGIN_ENDPOINT = "/auth/login";

    @Autowired
    private ServiceLoginSuccessfulHandler serviceLoginSuccessfulHandler;
    @Autowired
    private ServiceLoginFailureHandler serviceLoginFailureHandler;
    @Autowired
    private ServiceTokenQueryFailureHandler serviceTokenQueryFailureHandler;

    @Autowired
    private ObjectMapper objectMapper;

    private final AuthenticationManager authenticationManager;

    public ServiceSpringConfiguration(@Lazy AuthenticationManager authenticationManager) {
        this.authenticationManager = authenticationManager;
    }

    @Bean
    public ServiceLoginFilter loginFilter() {
        return new ServiceLoginFilter(LOGIN_ENDPOINT, serviceLoginSuccessfulHandler, serviceLoginFailureHandler, objectMapper, authenticationManager);
    }

    @Bean
    public UserService userService() {
        return new GatewayUserService("http://localhost:10010/auth/login");
    }

    @Bean
    public GatewayQueryService gatewayQueryService() {
        return new GatewayQueryService("http://localhost:10010/auth/query");
    }

    @Bean
    public ServiceCookieTokenFilter serviceCookieTokenFilter() {
        return new ServiceCookieTokenFilter(authenticationManager, serviceTokenQueryFailureHandler);
    }

    @Bean
    public ServiceHeaderTokenFilter serviceHeaderTokenFilter() {
        return new ServiceHeaderTokenFilter(authenticationManager, serviceTokenQueryFailureHandler);
    }
}
