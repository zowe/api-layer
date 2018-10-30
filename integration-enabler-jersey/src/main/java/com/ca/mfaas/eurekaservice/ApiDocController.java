/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.eurekaservice;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/")
public class ApiDocController {

    private Client client = javax.ws.rs.client.ClientBuilder.newClient();
    private static final Logger log = LoggerFactory.getLogger(ApiDocController.class);

    @GET
    @Path("/api-doc")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getApiDoc() {
        if (Bootstrap.getApiDocEndpoint() == null) {
            throw new IllegalStateException("API Doc target endpoint is not set, API Doc cannot be accessed via /api-doc.");
        }
        try {
            String apiDoc = client.target(Bootstrap.getApiDocEndpoint())
                    .request()
                    .accept(MediaType.APPLICATION_JSON)
                    .get(String.class);
            return Response.ok(apiDoc).build();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return Response.serverError().entity(e.getMessage() + ": " + Bootstrap.getApiDocEndpoint()).build();
        }
    }
}
