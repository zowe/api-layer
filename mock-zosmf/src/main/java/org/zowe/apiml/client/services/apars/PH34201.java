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
// TODO
// all other apars return 500 if auth
// this one return 401 if no auth - make a noAuth function in FunctionalApar
// run integration tests to verify
// adjust authenticated endpoint strategy and/or zosmfservice to handle this
// probably zosmfservice keep trying if get 401, then return when successful auth, or after all tried if one is 401 then 401, else idk
public class PH34201 extends FunctionalApar {
    private final String keystorePath;

    public PH34201(List<String> usernames, List<String> passwords, String keystorePath) {
        super(usernames, passwords);
        this.keystorePath = keystorePath;
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        if (containsInvalidUser(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }

        String[] credentials = getPiecesOfCredentials(headers);
        return validJwtResponse(response, credentials[0], keystorePath);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        return handleAuthenticationCreate(headers, response);
    }

    @Override
    protected ResponseEntity<?> handleAuthenticationDelete(Map<String, String> headers) {
        if (containsInvalidUser(headers)) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }
}
