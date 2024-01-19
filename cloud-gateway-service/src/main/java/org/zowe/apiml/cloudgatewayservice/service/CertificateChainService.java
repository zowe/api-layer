/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.springframework.stereotype.Service;
import org.zowe.apiml.cloudgatewayservice.config.ConnectionsConfig;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.StringWriter;
import java.security.cert.Certificate;

/**
 * This service provides gateway's certificate chain which is used for the southbound communication
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CertificateChainService {
    private static final ApimlLogger apimlLog = ApimlLogger.of(CertificateChainService.class, YamlMessageServiceInstance.getInstance());
    Certificate[] certificates;

    private final ConnectionsConfig connectionsConfig;

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
        HttpsConfig config = connectionsConfig.factory().getConfig();
        try {
            certificates = SecurityUtils.loadCertificateChain(config);
        } catch (Exception e) {
            apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
            throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(),
                e, HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
        }
    }
}
