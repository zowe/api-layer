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

import com.ca.mfaas.eurekaservice.model.Health;
import com.ca.mfaas.eurekaservice.model.InstanceInfo;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.HashMap;


@Path("/api/v1/application")
public class DiscoveryController {

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Send the application info",
        notes = "Send a application info to the caller",
        response = InstanceInfo.class)
    public Object getDiscoveryInfo() {
        return new HashMap<String, String>();
    }

    @GET
    @Path("/health")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Send the health status",
        notes = "Send a status greeting to the caller",
        response = Health.class)
    public Object getHealth() {
        return new Health("UP");
    }
}
