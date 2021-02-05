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

import org.springframework.http.ResponseEntity;

import javax.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class FunctionalApar implements Apar {
    private final List<String> usernames;
    private final List<String> passwords;

    protected FunctionalApar(List<String> usernames, List<String> passwords) {
        this.usernames = usernames;
        this.passwords = passwords;
    }

    @Override
    public Optional<ResponseEntity<?>> apply(Object... parameters) {
        String calledService = (String) parameters[0];
        String calledMethod = (String) parameters[1];
        Optional<ResponseEntity<?>> originalResult = (Optional<ResponseEntity<?>>) parameters[2];
        HttpServletResponse response = (HttpServletResponse) parameters[3];
        Map<String, String> headers = (Map<String, String>) parameters[4];
        Optional<ResponseEntity<?>> result = Optional.empty();

        if (calledService.equals("authentication")) {
            switch (calledMethod) {
                case "create":
                    result = handleAuthenticationCreate(headers, response);
                    break;
                case "verify":
                    result = handleAuthenticationVerify(headers, response);
                    break;
                case "delete":
                    result = handleAuthenticationDelete();
                    break;
            }
            if (!result.isPresent()) {
                result = handleAuthenticationDefault(headers);
            }
        }

        if (calledService.equals("information")) {
            result = handleInformation(headers, response);
        }

        if (calledService.equals("files")) {
            result = handleFiles(headers);
        }

        return result.isPresent() ? result : originalResult;
    }

    protected Optional<ResponseEntity<?>> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        return Optional.empty();
    }

    protected Optional<ResponseEntity<?>> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        return Optional.empty();
    }

    protected Optional<ResponseEntity<?>> handleAuthenticationDelete() {
        return Optional.empty();
    }

    protected Optional<ResponseEntity<?>> handleAuthenticationDefault(Map<String, String> headers) {
        return Optional.empty();
    }

    protected Optional<ResponseEntity<?>> handleInformation(Map<String, String> headers, HttpServletResponse response) {
        return Optional.empty();
    }

    protected Optional<ResponseEntity<?>> handleFiles(Map<String, String> headers) {
        return Optional.empty();
    }

    protected boolean containsInvalidUser(String authorization) {
        if (authorization == null || authorization.isEmpty()) {
            return true;
        }

        String[] piecesOfCredentials = getPiecesOfCredentials(authorization);
        return piecesOfCredentials.length <= 0 || (!usernames.contains(piecesOfCredentials[0]) ||
            (!passwords.contains(piecesOfCredentials[1]) && !piecesOfCredentials[1].contains("PASS_TICKET")));
    }

    protected String[] getPiecesOfCredentials(String authorization) {
        byte[] decoded = Base64.getDecoder().decode(authorization.replace("Basic ", ""));
        String credentials = new String(decoded);
        return credentials.split(":");
    }
}
