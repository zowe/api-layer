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

import com.ca.mfaas.eurekaservice.model.*;
import io.swagger.annotations.ApiOperation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import java.util.ResourceBundle;


@Path("/application")
public class DiscoveryController {
    private static ResourceBundle eurekaProperties = ResourceBundle.getBundle("eureka-client");

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(value = "Send the application info",
        notes = "Send a application info to the caller",
        response = InstanceInfo.class)
    public Object getDiscoveryInfo() {


        return new InstanceInfo(

            new App(
            eurekaProperties.getString("eureka.metadata.mfaas.api-info.apiVersionProperties.v1.title"),
            eurekaProperties.getString("eureka.metadata.mfaas.api-info.apiVersionProperties.v1.description"),
            eurekaProperties.getString("eureka.metadata.mfaas.api-info.apiVersionProperties.v1.version")
            ),

            new MFaasInfo(
                new DiscoveryInfo(
                    eurekaProperties.getString("eureka.service.hostname"),
                    Boolean.valueOf(eurekaProperties.getString("eureka.securePortEnabled")),
                    eurekaProperties.getString("eureka.name"),
                    Integer.valueOf(eurekaProperties.getString("eureka.port")),
                    "CLIENT",
                    eurekaProperties.getString("eureka.name"),
                    Boolean.TRUE,
                    eurekaProperties.getString("eureka.metadata.mfaas.discovery.catalogUiTile.description")
                )
            )
        );
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
