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

import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Slf4j
public class StatusCodeController {


    @GetMapping(value = "/api/v1/status-code")
    @ApiOperation(value = "Parametrized status code",
        tags = {"Other Operations"})
    public ResponseEntity<String> returnStatusCodeForGET(@RequestParam(value = "code", defaultValue = "200") int statusCode) {
        log.info("Calling GET from gateway, status code: {}",statusCode);
        return ResponseEntity.status(statusCode).body("status code: " + statusCode);
    }

    @PostMapping(value = "/api/v1/status-code")
    @ApiOperation(value = "Parametrized status code",
        tags = {"Other Operations"})
    public ResponseEntity<String> returnStatusCodeForPOST(@RequestParam(value = "code", defaultValue = "200") int statusCode) {
        log.info("Calling POST from gateway, status code: {}",statusCode);
        return ResponseEntity.status(statusCode).body("status code: " + statusCode);
    }
}
