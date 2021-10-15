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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.client.services.AparBasedService;

import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@SuppressWarnings("squid:S1452")
@ConditionalOnProperty(name = "zosmf.enableMock", havingValue = "true", matchIfMissing = true)
public class JwtController {
    private final AparBasedService jwtHandler;

    @GetMapping(value = "/jwt/ibm/api/zOSMFBuilder/**", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> jwk(HttpServletResponse response,
                                 @RequestHeader Map<String, String> headers) {
        return jwtHandler.process("jwtKeys", "get", response, headers);
    }
}
