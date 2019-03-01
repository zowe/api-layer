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
        @Value("${server.contextPath}") final String contextPath) {
        this.contextPath = contextPath;
    }

    @GetMapping(value = "/context")
    public String getContextPath() {
        return this.contextPath;
    }

    public boolean equals(final Object o) {
        if (o == this) return true;
        if (!(o instanceof com.ca.mfaas.enable.controllers.ContextPathTestController)) return false;
        final com.ca.mfaas.enable.controllers.ContextPathTestController other = (com.ca.mfaas.enable.controllers.ContextPathTestController) o;
        if (!other.canEqual((java.lang.Object) this)) return false;
        final java.lang.Object this$contextPath = this.getContextPath();
        final java.lang.Object other$contextPath = other.getContextPath();
        if (this$contextPath == null ? other$contextPath != null : !this$contextPath.equals(other$contextPath))
            return false;
        return true;
    }

    protected boolean canEqual(final Object other) {
        return other instanceof com.ca.mfaas.enable.controllers.ContextPathTestController;
    }

    public int hashCode() {
        final int PRIME = 59;
        int result = 1;
        final java.lang.Object $contextPath = this.getContextPath();
        result = result * PRIME + ($contextPath == null ? 43 : $contextPath.hashCode());
        return result;
    }

    public String toString() {
        return "ContextPathTestController(contextPath=" + this.getContextPath() + ")";
    }
}
