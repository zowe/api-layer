/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package io.apiml.security.service.login.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.apiml.security.service.authentication.ServiceLoginAuthentication;
import io.apiml.security.service.login.dto.ServiceLoginRequest;
import io.apiml.security.service.login.exception.ServiceLoginRequestFormatException;
import io.apiml.security.service.login.exception.WrongLoginMethodException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.AbstractAuthenticationProcessingFilter;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;

@Slf4j
public class ServiceLoginFilter extends AbstractAuthenticationProcessingFilter {
    private final String loginPath;
    private final AuthenticationSuccessHandler successHandler;
    private final AuthenticationFailureHandler failureHandler;
    private final ObjectMapper mapper;

    public ServiceLoginFilter(
        String loginPath,
        AuthenticationSuccessHandler successHandler,
        AuthenticationFailureHandler failureHandler,
        ObjectMapper mapper,
        AuthenticationManager authenticationManager) {
            super(loginPath);
            this.loginPath = loginPath;
            this.successHandler = successHandler;
            this.failureHandler = failureHandler;
            this.mapper = mapper;
            this.setAuthenticationManager(authenticationManager);
    }

    @Override
    public Authentication attemptAuthentication(HttpServletRequest request, HttpServletResponse response)
        throws AuthenticationException {
        if (!correctHttpMethod(request)) {
            log.debug("Authentication: wrong HTTP method was used to access login endpoint");
            throw new WrongLoginMethodException("This HTTP method is not supported");
        }
        ServiceLoginRequest loginRequest = parseLoginRequest(request);
        ServiceLoginAuthentication authentication = new ServiceLoginAuthentication(loginRequest);
        return this.getAuthenticationManager().authenticate(authentication);
    }

    @Override
    protected void successfulAuthentication(HttpServletRequest request, HttpServletResponse response, FilterChain chain, Authentication authResult) throws IOException, ServletException {
        successHandler.onAuthenticationSuccess(request, response, authResult);
    }

    @Override
    protected void unsuccessfulAuthentication(HttpServletRequest request, HttpServletResponse response, AuthenticationException failed) throws IOException, ServletException {
        SecurityContextHolder.clearContext();
        failureHandler.onAuthenticationFailure(request, response, failed);
    }

    public String getLoginPath() {
        return loginPath;
    }

    private boolean correctHttpMethod(HttpServletRequest request) {
        if (request.getMethod().equals(HttpMethod.POST.name())) {
            return true;
        }
        return false;
    }

    private ServiceLoginRequest parseLoginRequest(HttpServletRequest request) {
        try {
            ServiceLoginRequest loginRequest = mapper.readValue(request.getInputStream(), ServiceLoginRequest.class);
            if (StringUtils.isBlank(loginRequest.getUsername()) || StringUtils.isBlank(loginRequest.getPassword())) {
                logger.debug("Authentication: username or password is blank");
                throw new ServiceLoginRequestFormatException("Login object has wrong format");
            } else {
                return loginRequest;
            }
        } catch (IOException e) {
            logger.debug("Authentication: login object has wrong format");
            throw new ServiceLoginRequestFormatException("Login object has wrong format");
        }
    }
}
