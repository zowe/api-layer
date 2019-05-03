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

import com.ca.mfaas.constants.ApimlConstants;
import org.springframework.http.HttpHeaders;
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
public class BasicContentFilter extends AbstractSecureContentFilter {

    /**
     * Constructor
     */
    public BasicContentFilter(AuthenticationManager authenticationManager, AuthenticationFailureHandler failureHandler) {
        super(authenticationManager, failureHandler);
    }

    /**
     * Extract credentials from the authorization header in the request and decode them
     *
     * @param request the http request
     * @return the decoded credentials
     */
    protected Optional<UsernamePasswordAuthenticationToken> extractContent(HttpServletRequest request) {
        return Optional.ofNullable(
            request.getHeader(HttpHeaders.AUTHORIZATION)
        ).filter(
            header -> header.startsWith(ApimlConstants.BASIC_AUTHENTICATION_PREFIX)
        ).map(
            header -> header.replaceFirst(ApimlConstants.BASIC_AUTHENTICATION_PREFIX, "")
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
        String credentials = new String(Base64.getDecoder().decode(base64Credentials), StandardCharsets.UTF_8);
        int i = credentials.indexOf(':');
        if (i >= 0) {
            return new UsernamePasswordAuthenticationToken(credentials.substring(0, i), credentials.substring(i + 1));
        }

        return null;
    }
}
