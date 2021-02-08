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

import javax.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class RSU2012 extends FunctionalApar {
    private final String keystorePath;

    // TODO DRY authentication check across all apars, and their unit tests

    public RSU2012(List<String> usernames, List<String> passwords, String keystorePath) {
        super(usernames, passwords);
        this.keystorePath = keystorePath;
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        String authorization = headers.get("authorization");
        if (containsInvalidUser(authorization) && noLtpaCookie(headers)) {
            return Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        // TODO what if use ltpa token, then what is the username?
        String[] credentials = getPiecesOfCredentials(authorization);
        return Optional.of(validJwtResponse(response, credentials[0], keystorePath));
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        String authorization = headers.get("authorization");
        if (containsInvalidUser(authorization) && noLtpaCookie(headers) && noJwtCookie(headers)) {
            return Optional.of(new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
        }

        // TODO what if use ltpa token, then what is the username?
        String[] credentials = getPiecesOfCredentials(authorization);
        return Optional.of(validJwtResponse(response, credentials[0], keystorePath));
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationDelete() {
        // TODO implement auth check for all apars
        return Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }
}
