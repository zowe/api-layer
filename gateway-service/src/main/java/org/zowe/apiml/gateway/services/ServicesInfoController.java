/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.services;

import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping(ServicesInfoController.SERVICES_URL)
public class ServicesInfoController {

    public static final String SERVICES_URL = "/gateway/services";

    private final ServicesInfoService servicesInfoService;

    @GetMapping(value = "/{serviceId}", produces = MediaType.APPLICATION_JSON_UTF8_VALUE)
    public ResponseEntity<ServiceInfo> getService(@PathVariable String serviceId) {
        ServiceInfo serviceInfo = servicesInfoService.getServiceInfo(serviceId);
        HttpStatus status = (serviceInfo.getStatus() == InstanceInfo.InstanceStatus.UNKNOWN) ?
                HttpStatus.NOT_FOUND : HttpStatus.OK;

        return new ResponseEntity<>(serviceInfo, status);
    }

}
