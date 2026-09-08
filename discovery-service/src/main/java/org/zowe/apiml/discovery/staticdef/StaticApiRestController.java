/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.discovery.staticdef;

import org.zowe.apiml.registry.model.ServiceInstance;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.product.discovery.StaticRegistrationResult;

import java.util.List;

@RestController
@RequestMapping("/discovery/api/v1/staticApi")
@RequiredArgsConstructor
@ConditionalOnMissingBean(name = "modulithConfig")
public class StaticApiRestController {
    private final StaticServicesRegistrationService registrationService;

    @GetMapping(produces = "application/json")
    public List<ServiceInstance> list() {
        return registrationService.getStaticInstances();
    }

    @PostMapping(produces = "application/json")
    public StaticRegistrationResult reload() {
        return registrationService.reloadServices();
    }
}
