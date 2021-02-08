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

public class PH12143 extends FunctionalApar {
    private final String keystorePath;

    public PH12143(List<String> usernames, List<String> passwords, String keystorePath) {
        super(usernames, passwords);
        this.keystorePath = keystorePath;
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        if (isUnauthorized(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String[] credentials = getPiecesOfCredentials(headers);
        return validJwtResponse(response, credentials[0], keystorePath);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        if (isUnauthorized(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String[] credentials = getPiecesOfCredentials(headers);
        return validJwtResponse(response, credentials[0], keystorePath);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationDelete(Map<String, String> headers) {
        if (isUnauthorized(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    private boolean isUnauthorized(Map<String, String> headers) {
        return containsInvalidUser(headers) && noLtpaCookie(headers);
    }
}
