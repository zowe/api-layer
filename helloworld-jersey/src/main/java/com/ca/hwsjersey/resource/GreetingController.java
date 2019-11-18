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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Date;

@Path("/api/v1")
@Tag(name = "Greeting", description = "Greeting Controller")
public class GreetingController {

    private static final String TEMPLATE = "Hello, %s!";

    @GET
    @Path("/greeting/{name}")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Send a greeting",
        tags = {"Greeting"},
        description = "Send a greeting to the named person",
        responses = {
            @ApiResponse(
                description = "successful operation",
                responseCode = "200",
                content = @Content(
                    schema = @Schema(implementation = Greeting.class)
                )
            ),
            @ApiResponse(responseCode = "404", description = "URI not found")
        })
    public Response greeting(
        @Parameter(
            description = "The person who will be greeted",
            required = true)
        @PathParam("name") String name) {
        return Response.ok(new Greeting(new Date(), String.format(TEMPLATE, name))).build();
    }

    @GET
    @Path("/greeting")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(
        summary = "Send the default greeting",
        tags = {"Greeting"},
        description = "Send a default greeting to the caller",
        responses = {
            @ApiResponse(
                description = "successful operation",
                responseCode = "200",
                content = @Content(
                    schema = @Schema(implementation = Greeting.class)
                )
            ),
            @ApiResponse(responseCode = "404", description = "URI not found")
        })
    public Response defaultGreeting() {
        return Response.ok(new Greeting(new Date(), String.format(TEMPLATE, "World"))).build();
    }
}
