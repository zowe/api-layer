/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import org.zowe.apiml.gateway.security.service.AuthenticationService;
import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;

/**
 * Controller offer method to control security. It can contains method for user and also method for calling services
 * by gateway to distribute state of authentication between nodes.
 */
@AllArgsConstructor
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthenticationService authenticationService;

    @DeleteMapping(path = "/invalidate/**")
    public void invalidateJwtToken(HttpServletRequest request, HttpServletResponse response) {
        final String endpoint = "/auth/invalidate/";
        final String uri = request.getRequestURI();
        final int index = uri.indexOf(endpoint);

        final String jwtToken = uri.substring(index + endpoint.length());
        final boolean invalidated = authenticationService.invalidateJwtToken(jwtToken, false);

        response.setStatus(invalidated ? SC_OK : SC_SERVICE_UNAVAILABLE);
    }

}
