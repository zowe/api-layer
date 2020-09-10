/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import lombok.AllArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.gateway.metadata.service.EurekaApplications;

/**
 * REST API providing basic information about services registered in Eureka.
 */
@RestController
@RequestMapping(value = "/service-info")
@AllArgsConstructor
public class ServiceInfoController {

    private EurekaApplications applications;

    @GetMapping(value = "/{serviceId}/onboarded")
    public boolean isServiceRegistered(@PathVariable(value = "serviceId") String serviceId) {
        return applications.getRegistered().stream().anyMatch(application -> serviceId.equalsIgnoreCase(application.getName()));
    }
}
