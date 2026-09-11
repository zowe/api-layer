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
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.ECDSAVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.jws.AlgorithmIdentifiers;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.registry.client.spring.RegistryCacheRefreshedEvent;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsConfigError;
import org.zowe.apiml.security.SecurityUtils;
import org.zowe.apiml.zaas.security.login.Providers;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

/**
 * JWT Security related configuration. Distinguishes between methods used to generate JWT tokens provided by ZAAS.
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

    private JWSAlgorithm signatureAlgorithm;
    private PrivateKey jwtSecret;
    private PublicKey jwtPublicKey;
    private JWSVerifier jwtVerifier;

    private Optional<JsonWebKey> jwkPublicKey = Optional.empty();

    private final Providers providers;
    private final ZosmfListener zosmfListener;
    private final String zosmfServiceId;

    private final Set<String> events = Collections.synchronizedSet(new HashSet<>());

    @Autowired
    public JwtSecurity(Providers providers) {
        this.providers = providers;
        this.zosmfServiceId = providers.getZosmfServiceId();
        this.zosmfListener = new ZosmfListener();
    }

    @VisibleForTesting
    JwtSecurity(Providers providers, String keyAlias, String keyStore, char[] keyStorePassword, char[] keyPassword) {
        this(providers);

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
     * In case the configuration is altogether invalid, stop the ZAAS with the appropriate ERROR. This could
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
                return JwtProducer.UNKNOWN; // TODO remove autoconfiguration of zOSMF JWT
            }
        }
    }

    /**
     * Load the JWT secret. If there is a configuration issue the keys are not loaded and the error is logged.
     */
    private void loadJwtSecret() {
        signatureAlgorithm = JWSAlgorithm.RS256;

        HttpsConfig config = currentConfig();
        try {
            jwtSecret = SecurityUtils.loadKey(config);
            jwtPublicKey = SecurityUtils.loadPublicKey(config);
            jwtVerifier = buildVerifier(jwtPublicKey);
            jwkPublicKey = getJwkPublicKey();
        } catch (HttpsConfigError er) {
            apimlLog.log("org.zowe.apiml.zaas.jwtInitConfigError", er.getCode(), er.getMessage());
        }
    }

    /**
     * Validate JWT secret. If there is an issue fail the ZAAS startup. Should only validate the JWT secret
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
    @VisibleForTesting
    public JWSAlgorithm getSignatureAlgorithm() {
        return signatureAlgorithm;
    }

    public PrivateKey getJwtSecret() {
        return jwtSecret;
    }

    public PublicKey getJwtPublicKey() {
        return jwtPublicKey;
    }

    public String getJwtAlgorithm() {
        if (jwtPublicKey instanceof ECPublicKey) {
            return AlgorithmIdentifiers.ECDSA_USING_P256_CURVE_AND_SHA256;
        }
        return AlgorithmIdentifiers.RSA_USING_SHA256;
    }

    public JWSVerifier getJwtVerifier() {
        return jwtVerifier;
    }

    @VisibleForTesting
    JWSVerifier buildVerifier(PublicKey publicKey) {
        try {
            if (publicKey instanceof RSAPublicKey rsaPublicKey) {
                log.debug("Creating RSASSAVerifier for public key");
                return new RSASSAVerifier(rsaPublicKey);
            } else if (publicKey instanceof ECPublicKey ecPublicKey) {
                log.debug("Creating ECDSAVerifier for public key");
                return new ECDSAVerifier(ecPublicKey);
            } else {
                log.warn("Unsupported public key type for JWT verification: {}", publicKey == null ? null : publicKey.getClass());
                return null;
            }
        } catch (com.nimbusds.jose.JOSEException e) {
            log.warn("Failed to create JWT verifier for key type {}: {}", publicKey == null ? null : publicKey.getClass(), e.getMessage());
            return null;
        }
    }

    public JsonWebKeySet getPublicKeyInSet() {
        List<JsonWebKey> keys = new ArrayList<>();

        var publicKeyOptional = getJwkPublicKey();
        publicKeyOptional.ifPresent(keys::add);
        return new JsonWebKeySet(keys);
    }

    public Optional<JsonWebKey> getJwkPublicKey() {
        if (jwkPublicKey.isPresent()) return jwkPublicKey;
        if (jwtPublicKey instanceof RSAPublicKey rsaPublicKey) {
            try {
                var jwk = JsonWebKey.Factory.newJwk(rsaPublicKey);
                jwk.setKeyId(jwk.calculateBase64urlEncodedThumbprint("SHA-256"));
                jwkPublicKey = Optional.of(jwk);
                return jwkPublicKey;
            } catch (JoseException e) {
                log.debug("Unable to create JWK {}", e.getMessage(), e);
            }
        } else {
            log.debug("Unsupported type of public key: {}", jwtPublicKey == null ? null : jwtPublicKey.getClass());
        }

        return Optional.empty();
    }
    /*
     * End of the actual API for the security class
     */

    /**
     * Register event listener
     */
    private void validateInitializationWhenZosmfIsAvailable() {
        zosmfListener.register();
        events.add("Started waiting for z/OSMF instance " + zosmfServiceId + " to be registered and known by the discovery service");
        log.debug("Waiting for z/OSMF instance {} to be registered and known by the Discovery Service.", zosmfServiceId);

        new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    if (!zosmfListener.isZosmfReady()) {
                        synchronized (events) {
                            apimlLog.log("org.zowe.apiml.zaas.jwtProducerConfigError", StringUtils.join(events, "\n"));
                        }
                        apimlLog.log("org.zowe.apiml.security.zosmfInstanceNotFound", zosmfServiceId);
                    }
                }
            }, Duration.ofMinutes(1).toMillis()
        );
    }

    /**
     * Only for unit testing
     */
    @VisibleForTesting
    public ZosmfListener getZosmfListener() {
        return zosmfListener;
    }

    /**
     * Watches the registry until z/OSMF shows up, then decides who produces JWT tokens.
     * <p>
     * Previously a {@code EurekaEventListener} that unregistered itself from the Eureka client once it had
     * seen z/OSMF up. There is nothing to unregister from now: the listener is a Spring
     * {@code @EventListener} on {@link RegistryCacheRefreshedEvent} and simply stops acting once
     * {@link #isZosmfReady()} is set, which is checked here rather than by removing a callback from a
     * registry - the operation that, done in the wrong order, used to leave a listener holding a reference
     * to a closed application context.
     */
    public class ZosmfListener {

        private boolean isZosmfReady = false;

        private ZosmfListener() {
        }

        /**
         * Called whenever this service's view of the registry changes.
         * <p>
         * Idempotent and cheap once z/OSMF has been seen: the whole point is that the check runs on every
         * refresh until it succeeds exactly once.
         */
        public void onRegistryRefreshed() {
            if (isZosmfReady) {
                return;
            }

            events.add("Discovery Service Cache was updated.");
            log.debug("Trying to reach the z/OSMF instance " + zosmfServiceId + ".");
            if (providers.isZosmfAvailableAndOnline()) {
                events.add("z/OSMF instance " + zosmfServiceId + " is available and online.");
                log.debug("The z/OSMF instance {} was reached.", zosmfServiceId);

                isZosmfReady = true; // only need to see zosmf up once to validate jwt secret

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

        /**
         * No longer registers anything; kept because {@link #validateInitializationWhenZosmfIsAvailable()}
         * reads as a sequence and the timer below is the half that still matters.
         */
        public void register() {
            log.debug("Watching the registry for z/OSMF instance {}", zosmfServiceId);
        }

        public boolean isZosmfReady() {
            return isZosmfReady;
        }
    }

    /**
     * Bridges the registry refresh to {@link ZosmfListener}.
     * <p>
     * On the {@code JwtSecurity} bean rather than the inner class so the container can see it: an inner
     * class is not a bean, and {@code @EventListener} is only honoured on beans.
     */
    @EventListener
    public void onRegistryCacheRefreshed(RegistryCacheRefreshedEvent event) {
        zosmfListener.onRegistryRefreshed();
    }

    public enum JwtProducer {
        ZOSMF,
        APIML,
        UNKNOWN
    }
}
