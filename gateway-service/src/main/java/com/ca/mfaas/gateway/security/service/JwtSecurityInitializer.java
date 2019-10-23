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

import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsConfigError;
import com.ca.mfaas.security.SecurityUtils;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.security.PublicKey;


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

    @Value("${apiml.security.auth.jwtKeyAlias:jwtsecret}")
    private String keyAlias;

    private SignatureAlgorithm signatureAlgorithm;
    private Key jwtSecret;
    private PublicKey jwtPublicKey;

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    @PostConstruct
    public void init() {
        signatureAlgorithm = SignatureAlgorithm.RS256;
        HttpsConfig config = HttpsConfig.builder().keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
            .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).build();
        try {
            jwtSecret = SecurityUtils.loadKey(config);
            jwtPublicKey = SecurityUtils.loadPublicKey(config);
        } catch (HttpsConfigError er) {
            apimlLog.log("apiml.gateway.jwtInitConfigError", er.getCode(), er.getMessage());
        }
        if (jwtSecret == null || jwtPublicKey == null) {
            String errorMessage = String.format("Not found '%s' key alias in the keystore '%s'.", keyAlias, keyStore);
            apimlLog.log("apiml.gateway.jwtAliasNotFound", keyAlias, keyStore);
            throw new HttpsConfigError(errorMessage, HttpsConfigError.ErrorCode.WRONG_KEY_ALIAS, config);
        }
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public Key getJwtSecret() {
        return jwtSecret;
    }

    public PublicKey getJwtPublicKey() {
        return jwtPublicKey;
    }
}
