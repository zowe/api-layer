/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.client.controller.controllers.api;

import com.ca.mfaas.enable.services.MfaasServiceLocator;
import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * A Controller for returning instance runtime info
 */
@RestController
@RequestMapping("/api/v1/instance")
@Api(tags = {"Other Operations"}, description = "General Endpoints")
public class InstanceController {

    private final MFaaSConfigPropertiesContainer propertiesContainer;
    private MfaasServiceLocator mfaasServiceLocator;

    /**
     * Test controller for checking instance services
     * @param propertiesContainer MFaaS properties
     * @param mfaasServiceLocator Enabler service for locating services based on serviceId
     */
    @Autowired
    public InstanceController(MFaaSConfigPropertiesContainer propertiesContainer,
                              MfaasServiceLocator mfaasServiceLocator) {
        this.propertiesContainer = propertiesContainer;
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
        tags = {"Other Operations"},
        response = String.class)
    @ApiResponses(value = {
        @ApiResponse(code = 200, message = "OK"),
        @ApiResponse(code = 404, message = "URI not found"),
    })
    public String getPort() {
        return propertiesContainer.getServer().getPort();
    }
}
