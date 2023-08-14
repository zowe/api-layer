/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.verify;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.net.URL;
import java.security.*;

/**
 * Service to retrieve the public key during initialization of CertificateValidator bean. The public key is then used to verify the
 * certificate's signature provided on the request header.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class CertificateValidator {
    private PublicKey publicKey;
    private static final String ALGORITHM = "SHA256withRSA";
    @Value("${apiml.security.x509.publicKeyUrl:}")
    private String publicKeyEndpoint;

    @Cacheable(value = "publicKey", key = "#publicKey", condition = "#publicKey != null")
    public void initializePublicKey() {
        try {
            JWKSet jwkSet = JWKSet.load(new URL(publicKeyEndpoint));
            RSAKey rsaKey = (RSAKey) jwkSet.getKeys().get(0);
            publicKey = rsaKey.toPublicKey();
        } catch (Exception e) {
            log.error("Failed to initialize public key. {}", e.getMessage());
        }
    }

    public boolean verify(byte[] data, byte[] dataSignature) throws SignatureException {
        initializePublicKey();
        Signature signature;
        try {
            signature = Signature.getInstance(ALGORITHM);
            signature.initVerify(publicKey);
            signature.update(data);
            return signature.verify(dataSignature);
        } catch (NoSuchAlgorithmException e) {
            log.warn("Failed to verify the signature. The cryptographic algorithm {} is not available in the environment. {}", ALGORITHM, e.getMessage());
            throw new SignatureException(e);
        } catch (InvalidKeyException e) {
            log.warn("Failed to verify the signature due to the invalid public key. {}", e.getMessage());
            throw new SignatureException(e);
        }
    }
}
