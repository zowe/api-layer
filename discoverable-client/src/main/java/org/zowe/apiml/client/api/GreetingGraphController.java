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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.client.model.Greeting;

import java.util.Date;

/**
 * Version WS of the controller that returns greetings.
 */
@RestController
@Tag(name = "Other Operations", description = "General Operations")
@RequestMapping("/graphql/v1")
public class GreetingGraphController {
    private static final String TEMPLATE = "Hi, %s!";

    /**
     * Gets a greeting for anyone.
     */
    @GetMapping(value = "/greeting")
    @Operation(summary = "Get a greeting", tags = {"Other Operations"})
    public Greeting greeting(@RequestParam(value = "name", defaultValue = "user") String name,
                             @RequestParam(value = "delayMs", defaultValue = "0", required = false) Integer delayMs) {
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return new Greeting(new Date(), String.format(TEMPLATE, name));
    }
}
