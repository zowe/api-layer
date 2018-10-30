/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.controllers.swagger;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.core.annotation.Order;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication API specification for Swagger documentation and Code Generation.
 * These endpoints are not meant to be called and will throw an exception
 * Order set to lowest precedence
 * Spring Security endpoints will override these endpoints
 */
@Order
@Api(tags = {"Authentication"}, description = "Security Operations")
@RestController
public class SpringSecurityAuthenticationApi {
    /**
     * Implemented by Spring Security
     */
    @ApiOperation(value = "Login", tags = {"Authentication"})
    @ApiResponses({@ApiResponse(code = 200, message = "", response = Authentication.class)})
    @PostMapping(value = "/auth/login")
    public String login() {
        throw new IllegalStateException("You have called a dummy implementation of the authentication endpoint." +
            "Add Spring Security to handle authentication");
    }

    /**
     * Implemented by Spring Security
     */
    @ApiOperation(value = "Logout", notes = "Logout the current user.", tags = {"Authentication"})
    @ApiResponses({@ApiResponse(code = 200, message = "")})
    @RequestMapping(value = "/auth/logout", method = RequestMethod.POST)
    public String logout() {
        throw new IllegalStateException("Add Spring Security to handle authentication");
    }
}
