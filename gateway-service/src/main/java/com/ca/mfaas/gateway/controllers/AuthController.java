/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.controllers;

import com.ca.mfaas.gateway.security.service.AuthenticationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * Controller offer method to control security. It can contains method for user and also method for calling services
 * by gateway to distribute state of authentication between nodes.
 */
@AllArgsConstructor
@RestController("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    @DeleteMapping("/invalidate/**")
    public Boolean invalidateJwtToken(HttpServletRequest request) {
        final String jwtToken = request.getRequestURI().split(request.getContextPath() + "/invalidate/")[1];
        return authenticationService.invalidateJwtToken(jwtToken, false);
    }

}
