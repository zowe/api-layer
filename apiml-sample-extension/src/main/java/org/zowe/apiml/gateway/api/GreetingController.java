/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.api;

import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that returns greetings.
 */
@RestController
@Tag(name = "Other Operations")
@RequestMapping("/api/v1")
public class GreetingController {
    private static final String GREETING = "Hello, I'm a sample extension!";

    /**
     * Gets a greeting for anyone.
     */
    @GetMapping(value = "/greeting")
    public String greeting() {
        return GREETING;
    }
}
