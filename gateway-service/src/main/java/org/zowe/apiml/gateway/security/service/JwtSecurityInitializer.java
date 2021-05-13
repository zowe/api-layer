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

import com.netflix.discovery.*;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.awaitility.Duration;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.discovery.ApimlDiscoveryClient;
import org.zowe.apiml.gateway.security.login.Providers;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.*;

import javax.annotation.PostConstruct;
import java.security.*;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.util.Optional;

import static org.awaitility.Awaitility.await;

@Slf4j
@Service
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

    @Value("${server.attls.enabled:false}")
    private boolean isAttlsEnabled;

    private SignatureAlgorithm signatureAlgorithm;
    private Key jwtSecret;
    private PublicKey jwtPublicKey;

    private final Providers providers;
    private final ZosmfListener zosmfListener;

    @Autowired
    public JwtSecurityInitializer(Providers providers, ApimlDiscoveryClient discoveryClient) {
        this.providers = providers;
        this.zosmfListener = new ZosmfListener(discoveryClient);
    }

    public JwtSecurityInitializer(Providers providers, String keyAlias, String keyStore, char[] keyStorePassword, char[] keyPassword, ApimlDiscoveryClient discoveryClient) {
        this(providers, discoveryClient);

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
        loadJwtSecret();

        // check if JWT secret is actually needed and if so validate it
        if (!providers.isZosfmUsed()) {
            log.debug("zOSMF isn't used as the Authentication provider");
            validateJwtSecret();
        } else {
            log.debug("zOSMF is used as authentication provider");
            if (providers.isZosmfConfigurationSetToLtpa()) {
                log.debug("Configuration indicates zOSMF supports LTPA token");
                validateJwtSecret();
            } else if (providers.isZosmfAvailableAndOnline()) {
                validateInitializationAgainstZosmf();
            } else {
                validateInitializationWhenZosmfIsAvailable();
            }
        }
    }

    /**
     * Register event listener
     */
    private void validateInitializationWhenZosmfIsAvailable() {
        zosmfListener.register();

        new Thread(() -> {
            try {
                await()
                    .atMost(Duration.FIVE_MINUTES)
                .with()
                    .pollInterval(Duration.ONE_MINUTE)
                    .until(zosmfListener::isZosmfReady);
            } catch (ConditionTimeoutException e) {
                apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", "zOSMF");
                System.exit(1);
            }
        }).start();
    }

    /**
     * Load the JWT secret. If there is a configuration issue the error is thrown.
     */
    private void loadJwtSecret() {
        signatureAlgorithm = SignatureAlgorithm.RS256;
        if (isAttlsEnabled) {
            log.debug("Loading JWTSecret from environment (AT-TLS)");
            loadJwtSecretFromEnv();
        } else {
            log.debug("Loading JWTSecret from TLS configuration");
            loadJwtSecretFromTlsConfig();
        }
    }

    private void loadJwtSecretFromEnv() {
        try {
            jwtSecret = SecurityUtils.readPemPrivateKey();
            jwtPublicKey = SecurityUtils.readPemPublicKey();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            apimlLog.log("org.zowe.apiml.gateway.jwtInitConfigError", "",e.getMessage());
        }
    }

    private void loadJwtSecretFromTlsConfig() {
        HttpsConfig config = HttpsConfig.builder().keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
            .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).build();
        try {
            jwtSecret = SecurityUtils.loadKey(config);
            jwtPublicKey = SecurityUtils.loadPublicKey(config);
        } catch (HttpsConfigError er) {
            apimlLog.log("org.zowe.apiml.gateway.jwtInitConfigError", er.getCode(), er.getMessage());
        }
    }

    /**
     * Validate JWT secret. If there is an issue fail the Gateway startup. Should only validate the JWT secret
     * when the secret is required.
     */
    private void validateJwtSecret() {
        if (jwtSecret == null || jwtPublicKey == null) {
            apimlLog.log("org.zowe.apiml.gateway.jwtKeyMissing", keyAlias, keyStore);

            String errorMessage = String.format("Not found '%s' key alias in the keystore '%s'.", keyAlias, keyStore);
            HttpsConfig config = HttpsConfig.builder().keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
                .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).build();
            throw new HttpsConfigError(errorMessage, HttpsConfigError.ErrorCode.WRONG_KEY_ALIAS, config);
        }
    }

    private void validateInitializationAgainstZosmf() {
        if (!providers.zosmfSupportsJwt()) {
            log.debug("zOSMF is UP and does not support JWT");
            validateJwtSecret();
        } else {
            log.debug("zOSMF is UP and supports JWT");
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

    /**
     * Only for unit testing
     */
    ZosmfListener getZosmfListener() {
        return zosmfListener;
    }

    class ZosmfListener {
        private boolean isZosmfReady = false;
        private final ApimlDiscoveryClient discoveryClient;

        private ZosmfListener(ApimlDiscoveryClient discoveryClient) {
            this.discoveryClient = discoveryClient;
        }

        // instance variable so can create an accessor for unit testing purposes
        private final EurekaEventListener zosmfRegisteredListener = new EurekaEventListener() {
            @Override
            public void onEvent(EurekaEvent event) {
                if (event instanceof CacheRefreshedEvent && providers.isZosmfAvailableAndOnline()) {
                    discoveryClient.unregisterEventListener(this); // only need to see zosmf up once to validate jwt secret
                    isZosmfReady = true;

                    try {
                        validateInitializationAgainstZosmf();
                    } catch (HttpsConfigError e) {
                        System.exit(1);
                    }
                }
            }
        };

        public void register() {
            discoveryClient.registerEventListener(zosmfRegisteredListener);
        }

        public boolean isZosmfReady() {
            return isZosmfReady;
        }

        /**
         * Only for unit testing the event listener.
         */
        EurekaEventListener getZosmfRegisteredListener() {
            return zosmfRegisteredListener;
        }
    }
}
