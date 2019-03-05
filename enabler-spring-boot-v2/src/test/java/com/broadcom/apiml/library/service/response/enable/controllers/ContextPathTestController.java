/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.enable.controllers;


import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/")
public class ContextPathTestController {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(ContextPathTestController.class);
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

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof ContextPathTestController)) return false;
        final ContextPathTestController other = (ContextPathTestController) o;
        if (!other.canEqual((Object) this)) return false;
        final Object this$contextPath = this.getContextPath();
        final Object other$contextPath = other.getContextPath();
        if (this$contextPath == null ? other$contextPath != null : !this$contextPath.equals(other$contextPath))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof ContextPathTestController;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final Object $contextPath = this.getContextPath();
        result = result * PRIME + ($contextPath == null ? 43 : $contextPath.hashCode());
        return result;
    }

    public String toString() {
        return "ContextPathTestController(contextPath=" + this.getContextPath() + ")";
    }
}
