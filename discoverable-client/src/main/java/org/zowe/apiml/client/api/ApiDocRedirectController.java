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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Tag(name = "Other Operations")
public class ApiDocRedirectController {

    private static final String ORIGINAL_APIV2_DOC_PATH = "/v3/api-docs/redirect";

    @GetMapping("/v3/api-docs/redirect")
    public ResponseEntity<String> getDocWithRedirect() {
        return ResponseEntity.status(301)
            .header("Location", ORIGINAL_APIV2_DOC_PATH)
            .build();
    }

}
