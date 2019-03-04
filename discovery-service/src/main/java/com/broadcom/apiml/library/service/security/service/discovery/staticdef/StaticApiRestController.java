/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.security.service.discovery.staticdef;

import com.netflix.appinfo.InstanceInfo;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/discovery/api/v1/staticApi")
public class StaticApiRestController {
    private final StaticServicesRegistrationService registrationService;

    @Autowired
    public StaticApiRestController(StaticServicesRegistrationService registrationService) {
        this.registrationService = registrationService;
    }

    @GetMapping
    @ApiOperation(value = "List static API definitions from the filesystem")
    public List<InstanceInfo> list() {
        return registrationService.getStaticInstances();
    }

    @PostMapping
    @ApiOperation(value = "Reload static API definitions from the filesystem")
    public List<InstanceInfo> reload() {
        registrationService.reloadServices();
        return registrationService.getStaticInstances();
    }
}
