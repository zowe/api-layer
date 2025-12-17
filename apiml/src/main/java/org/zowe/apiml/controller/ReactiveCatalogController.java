/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


/**
 * This controller is intended for redirection of API Catalog APIs. It also serves as a fix for Cors.
 */
@RestController
@RequestMapping("/apicatalog")
@Slf4j
@RequiredArgsConstructor
public class ReactiveCatalogController {

    private static final String API_V1 = "/api/v1";
    private static final String UI_V1 = "/ui/v1";

    @GetMapping(API_V1)
    public Mono<ResponseEntity<Void>> catalogApi() {
        return Mono.just(ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/apicatalog/api/v1/").build());
    }

    @GetMapping(API_V1 + "/")
    public Mono<ResponseEntity<Void>> catalogApiIndex() {
        return Mono.just(ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/apicatalog/api/v1/index.html").build());
    }

    @GetMapping(UI_V1)
    public Mono<ResponseEntity<Void>> catalogUi() {
        return Mono.just(ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/apicatalog/ui/v1/").build());
    }

    @GetMapping(UI_V1 + "/")
    public Mono<ResponseEntity<Void>> catalogUiIndex() {
        return Mono.just(ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/apicatalog/ui/v1/index.html").build());
    }

    @PostMapping(API_V1 + "/auth/login")
    public Mono<ResponseEntity<Void>> catalogLogin() {
        return Mono.just(ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/gateway/api/v1/auth/login").build());
    }

    @PostMapping(API_V1 + "/auth/logout")
    public Mono<ResponseEntity<Void>> catalogLogout() {
        return Mono.just(ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/gateway/api/v1/auth/logout").build());
    }

    @GetMapping(API_V1 + "/auth/query")
    public Mono<ResponseEntity<Void>> catalogQuery() {
        return Mono.just(ResponseEntity.status(308).header(HttpHeaders.LOCATION, "/gateway/api/v1/auth/query").build());
    }
}
