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

import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.apicatalog.oidc.OidcUtils;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Endpoints related to the OIDC integration
 */
@Slf4j
@Tag(name = "OIDC integration")
public class OidcController {

    private AtomicReference<List<String>> oidcProviderCache = new AtomicReference<>();

    @GetMapping(value = "/provider", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<ResponseEntity<List<String>>> getOidcProvider() {
        if (oidcProviderCache.get() == null) {
            oidcProviderCache.set(OidcUtils.getOidcProvider());
        }

        return Mono.just(new ResponseEntity<>(oidcProviderCache.get(), oidcProviderCache.get().isEmpty() ? HttpStatus.NO_CONTENT : HttpStatus.OK));
    }

}

@RestController
@RequestMapping({"/apicatalog/oidc", "/apicatalog/api/v1/oidc"})
@ConditionalOnBean(name = "modulithConfig")
class OidcControllerModulith extends  OidcController {

}

@RestController
@RequestMapping({"/apicatalog/oidc", "/apicatalog/api/v1/oidc"})
@ConditionalOnMissingBean(name = "modulithConfig")
class OidcControllerMicroservice extends  OidcController {

}
