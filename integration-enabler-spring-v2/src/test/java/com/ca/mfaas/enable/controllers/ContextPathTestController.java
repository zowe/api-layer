/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.enable.controllers;


import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Data
@RestController
@RequestMapping("/")
public class ContextPathTestController {

    private final String contextPath;

    @Autowired
    public ContextPathTestController(
        @Value("${server.contextPath:}") final String contextPath) {
        this.contextPath = contextPath;
    }

    @GetMapping(value = "/context")
    public String getContextPath() {
        return this.contextPath;
    }
}
