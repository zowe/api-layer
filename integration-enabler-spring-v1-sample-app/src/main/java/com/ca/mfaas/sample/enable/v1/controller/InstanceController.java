/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.sample.enable.v1.controller;

import com.ca.mfaas.enable.services.MfaasServiceLocator;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A Controller for returning instance runtime info
 */
@RestController
@RequestMapping("/api/v1/instance")
@Api(tags = {"Instance"})
public class InstanceController {

    @Value("${mfaas.server.port}")
    private String port;

    private MfaasServiceLocator mfaasServiceLocator;

    /**
     * Test controller for checking instance services
     * @param mfaasServiceLocator Enabler service for locating services based on serviceId
     */
    @Autowired
    public InstanceController(MfaasServiceLocator mfaasServiceLocator) {
        this.mfaasServiceLocator = mfaasServiceLocator;
    }

    /**
     * What is the configured port
     *
     * @return app.yaml port
     */
    @GetMapping(value = "/configured-port", produces = "text/plain")
    @ApiOperation(value = "What port is this controller configured for",
        notes = "What port is this controller configured for",
        response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "URI not found"),
    })
    public String getPort() {
        return port;
    }

    /**
     * What is the gateway URL
     *
     * @return gateway url
     */
    @GetMapping(value = "/gateway-url", produces = "text/plain")
    @ApiOperation(value = "What is the URI of the Gateway",
        notes = "What is the URI of the Gateway",
        response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "URI not found"),
        @ApiResponse(code = 500, message = "Internal Error Occurred"),
    })
    public String getGatewayLocation() throws Exception {
        return mfaasServiceLocator.locateGatewayUrl().toString();
    }

}
