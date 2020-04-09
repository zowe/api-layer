/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.product.version.VersionInfo;
import org.zowe.apiml.product.version.VersionService;

/**
 * API for providing information about Zowe and API ML versions
 */

@AllArgsConstructor
@RestController
@RequestMapping("/gateway")
public class VersionController {

    private VersionService versionService;

    @GetMapping(value = "/version", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<VersionInfo> getVersion() {
        return new ResponseEntity<>(versionService.getVersion(), HttpStatus.OK);
    }
}
