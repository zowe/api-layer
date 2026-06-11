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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;
import reactor.core.publisher.Mono;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.Certificate;

@RequiredArgsConstructor
@Tag(name = "Certificates")
@RestController
@Slf4j
@RequestMapping({ CertificatesController.CONTROLLER_PATH, CertificatesController.CONTROLLER_FULL_PATH })
public class CertificatesController {

    public static final String CONTROLLER_PATH = "/gateway/certificates";
    public static final String CONTROLLER_FULL_PATH = "/gateway/api/v1/certificates";
    private static final ApimlLogger apimlLog = ApimlLogger.of(CertificatesController.class, YamlMessageServiceInstance.getInstance());
    private Certificate[] certificates;

    @GetMapping
    @Operation(summary = "Returns the certificate chain that is used by Gateway",
        operationId = "getCertificates",
        description = "Use the `/certificates` API to obtain public certificate chain used by Gateway for signed communication. " +
            "With this endpoint you can verify who send the certificate. It is used for forwarding the client certificates between Gateways.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successful responding of certificates", content = @Content(
            mediaType = MediaType.TEXT_PLAIN_VALUE,
            schema = @Schema(implementation = String.class)
        ))
    })
    public String getCertificates() {
        return getCertificatesInPEMFormat();
    }

    private final HttpsConfig config;

    public String getCertificatesInPEMFormat() {
        StringWriter stringWriter = new StringWriter();
        if (certificates != null && certificates.length > 0) {
            try (JcaPEMWriter jcaPEMWriter = new JcaPEMWriter(stringWriter)) {
                for (Certificate cert : certificates) {
                    jcaPEMWriter.writeObject(cert);
                }
            } catch (IOException e) {
                log.error("Failed to convert a certificate to PEM format. {}", e.getMessage());
                return null;
            }
        }

        return stringWriter.toString();
    }

    @PostConstruct
    void loadCertChain() {
        try {
            certificates = SecurityUtils.loadCertificateChain(config);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(),
                e, HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
        }
    }
}
