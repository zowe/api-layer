/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import com.netflix.appinfo.InstanceInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.DependsOn;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.discovery.staticdef.StaticRegistrationResult;
import org.zowe.apiml.discovery.staticdef.StaticServicesRegistrationService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.List;



@RestController
@RequiredArgsConstructor
@RequestMapping("/discovery/api/v1/staticApi")
@DependsOn("modulithConfig")
@Slf4j
public class StaticDefinitionsRefreshRestController {

    private final StaticServicesRegistrationService registrationService;

    @GetMapping(produces = "application/json")
    public Mono<ResponseEntity<List<InstanceInfo>>> list() {
        return Mono.just(
            ResponseEntity.ok()
                .body(registrationService.getStaticInstances()));
    }

    @PostMapping(produces = "application/json")
    public Mono<ResponseEntity<StaticRegistrationResult>> reload() {
        return Mono.just(ResponseEntity.ok())
            .publishOn(Schedulers.boundedElastic())
            .map(x ->  x.body(registrationService.reloadServices()));

    }

}
