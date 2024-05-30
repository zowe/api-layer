/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.controllers;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.cloudgatewayservice.service.CertificateChainService;
import reactor.core.publisher.Mono;

/**
 * This simple controller provides a public endpoint with the client certificate chain.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(CertificatesRestController.CONTROLLER_PATH)
public class CertificatesRestController {
    public static final String CONTROLLER_PATH = "/gateway/certificates";

    private final CertificateChainService certificateChainService;

    @GetMapping
    public Mono<String> getCertificates() {
        return Mono.just(certificateChainService.getCertificatesInPEMFormat());
    }
}
