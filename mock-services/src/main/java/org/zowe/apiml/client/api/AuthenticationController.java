/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.client.api;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.client.model.LoginBody;
import org.zowe.apiml.client.services.AparBasedService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@SuppressWarnings("squid:S1452")
public class AuthenticationController {
    private static final String AUTHENTICATION_SERVICE = "authentication";

    private final AparBasedService authentication;

    @DeleteMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> logout(HttpServletResponse response,
                                    @RequestHeader Map<String, String> headers) {
        return authentication.process(AUTHENTICATION_SERVICE, "delete", response, headers);
    }

    @PostMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> authenticate(
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) {
        return authentication.process(AUTHENTICATION_SERVICE, "create", response, headers);
    }

    @PutMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> changePassword(
        @RequestBody LoginBody loginBody,
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) {
        return authentication.process(AUTHENTICATION_SERVICE, "update", response, headers, loginBody);
    }

    @GetMapping(value = "/zosmf/notifications/inbox", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> verify(HttpServletResponse response, @RequestHeader Map<String, String> headers) {
        return authentication.process(AUTHENTICATION_SERVICE, "verify", response, headers);
    }
}
