/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.apidoc.reader;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-doc")
@RequiredArgsConstructor
public class ApiDocController {

    private static final String API_DOC_LOCATION = "gateway-api-doc.json";

    private final ApiDocReader apiDocReader;

    @GetMapping(produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public String getApiDoc() {
        return apiDocReader.load(API_DOC_LOCATION);
    }
}
