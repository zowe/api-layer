/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.hwsjersey.resource;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Date;

@Api(value = "Greeting", tags = {"Greeting Controller"})
@Path("/api/v1")
public class GreetingController {

    private static final String TEMPLATE = "Hello, %s!";

    @GET
    @Path("/greeting/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Send a greeting",
        notes = "Send a greeting to the named person",
        response = Greeting.class)
    @ApiResponses(value = {@ApiResponse(code = 404, message = "URI not found")})
    public Response greeting(
        @ApiParam(value = "The person who will be greeted", required = true)
        @PathParam("name") String name) {
        return Response.ok(new Greeting(new Date(), String.format(TEMPLATE, name))).build();
    }

    @GET
    @Path("/greeting")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Send the default greeting",
        notes = "Send a default greeting to the caller",
        response = Greeting.class)
    @ApiResponses(value = {@ApiResponse(code = 404, message = "URI not found")})
    public Response defaultGreeting() {
        return Response.ok(new Greeting(new Date(), String.format(TEMPLATE, "World"))).build();
    }
}
