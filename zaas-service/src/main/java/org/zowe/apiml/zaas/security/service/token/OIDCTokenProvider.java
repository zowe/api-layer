/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKException;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.JWTParser;
import com.nimbusds.jwt.SignedJWT;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.http.HttpHeaders;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.jose4j.lang.JoseException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.security.common.token.OIDCProvider;

import java.io.IOException;
import java.security.interfaces.RSAPublicKey;
import java.text.ParseException;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnExpression("'${apiml.security.oidc.enabled:false}' == 'true'")
public class OIDCTokenProvider implements OIDCProvider {

    @Value("${apiml.security.oidc.jwks.uri}")
    private List<String> jwksUri;

    @Value("${apiml.security.oidc.jwks.refreshInternalHours:1}")
    private int jwkRefreshInterval;

    @Qualifier("oidcJwtClock")
    private final Clock clock;

    @Value("${apiml.security.oidc.userInfo.uri}")
    private String endpointUrl;

    private final JWKResolver jwkResolver;
    private final CloseableHttpClient secureHttpClientWithKeystore;
    @Getter
    private final Map<String, JsonWebKey> publicKeys = new ConcurrentHashMap<>();
    @Getter
    private JsonWebKeySet jwkSet;

    @PostConstruct
    public void afterPropertiesSet() {
        this.fetchJWKSet();
        Executors.newSingleThreadScheduledExecutor(r -> new Thread(r, "OIDC JWK Refresh"))
            .scheduleAtFixedRate(this::fetchJWKSet, jwkRefreshInterval, jwkRefreshInterval, TimeUnit.HOURS);
    }

    @Retryable
    void fetchJWKSet() {
        if (jwksUri == null || jwksUri.isEmpty()) {
            log.debug("OIDC JWK URI not provided, JWK refresh not performed");
            return;
        }

        publicKeys.clear();
        for (String url : jwksUri) {
            log.debug("Refreshing JWK endpoints {}", url);
            try {
                var keySet = jwkResolver.resolve(url);
                keySet.getJsonWebKeys().forEach(jwk -> publicKeys.put(jwk.getKeyId(), jwk));
            } catch (IOException | IllegalStateException | JoseException e) {
                log.error("Error processing response from URI {} message: {}", url, e.getMessage());
            }
        }

        jwkSet = new JsonWebKeySet(publicKeys.entrySet().stream().map(entry -> entry.getValue()).toList());
    }

    @Override
    public boolean isValid(String token) {
        try {
            if (CollectionUtils.isEmpty(jwksUri) || getClaims(token) == null) {
                return isValidExternal(token);
            }
            return true;
        } catch (ParseException e) {
            log.debug("Malformed JWT: {}", e.getMessage(), e.getCause());
            return false;
        } catch (JOSEException e) {
            log.debug("JWK token validation failed with the exception {}", e.getMessage(), e.getCause());
            return isValidExternal(token);
        } catch (BadJOSEException e) {
            log.debug("Bad JWT: {}", e.getMessage(), e.getCause());
            return false;
        }

    }

    public boolean isValidExternal(String token) {
        try {
            if (StringUtils.isBlank(endpointUrl)) {
                log.debug("JWT can't be validated externally because endpoint URL was not provided.");
                return false;
            }
            log.debug("Validating the token against URL: {}", endpointUrl);
            var httpGet = new HttpGet(endpointUrl);
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token);

            return secureHttpClientWithKeystore.execute(httpGet, response -> {
                final int responseCode = response.getCode();
                log.debug("Response code: {}", responseCode);
                return HttpStatus.valueOf(responseCode).is2xxSuccessful();
            });
        } catch (IOException e) {
            log.error("An error occurred during validation of OIDC token using userInfo URI {}: {}", endpointUrl, e.getMessage());
            return false;
        }

    }

    JWTClaimsSet getClaims(String token) throws ParseException, BadJOSEException, JOSEException {
        if (jwkSet == null || jwkSet.getJsonWebKeys().isEmpty()) {
            fetchJWKSet();
        }

        if (jwkSet == null || jwkSet.getJsonWebKeys().isEmpty()) {
            throw new JWKException("Could not validate the token due to missing public key.");
        }

        if (StringUtils.isBlank(token)) {
            throw new BadJOSEException("Empty string provided instead of a token.");
        }

        log.debug("Validating the token with JWK");
        var jwt = JWTParser.parse(token);
        if (jwt instanceof SignedJWT signedJwt) {
            if (StringUtils.isBlank(signedJwt.getHeader().getKeyID())) {
                throw new JWKException("Token does not provide kid. It uses an unsupported type of signature.");
            }

            var jsonWebKey = publicKeys.get(signedJwt.getHeader().getKeyID());
            if (jsonWebKey != null) {
                var rsaVerifier = new RSASSAVerifier((RSAPublicKey) jsonWebKey.getKey());
                var verified = signedJwt.verify(rsaVerifier);
                if (verified) {
                    var claims = jwt.getJWTClaimsSet();
                    if (claims.getExpirationTime().toInstant().isBefore(clock.instant())) {
                        log.debug("OIDC Token is expired");
                        return null;
                    }
                    return claims;
                } else {
                    throw new BadJOSEException("Provided OIDC JWT token has invalid signature");
                }
            } else {
                throw new JWKException("Key with id " + signedJwt.getHeader().getKeyID() + " is null in JWK");
            }
        } else {
            log.debug("OIDC Token is not signed");
        }
        return null;

    }

}
