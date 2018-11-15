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

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

import java.util.Collections;

/**
 * Helper controller for testing encoded / in a url
 * Not to be displayed in Swagger
 */
@Slf4j
@RestController
@ApiIgnore
public class PathController {

    @GetMapping(value = "/api/v1/files/{path}/content")
    public ResponseEntity<?> getContentForPath(@PathVariable(value = "path") String path) {
        return ResponseEntity.ok().body(Collections.singletonMap("path", path));
    }
}
