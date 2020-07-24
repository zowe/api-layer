/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.api;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.client.model.Registered;
import org.zowe.apiml.client.service.ApiMediationClientService;
import org.zowe.apiml.exception.ServiceDefinitionException;

@RestController
@RequestMapping("/api/v1/apiMediationClient")
@Api(
    value = "/api/v1/apiMediationClient",
    tags = {"API Mediation Client test call"}
)
public class ApiMediationClientTestController {
    private final ApiMediationClientService apiMediationClientService;

    public ApiMediationClientTestController(ApiMediationClientService apiMediationClientService) {
        this.apiMediationClientService = apiMediationClientService;
    }

    @PostMapping
    @ApiOperation(value = "Forward registration to discovery service via API mediation client")
    public ResponseEntity<String> forwardRegistration() {
        try {
            apiMediationClientService.register();
            return ResponseEntity.ok().build();
        } catch (ServiceDefinitionException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }

    @DeleteMapping
    @ApiOperation(value = "Forward un-registration to discovery service via API mediation client")
    public ResponseEntity<String> forwardUnRegistration() {
        apiMediationClientService.unregister();
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @ApiOperation(value = "Indicate if registration with discovery service via API mediation client was successful")
    public Registered isRegistered() {
        boolean isRegistered = apiMediationClientService.isRegistered();
        return new Registered(isRegistered);
    }
}
