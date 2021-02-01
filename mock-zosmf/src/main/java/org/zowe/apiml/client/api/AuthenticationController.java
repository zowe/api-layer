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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.client.services.AparBasedService;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthenticationController {
    private final AparBasedService authentication;

    @RequestMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8", method = RequestMethod.DELETE)
    public ResponseEntity<?> logout(HttpServletResponse response,
                                    @RequestHeader Map<String, String> headers) {
        return authentication.process("authentication","delete",response, headers);
    }

    @RequestMapping(value = "/zosmf/services/authenticate", produces = "application/json; charset=utf-8", method = RequestMethod.POST)
    public ResponseEntity<?> authenticate(
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) {
        return authentication.process("authentication","create",response, headers);
    }

    @RequestMapping(value = "/jwt/ibm/api/zOSMFBuilder/**", produces = "application/json; charset=utf-8", method = RequestMethod.GET)
    public ResponseEntity<?> jwk() {
        return new ResponseEntity<>(HttpStatus.OK);
    }
}
