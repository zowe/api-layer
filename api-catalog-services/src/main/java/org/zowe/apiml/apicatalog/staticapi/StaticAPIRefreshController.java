/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/static")
@RequiredArgsConstructor
public class StaticAPIRefreshController {

    private final StaticAPIService staticAPIService;

    @PostMapping(value = "/api/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity refreshStaticApis() {
        StaticAPIResponse staticAPIResponse = staticAPIService.refresh();
        return ResponseEntity
            .status(staticAPIResponse.getStatusCode())
            .body(staticAPIResponse.getBody());
    }

}
