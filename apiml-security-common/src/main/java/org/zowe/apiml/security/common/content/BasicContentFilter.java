/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.content;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.security.common.error.ResourceAccessExceptionHandler;

import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Optional;

/**
 * Authenticate the credentials from the basic authorization header
 */
@Slf4j
public class BasicContentFilter extends AbstractSecureContentFilter {

    public BasicContentFilter(AuthenticationManager authenticationManager,
                              AuthenticationFailureHandler failureHandler,
                              ResourceAccessExceptionHandler resourceAccessExceptionHandler) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, new String[0]);
    }

    public BasicContentFilter(AuthenticationManager authenticationManager,
                              AuthenticationFailureHandler failureHandler,
                              ResourceAccessExceptionHandler resourceAccessExceptionHandler,
                              String[] endpoints) {
        super(authenticationManager, failureHandler, resourceAccessExceptionHandler, endpoints);
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
        byte[] credentials = null;
        try {
            credentials = Base64.getDecoder().decode(base64Credentials);
            int index = ArrayUtils.indexOf(credentials, (byte) ':');
            if (index >= 0) {
                byte[] password = null;
                char[] passwordChars;
                try {
                    password = Arrays.copyOfRange(credentials, index + 1, credentials.length);
                    passwordChars = new char[password.length];
                    for (int i = 0; i < password.length; i++) {
                        passwordChars[i] = (char) password[i];
                    }
                    return new UsernamePasswordAuthenticationToken(
                            new String(Arrays.copyOfRange(credentials, 0, index), StandardCharsets.UTF_8),
                            passwordChars
                    );
                } finally {
                    if (password != null) {
                        Arrays.fill(password, (byte) 0);
                    }
                }
            }
        } catch (Exception e) {
            log.debug("Conversion problem with the credentials {}", base64Credentials);
        } finally {
            if (credentials != null) {
                Arrays.fill(credentials, (byte) 0);
            }
        }

        return new UsernamePasswordAuthenticationToken(null, null);
    }
}
