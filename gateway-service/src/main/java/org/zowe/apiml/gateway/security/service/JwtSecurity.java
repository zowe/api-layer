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
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.awaitility.Durations;
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
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import static org.awaitility.Awaitility.await;

/**
 * JWT Security related configuration. Distinguishes between methods used to generate JWT tokens provided by API Gateway.
 * Loads proper keys and stops the service if there is no valid configuration available.
 */
@Slf4j
@Service
public class JwtSecurity {

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

    @Value("${apiml.security.jwtInitializerTimeout:5}")
    private int timeout;

    private SignatureAlgorithm signatureAlgorithm;
    private Key jwtSecret;
    private PublicKey jwtPublicKey;

    private final Providers providers;
    private final ZosmfListener zosmfListener;

    private final List<String> events = new ArrayList<>();

    @Autowired
    public JwtSecurity(Providers providers, ApimlDiscoveryClient discoveryClient) {
        this.providers = providers;
        this.zosmfListener = new ZosmfListener(discoveryClient);
    }

    public JwtSecurity(Providers providers, String keyAlias, String keyStore, char[] keyStorePassword, char[] keyPassword, ApimlDiscoveryClient discoveryClient) {
        this(providers, discoveryClient);

        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        this.keyAlias = keyAlias;
        this.keyStoreType = "PKCS12";
    }

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    /**
     * When the class is constructed and fully set, understand the zOSMF configuration and/or API ML configuration to
     * load the key used to sign the JWT token.
     *
     * In case the configuration is altogether invalid, stop the Gateway Service with the appropriate ERROR. This could
     * take a while as we are waiting in certain scenarios for the zOSMF to properly start.
     */
    @PostConstruct
    public void loadAppropriateJwtKeyOrFail() {
        JwtProducer used = actualJwtProducer();

        loadJwtSecret();
        switch (used) {
            case ZOSMF:
                log.info("zOSMF is used as the JWT producer");
                events.add("zOSMF is recognized as authentication provider.");
                validateInitializationAgainstZosmf();
                break;
            case APIML:
                log.info("API ML is used as the JWT producer");
                events.add("API ML is recognized as authentication provider.");
                validateJwtSecret();
                break;
            case UNKNOWN:
                log.info("zOSMF is probably used as the JWT producer but isn't available yet.");
                events.add("Wait for zOSMF to come online before deciding who provides JWT tokens.");
                validateInitializationWhenZosmfIsAvailable();
                break;
            default:
                log.warn("Unknown error when deciding who is providing the JWT token.");
        }
    }

    /**
     * Based on the configuration and the state decide whether we know actualJwtProvider and if we know then which one
     * is used.
     *
     * @return Currently used JWT Producer or Unknown.
     */
    public JwtProducer actualJwtProducer() {
        if (!providers.isZosfmUsed()) {
            return JwtProducer.APIML;
        } else {
            if (providers.isZosmfConfigurationSetToLtpa()) {
                return JwtProducer.APIML;
            } else if (providers.isZosmfAvailableAndOnline()) {
                return JwtProducer.ZOSMF;
            } else {
                return JwtProducer.UNKNOWN;
            }
        }
    }

    /**
     * Load the JWT secret. If there is a configuration issue the keys are not loaded and the error is logged.
     */
    private void loadJwtSecret() {
        signatureAlgorithm = SignatureAlgorithm.RS256;

        HttpsConfig config = currentConfig();
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
            HttpsConfig config = currentConfig();
            throw new HttpsConfigError(errorMessage, HttpsConfigError.ErrorCode.WRONG_KEY_ALIAS, config);
        }
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

    /**
     * Call the zOSMF to verify the actual status of the zOSMF.
     */
    private void validateInitializationAgainstZosmf() {
        if (!providers.zosmfSupportsJwt()) {
            events.add("API ML is responsible for token generation.");
            log.debug("zOSMF is UP and does not support JWT");
            validateJwtSecret();
        } else {
            events.add("zOSMF is UP and supports JWT");
            log.debug("zOSMF is UP and supports JWT");
        }
    }

    /*
     * Start of the actual API for the security class
     */
    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public Key getJwtSecret() {
        return jwtSecret;
    }

    public PublicKey getJwtPublicKey() {
        return jwtPublicKey;
    }

    public JWKSet getPublicKeyInSet() {
        final List<JWK> keys = new LinkedList<>();

        Optional<JWK> publicKey = getJwkPublicKey();
        publicKey.ifPresent(keys::add);

        return new JWKSet(keys);
    }

    public Optional<JWK> getJwkPublicKey() {
        if (jwtPublicKey == null) {
            return Optional.empty();
        }

        final RSAKey rsaKey = new RSAKey.Builder((RSAPublicKey) jwtPublicKey).build();
        return Optional.of(rsaKey.toPublicJWK());
    }
    /*
     * End of the actual API for the security class
     */

    /**
     * Register event listener
     */
    private void validateInitializationWhenZosmfIsAvailable() {
        zosmfListener.register();

        new Thread(() -> {
            try {
                events.add("Started waiting for zOSMF to be registered and known by the discovery service");
                log.debug("Waiting for zOSMF to be registered and known by the Discovery Service.");
                await()
                    .atMost(Duration.of(timeout, ChronoUnit.MINUTES))
                    .with()
                    .pollInterval(Durations.ONE_MINUTE)
                    .until(zosmfListener::isZosmfReady);
            } catch (ConditionTimeoutException e) {
                apimlLog.log("org.zowe.apiml.gateway.jwtProducerConfigError", StringUtils.join(events, "\n"));
                apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", "zOSMF");
                System.exit(1);
            }
        }).start();
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
                if (!(event instanceof  CacheRefreshedEvent)) {
                    return;
                }

                events.add("Discovery Service Cache was updated.");
                log.debug("Trying to reach the zOSMF.");
                if (providers.isZosmfAvailableAndOnline()) {
                    events.add("zOSMF is avaiable and online.");
                    log.debug("The zOSMF was reached ");

                    discoveryClient.unregisterEventListener(this); // only need to see zosmf up once to validate jwt secret
                    isZosmfReady = true;

                    try {
                        validateInitializationAgainstZosmf();
                    } catch (HttpsConfigError e) {
                        apimlLog.log("org.zowe.apiml.gateway.jwtProducerConfigError", StringUtils.join(events, "\n"));
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

    public enum JwtProducer {
        ZOSMF,
        APIML,
        UNKNOWN
    }
}
