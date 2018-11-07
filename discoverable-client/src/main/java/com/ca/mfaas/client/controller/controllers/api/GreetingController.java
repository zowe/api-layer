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

import com.ca.mfaas.client.controller.domain.Greeting;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Optional;

/**
 * Version 1 of the controller that returns greetings.
 */
@Slf4j
@RestController
@Api(tags = {"Other Operations"}, description = "General Operations")
public class GreetingController {
    private static final String TEMPLATE = "Hello, %s!";

    /**
     * Gets a greeting.
     */
    @GetMapping(value = {"/greeting/{yourName}", "/greeting"})
    @ApiOperation(value = "Get a greeting", response = Greeting.class,
        tags = {"Other Operations"})
    public Greeting yourGreeting(@RequestParam(value = "delayMs", defaultValue = "0", required = false) Integer delayMs,
                                 @PathVariable(value = "yourName") Optional<String> yourName) {

        String name;
        if (yourName.isPresent()) {
            name = yourName.get();
        } else {
            name = "world";
        }
        if (delayMs > 0) {
            try {
                Thread.sleep(delayMs);
            } catch (InterruptedException e) {
                log.warn("Delay interrupted", e);
                Thread.currentThread().interrupt();
            }
        }
        return new Greeting(new Date(), String.format(TEMPLATE, name));
    }
}
