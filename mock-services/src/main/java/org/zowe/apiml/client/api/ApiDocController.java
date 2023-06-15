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
import org.springframework.web.bind.annotation.RestController;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

/**
 * z/OSMF returns Swagger v2
 */
@RestController
@RequiredArgsConstructor
public class ApiDocController {

    @GetMapping(value = "/zosmf/api/docs", produces = "application/json; charset=utf-8")
    public ResponseEntity<?> getApiDoc() {
        URL apidoc = getClass().getClassLoader().getResource("apidoc.json");
        try (InputStream stream = apidoc.openStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            StringBuilder builder = new StringBuilder();
            reader.lines().forEach(builder::append);
            return ResponseEntity.ok(builder.toString());
        } catch (IOException e) {
            return ResponseEntity.status(500).body(e.getMessage());
        }
    }
}
