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

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.client.services.AparBasedService;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@SuppressWarnings("squid:S1452")
public class FilesController {
    private final AparBasedService files;

    @GetMapping(value = "/zosmf/restfiles/ds", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> readFiles(
        HttpServletResponse response,
        @RequestHeader Map<String, String> headers
    ) {
        return files.process("files", "read", response, headers);
    }
}

