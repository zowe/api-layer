/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.hellospring.controller;

import org.zowe.apiml.hellospring.model.Greeting;
import io.swagger.annotations.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Date;

@RestController
@RequestMapping(value = "/api/v1", produces = {MediaType.APPLICATION_JSON_VALUE})
@Api(value = "Greeting", tags = {"Greeting Controller"})
public class GreetingController {

    private static final String TEMPLATE = "Hello, %s!";

    @GetMapping(value = "/greeting")
    @ApiOperation(value = "Send the default greeting",
        notes = "Send a default greeting to the caller",
        response = Greeting.class)
    @ApiResponses(value = {@ApiResponse(code = 404, message = "URI not found")})
    public Greeting greeting() {
        return new Greeting(new Date(), String.format(TEMPLATE, "World"));
    }

    @GetMapping(value = "/greeting/{name}")
    @ApiOperation(value = "Send a greeting",
        notes = "Send a greeting to the named person",
        response = Greeting.class)
    @ApiResponses(value = {@ApiResponse(code = 404, message = "URI not found")})
    public Greeting greeting(@ApiParam(value = "The person who will be greeted", required = true)
                             @PathVariable(value = "name") String name) {
        return new Greeting(new Date(), String.format(TEMPLATE, name));
    }
}
