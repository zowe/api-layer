/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.api;

import com.ca.mfaas.client.model.RedirectLocation;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

import static org.springframework.http.HttpHeaders.LOCATION;

/**
 * This Rest Controller is used for the integration test of PageRedirectionFilter.java
 */
@Slf4j
@RestController
@Api(tags = {"Other Operations"}, description = "General Operations")
public class PageRedirectionController {

    @PostMapping(
        value = "/api/v1/redirect",
        produces = MediaType.APPLICATION_JSON_UTF8_VALUE,
        consumes = MediaType.APPLICATION_JSON_VALUE
    )
    @ResponseStatus(HttpStatus.TEMPORARY_REDIRECT)
    @ApiOperation(
        value = "/redirect",
        notes = "Redirect to location")
    @ApiResponses(value = {
        @ApiResponse(code = 307, message = "Redirect to specified location", response = String.class)
    })
    public RedirectLocation redirectPage(@ApiParam(value = "Location that need to be redirected to", required = true)
                                         @RequestBody RedirectLocation redirectLocation,
                                         HttpServletResponse response) {
        response.setHeader(LOCATION, redirectLocation.getLocation());
        return redirectLocation;
    }
}
