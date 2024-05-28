/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service;

import com.google.common.annotations.VisibleForTesting;
import com.netflix.discovery.CacheRefreshedEvent;
import com.netflix.discovery.DiscoveryClient;
import com.netflix.discovery.EurekaEvent;
import com.netflix.discovery.EurekaEventListener;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.awaitility.Durations;
import org.awaitility.core.ConditionTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.zaas.security.login.Providers;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
    private PrivateKey jwtSecret;
    private PublicKey jwtPublicKey;

    private final Providers providers;
    private final ZosmfListener zosmfListener;
    private final String zosmfServiceId;

    private final Set<String> events = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    public JwtSecurity(Providers providers, DiscoveryClient discoveryClient) {
        this.providers = providers;
        this.zosmfServiceId = providers.getZosmfServiceId();
        this.zosmfListener = new ZosmfListener(discoveryClient);
    }

    @VisibleForTesting
    JwtSecurity(Providers providers, String keyAlias, String keyStore, char[] keyStorePassword, char[] keyPassword, DiscoveryClient discoveryClient) {
        this(providers, discoveryClient);

        this.keyStore = keyStore;
        this.keyStorePassword = keyStorePassword;
        this.keyPassword = keyPassword;
        this.keyAlias = keyAlias;
        this.keyStoreType = "PKCS12";
    }

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    void updateStorePaths() {
        if (SecurityUtils.isKeyring(keyStore)) {
            keyStore = SecurityUtils.formatKeyringUrl(keyStore);
            if (keyStorePassword == null) keyStorePassword = "password".toCharArray();
        }
    }

    /**
     * When the class is constructed and fully set, understand the zOSMF configuration and/or API ML configuration to
     * load the key used to sign the JWT token.
     * <p>
     * In case the configuration is altogether invalid, stop the Gateway Service with the appropriate ERROR. This could
     * take a while as we are waiting in certain scenarios for the zOSMF to properly start.
     */
    @PostConstruct
    public void loadAppropriateJwtKeyOrFail() {
        updateStorePaths();
        JwtProducer used = actualJwtProducer(providers.isZosmfConfigurationSetToLtpa());
        loadJwtSecret();
        switch (used) {
            case ZOSMF:
                log.info("z/OSMF instance {} is used as the JWT producer", zosmfServiceId);
                events.add(String.format("z/OSMF instance %s is recognized as authentication provider.", zosmfServiceId));
                validateInitializationAgainstZosmf();
                break;
            case APIML:
                log.info("API ML is used as the JWT producer");
                events.add("API ML is recognized as authentication provider.");
                validateJwtSecret();
                break;
            case UNKNOWN:
                log.info("z/OSMF instance {} is probably used as the JWT producer but isn't available yet.", zosmfServiceId);
                events.add(String.format("Wait for z/OSMF instance %s to come online before deciding who provides JWT tokens.", zosmfServiceId));
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
        return actualJwtProducer(providers.isZosmfConfigurationSetToLtpa() || !providers.zosmfSupportsJwt());
    }

    public JwtProducer actualJwtProducer(boolean isLtpaSupported) {
        if (!providers.isZosfmUsed()) {
            return JwtProducer.APIML;
        } else {
            if (isLtpaSupported) {
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
        signatureAlgorithm = Jwts.SIG.RS256;

        HttpsConfig config = currentConfig();
        try {
            jwtSecret = SecurityUtils.loadKey(config);
            jwtPublicKey = SecurityUtils.loadPublicKey(config);
        } catch (HttpsConfigError er) {
            apimlLog.log("org.zowe.apiml.zaas.jwtInitConfigError", er.getCode(), er.getMessage());
        }
    }

    /**
     * Validate JWT secret. If there is an issue fail the Gateway startup. Should only validate the JWT secret
     * when the secret is required.
     */
    private void validateJwtSecret() {
        if (jwtSecret == null || jwtPublicKey == null) {
            apimlLog.log("org.zowe.apiml.zaas.jwtKeyMissing", keyAlias, keyStore);

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
            log.debug("z/OSMF instance {} is UP and does not support JWT", zosmfServiceId);
            validateJwtSecret();
        } else {
            events.add(String.format("z/OSMF instance %s is UP and supports JWT", zosmfServiceId));
            log.debug("z/OSMF instance {} is UP and supports JWT", zosmfServiceId);
        }
    }

    /*
     * Start of the actual API for the security class
     */
    public SignatureAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public PrivateKey getJwtSecret() {
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
                events.add("Started waiting for z/OSMF instance " + zosmfServiceId + " to be registered and known by the discovery service");
                log.debug("Waiting for z/OSMF instance {} to be registered and known by the Discovery Service.", zosmfServiceId);
                await()
                    .atMost(Duration.of(timeout, ChronoUnit.MINUTES))
                    .with()
                    .pollInterval(Durations.ONE_MINUTE)
                    .until(zosmfListener::isZosmfReady);
            } catch (ConditionTimeoutException e) {
                synchronized (events) {
                    apimlLog.log("org.zowe.apiml.zaas.jwtProducerConfigError", StringUtils.join(events, "\n"));
                }
                apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", zosmfServiceId);
            }
        }).start();
    }

    /**
     * Only for unit testing
     */
    @VisibleForTesting
    ZosmfListener getZosmfListener() {
        return zosmfListener;
    }

    class ZosmfListener {
        private boolean isZosmfReady = false;
        private final DiscoveryClient discoveryClient;

        private ZosmfListener(DiscoveryClient discoveryClient) {
            this.discoveryClient = discoveryClient;
        }

        // instance variable so can create an accessor for unit testing purposes
        private final EurekaEventListener zosmfRegisteredListener = new EurekaEventListener() {
            @Override
            public void onEvent(EurekaEvent event) {
                if (!(event instanceof CacheRefreshedEvent)) {
                    return;
                }

                events.add("Discovery Service Cache was updated.");
                log.debug("Trying to reach the z/OSMF instance " + zosmfServiceId + ".");
                if (providers.isZosmfAvailableAndOnline()) {
                    events.add("z/OSMF instance " + zosmfServiceId + " is available and online.");
                    log.debug("The z/OSMF instance {} was reached.", zosmfServiceId);

                    discoveryClient.unregisterEventListener(this); // only need to see zosmf up once to validate jwt secret
                    isZosmfReady = true;

                    try {
                        validateInitializationAgainstZosmf();
                    } catch (HttpsConfigError e) {
                        synchronized (events) {
                            apimlLog.log("org.zowe.apiml.zaas.jwtProducerConfigError", StringUtils.join(events, "\n"));
                        }
                        System.exit(1); // TODO remove
                    }
                } else {
                    events.add("z/OSMF instance " + zosmfServiceId + " is not available and online yet.");
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
