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

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Controller to test the support of Catalog to render redirected Swagger
 */
@RestController
@Tag(name = "Other Operations")
public class ApiDocRedirectController {

    @Value("${apiml.service.baseUrl}")
    private String baseUrl;

    @Value("${apiml.service.contextPath}")
    private String contextPath;

    private static final String REDIRECT_DOC_URL = "/docs/redirect";

    @GetMapping(REDIRECT_DOC_URL)
    public ResponseEntity<Void> getDocWithRedirect() {
        String location = UriComponentsBuilder.fromUriString(baseUrl)
            .path(contextPath)
            .path("/v3/api-docs/apiv2").toUriString();

        return ResponseEntity.status(301)
            .header("Location", location)
            .build();
    }

}
