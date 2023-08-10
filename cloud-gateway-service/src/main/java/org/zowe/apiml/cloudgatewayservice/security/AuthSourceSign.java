/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;

import javax.annotation.PostConstruct;
import java.security.*;

@Service
@Slf4j
public class AuthSourceSign {

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private char[] keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    private PrivateKey privateKey;

    @Getter
    private PublicKey publicKey;

    private static final String ALGORITHM = "SHA256withRSA";

    private final SecureRandom secureRandom = new SecureRandom();

    public byte[] sign(byte[] data) throws SignatureException {
        Signature signature;
        try {
            signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey, secureRandom);
            signature.update(data);
            return signature.sign();
        } catch (NoSuchAlgorithmException e) {
            log.warn("Failed to create the signature. The cryptographic algorithm {} is not available in the environment. {}", ALGORITHM, e.getMessage());
            throw new SignatureException(e);
        } catch (InvalidKeyException e) {
            log.warn("Failed to create the signature due to the invalid private key. {}", e.getMessage());
            throw new SignatureException(e);
        }
    }

    public boolean verify(byte[] data, byte[] dataSignature) throws SignatureException {
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

    @PostConstruct
    private void loadKeys() {
        HttpsConfig config = currentConfig();
        privateKey = (PrivateKey) SecurityUtils.loadKey(config);
        publicKey = SecurityUtils.loadPublicKey(config);
    }

    private HttpsConfig currentConfig() {
        return HttpsConfig.builder()
                .keyAlias(keyAlias)
                .keyStore(keyStore)
                .keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword)
                .keyStoreType(keyStoreType)
                .build();
    }
}
