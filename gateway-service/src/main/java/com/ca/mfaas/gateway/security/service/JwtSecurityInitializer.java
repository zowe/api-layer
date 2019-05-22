/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.gateway.security.service;

import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsConfigError;
import com.ca.mfaas.utils.SecurityUtils;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.Key;
import java.util.Base64;


@Slf4j
@Service
public class JwtSecurityInitializer {

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private String keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private String keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.jwt.keyAlias:jwtsecret}")
    private String keyAlias;

    @Value("${apiml.security.jwt.secretKeyFilePath:#{null}}")
    private String filePath;

    private String signatureAlgorithm;
    private Key jwtSecret;

    @PostConstruct
    public void init() {
        signatureAlgorithm = SignatureAlgorithm.HS256.getValue();
        HttpsConfig config = HttpsConfig.builder().keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
            .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).trustStore(null).build();
        try {
            jwtSecret = SecurityUtils.loadKey(config);
        } catch (HttpsConfigError er) {
            log.warn("Not found {} alias in the keystore. Will try to load secret from file.", keyAlias);
        }
        if (jwtSecret == null) {
            if (!isJwtFilePathEmpty()) {
                try {
                    String keyInBase64 = new String(Files.readAllBytes(Paths.get(new File(filePath).getAbsolutePath())));
                    byte[] key = Base64.getDecoder().decode(keyInBase64);
                    jwtSecret = new SecretKeySpec(key, signatureAlgorithm);
                } catch (IOException e) {
                    log.error("Error reading secret key from file {}.", filePath, e);
                }
            } else {
                log.warn("Path to the file with JWT secret key not specified!");
            }
        }
    }

    public Key getJwtSecret() {
        return jwtSecret;
    }

    private boolean isJwtFilePathEmpty() {
        return filePath == null || filePath.isEmpty();
    }

    public String getSignatureAlgorithm() {
        return signatureAlgorithm;
    }
}
