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

import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
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
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;

import javax.annotation.PostConstruct;
import java.security.Key;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
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

    private SignatureAlgorithm signatureAlgorithm;
    private Key jwtSecret;
    private PublicKey jwtPublicKey;

    private final Providers providers;
    private final ApimlDiscoveryClient discoveryClient;
    private boolean isZosmfReady = false;

    // instance variable so can create an accessor for unit testing purposes
    private final EurekaEventListener zosmfRegisteredListener = new EurekaEventListener() {
        @Override
        public void onEvent(EurekaEvent event) {
            if (event instanceof CacheRefreshedEvent) {
                boolean zosmf = providers.isZosmfAvailableAndOnline();
                if (zosmf) {
                    discoveryClient.unregisterEventListener(this); // only need to see zosmf up once to load jwt secret
                    if (!providers.zosmfSupportsJwt()) {
                        try {
                            loadJwtSecret();
                        } catch (HttpsConfigError exception) {
                            System.exit(1);
                        }
                    }

                    isZosmfReady = true;
                }
            }
        }
    };

    @Autowired
    public JwtSecurityInitializer(Providers providers, ApimlDiscoveryClient discoveryClient) {
        this.providers = providers;
        this.discoveryClient = discoveryClient;
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
        // zOSMF isn't the authentication provider.
        if (!providers.isZosfmUsed()) {
            log.debug("zOSMF isn't used as the Authentication provider");
            loadJwtSecret();
            // zOSMF is authentication provider
        } else {
            log.debug("zOSMF is used as authentication provider");
            if (providers.isZosmfConfigurationSetToLtpa()) {
                log.debug("Configuration indicates zOSMF supports LTPA token");
                loadJwtSecret();
            } else if (!providers.isZosmfAvailableAndOnline()) {
                // zOSMF isn't available at the moment, listen for registration and then check if zOSMF supports JWT
                waitUntilZosmfIsUp();
            } else {
                // zOSMF is UP and can determine if zOSMF supports JWT or not
                if (!providers.zosmfSupportsJwt()) {
                    log.debug("zOSMF is UP and does not support JWT");
                    loadJwtSecret();
                } else {
                    log.debug("zOSMF is UP and supports JWT");
                }
            }
        }
    }

    /**
     * Register event listener
     */
    private void waitUntilZosmfIsUp() {
        discoveryClient.registerEventListener(zosmfRegisteredListener);

        new Thread(() -> {
            try {
                await()
                    .atMost(Duration.FIVE_MINUTES)
                .with()
                    .pollInterval(Duration.ONE_MINUTE)
                    .until(this::isZosmfReady);
            } catch (ConditionTimeoutException e) {
                apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", "zOSMF");
                System.exit(1);
            }
        }).start();
    }

    /**
     * Awaitility requires a method, can't use an instance variable.
     */
    private boolean isZosmfReady() {
        return isZosmfReady;
    }

    /**
     * Only for unit testing the event listener.
     */
    EurekaEventListener getZosmfRegisteredListener() {
        return zosmfRegisteredListener;
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
