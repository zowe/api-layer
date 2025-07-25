/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.apicatalog.staticapi.StaticAPIService;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

@RequiredArgsConstructor
public class StaticAPIRefreshController {

    private final StaticAPIService staticAPIService;

    @PostMapping(value = "/refresh", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<String>> refreshStaticApis() {
        return Mono.fromCallable(staticAPIService::refresh)
            .map(staticAPIResponse -> ResponseEntity
                .status(staticAPIResponse.getStatusCode())
                .body(staticAPIResponse.getBody())
            )
            .subscribeOn(Schedulers.boundedElastic());
    }

}

@RestController
@RequestMapping("/apicatalog/api/v1/static-api")
@ConditionalOnBean(name = "modulithConfig")
class StaticAPIRefreshControllerModulith extends StaticAPIRefreshController {

    public StaticAPIRefreshControllerModulith(StaticAPIService staticAPIService) {
        super(staticAPIService);
    }

}

@RestController
@RequestMapping("/apicatalog/static-api")
@ConditionalOnMissingBean(name = "modulithConfig")
class StaticAPIRefreshControllerMicroservice extends StaticAPIRefreshController {

    public StaticAPIRefreshControllerMicroservice(StaticAPIService staticAPIService) {
        super(staticAPIService);
    }

}
