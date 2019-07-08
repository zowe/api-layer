/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.apiml.security.content;

import com.ca.apiml.security.error.NotFoundExceptionHandler;
import com.ca.mfaas.constants.ApimlConstants;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

import javax.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Optional;

/**
 * Authenticate the credentials from the basic authorization header
 */
@Slf4j
public class BasicContentFilter extends AbstractSecureContentFilter {

    public BasicContentFilter(AuthenticationManager authenticationManager,
                              AuthenticationFailureHandler failureHandler,
                              NotFoundExceptionHandler notFoundExceptionHandler) {
        super(authenticationManager, failureHandler, notFoundExceptionHandler, new String[0]);
    }

    public BasicContentFilter(AuthenticationManager authenticationManager,
                              AuthenticationFailureHandler failureHandler,
                              NotFoundExceptionHandler notFoundExceptionHandler,
                              String[] endpoints) {
        super(authenticationManager, failureHandler, notFoundExceptionHandler, endpoints);
    }

    /**
     * Extract credentials from the authorization header in the request and decode them
     *
     * @param request the http request
     * @return the decoded credentials
     */
    public Optional<AbstractAuthenticationToken> extractContent(HttpServletRequest request) {
        return Optional.ofNullable(
            request.getHeader(HttpHeaders.AUTHORIZATION)
        ).filter(
            header -> header.startsWith(ApimlConstants.BASIC_AUTHENTICATION_PREFIX)
        ).map(
            header -> header.replaceFirst(ApimlConstants.BASIC_AUTHENTICATION_PREFIX, "").trim()
        )
            .filter(base64Credentials -> !base64Credentials.isEmpty())
            .map(this::mapBase64Credentials);
    }

    /**
     * Decode the encoded credentials
     *
     * @param base64Credentials the credentials encoded in base64
     * @return the decoded credentials
     */
    private UsernamePasswordAuthenticationToken mapBase64Credentials(String base64Credentials) {
        String principal = null;
        String credentials = null;

        try {
            String decodedCredentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
            int i = decodedCredentials.indexOf(':');
            if (i >= 0) {
                principal = decodedCredentials.substring(0, i);
                credentials = decodedCredentials.substring(i + 1);
            }
        } catch (Exception e) {
            log.debug("Conversion problem with the credentials {}", base64Credentials);
        }

        return new UsernamePasswordAuthenticationToken(principal, credentials);
    }
}
