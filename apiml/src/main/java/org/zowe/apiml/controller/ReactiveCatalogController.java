/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/apicatalog/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactiveCatalogController {

    @PostMapping("/login")
    public ResponseEntity<Void> catalogLogin() {
        return ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/gateway/api/v1/auth/login").build();
    }

}
