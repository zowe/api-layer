/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.services.apars;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.client.services.JwtTokenService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;

public class AuthenticateApar extends FunctionalApar {
    public AuthenticateApar(List<String> usernames, List<String> passwords, Integer timeout) {
        super(usernames, passwords, new JwtTokenService(timeout));
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        if (noAuthentication(headers)) {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (isUnauthorized(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        setLtpaToken(response);
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        return handleAuthenticationCreate(headers, response);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationDefault(Map<String, String> headers) {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    private boolean isUnauthorized(Map<String, String> headers) {
        return containsInvalidOrNoUser(headers) && !ltpaIsPresent(headers);
    }
}
