/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service;

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.*;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Optional;

import static org.awaitility.Awaitility.await;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtSecurityInitializer {

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private char[] keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.auth.jwtKeyAlias:}")
    private String keyAlias;

    private SignatureAlgorithm signatureAlgorithm;
    private Key jwtSecret;
    private PublicKey jwtPublicKey;

    private Providers providers;

    @Autowired
    public JwtSecurityInitializer(Providers providers) {
        this.providers = providers;
    }

    public JwtSecurityInitializer(Providers providers, String keyAlias, String keyStore, char[] keyStorePassword, char[] keyPassword) {
        this(providers);

        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        this.keyAlias = keyAlias;
        this.keyStoreType = "PKCS12";
    }

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    @PostConstruct
    public void init() {
        // zOSMF isn't the authentication provider.
        if (!providers.isZosfmUsed()) {
            log.debug("zOSMF isn't used as the Authentication provider");
            loadJwtSecret();
        // zOSMF is authentication provider
        } else {
            // zOSMF isn't available at the moment.
            log.debug("zOSMF is used as authentication provider");
            if (!providers.isZosmfAvailableAndOnline()) {
                // Wait for some time before giving up. Mainly used in the integration testing.
                waitUntilZosmfIsUp();
            } else {
                // zOSMF is UP and the APAR PH12143 isn't applied
                if (!providers.zosmfSupportsJwt()) {
                    log.debug("zOSMF is UP and APAR PH12143 was not applied");
                    loadJwtSecret();
                } else {
                    log.debug("zOSMF is UP and APAR PH12143 was applied");
                }
            }
        }
    }

    /**
     * The PostConstruct happens on the main thread and as such waiting on this thread stops the whole application.
     * Therefore the waiting is externalized to another thread, which kills the VM if the setup is unsuccesfull. .
     */
    private void waitUntilZosmfIsUp() {
        new Thread(() -> {
            try {
                await()
                    .atMost(Duration.FIVE_MINUTES)
                    .with()
                    .pollInterval(Duration.ONE_MINUTE)
                    .until(providers::isZosmfAvailableAndOnline);
            } catch (ConditionTimeoutException ex) {
                apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", "zOSMF");

                System.exit(1);
            }

            if (!providers.zosmfSupportsJwt()) {
                try {
                    loadJwtSecret();
                } catch (HttpsConfigError exception) {
                    System.exit(1);
                }
            }
        }).start();
    }

    /**
     * Load the JWT secret. If there is an configuration issue the error is thrown.
     */
    private void loadJwtSecret() {
        signatureAlgorithm = SignatureAlgorithm.RS256;
        HttpsConfig config = HttpsConfig.builder().keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
            .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).build();
        try {
            jwtSecret = SecurityUtils.loadKey(config);
            jwtPublicKey = SecurityUtils.loadPublicKey(config);
        } catch (HttpsConfigError er) {
            apimlLog.log("org.zowe.apiml.gateway.jwtInitConfigError", er.getCode(), er.getMessage());
        }

        if (jwtSecret == null || jwtPublicKey == null) {
            String errorMessage = String.format("Not found '%s' key alias in the keystore '%s'.", keyAlias, keyStore);
            apimlLog.log("org.zowe.apiml.gateway.jwtKeyMissing", keyAlias, keyStore);
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

    public Optional<JWK> getJwkPublicKey() {
        if (jwtPublicKey == null) {
            return Optional.empty();
        }

        final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) jwtPublicKey).build();
        return Optional.of(rsaKey.toPublicJWK());
    }

}
