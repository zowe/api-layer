/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.util;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.Payload;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import io.jsonwebtoken.Jwts;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
public class JWTTestUtils {

    public static String createZoweJwtToken(String username, String domain, String ltpaToken, HttpsConfig config) {
        return createToken(username, domain, ltpaToken, null, null, config, "APIML");
    }

    public static String createExpiredZoweJwtToken(String username, String domain, String ltpaToken, HttpsConfig config) {
        return createToken(username, domain, ltpaToken, System.currentTimeMillis() - Duration.ofDays(1).toMillis(), null, config, "APIML");
    }

    public static String createZosmfJwtToken(String username, String domain, String ltpaToken, HttpsConfig config) {
        return createToken(username, domain, ltpaToken, null, null, config, "zOSMF");
    }

    public static String createZowePatJwtToken(String username, String domain, List<String> scopes, HttpsConfig config) {
        return createToken(username, domain, null, null, scopes, config, "APIML_PAT");
    }

    public static String createToken(String username, String domain, String ltpaToken, Long expiration, List<String> scopes, HttpsConfig config, String issuer) {
        long now = System.currentTimeMillis();
        if (expiration == null) {
            expiration = now + 100_000L;
        }
        Key jwtSecret = SecurityUtils.loadKey(config);

        var builder = Jwts.builder();

        builder
            .subject(username)
            .claim("dom", domain)
            .issuedAt(new Date(now))
            .expiration(new Date(expiration))
            .issuer(issuer)
            .id(UUID.randomUUID().toString())
            .signWith(jwtSecret);

        if (!StringUtils.isEmpty(ltpaToken)) {
            builder.claim("ltpa", ltpaToken);
        }

        if (scopes != null && scopes.size() > 0) {
            builder.claim("scopes", scopes);
        }

        return builder.compact();
    }

    public static String createDummyJwtToken(String username, String issuer, long expiration) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(username)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expiration))
            .issuer(issuer)
            .id(UUID.randomUUID().toString())
            .compact();
    }

    public static String createDummyJwtToken(String username, String issuer) {
        return createDummyJwtToken(username, issuer, 100_000L);
    }

    public static String createDummyAPIMLToken(String username) {
        return createDummyJwtToken(username, "APIML");
    }

    public static String createDummyZOSMFToken(String username) {
        return createDummyJwtToken(username, "ZOSMF");
    }

    public static String createDummySignedJwt(String username, String issuer, String customPayload, String keyId) {
        var algorithm = JWSAlgorithm.RS256;
        var now = Instant.now();
        Payload payload = null;
        if (customPayload != null) {
            payload = new Payload(customPayload);
        } else if (username != null && issuer != null) {
            var claims = new JWTClaimsSet.Builder()
                .subject(username)
                .issuer(issuer)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(1200)))
                .build();
            payload = claims.toPayload();
        }
        var header = new JWSHeader.Builder(algorithm).keyID(keyId).build().toBase64URL();
        var signature = Base64URL.encode("signature-bytes");
        assert payload != null;
        return header + "." + payload.toBase64URL() + "." + signature;
    }

    @SneakyThrows
    public static String createTokenWithUserFields() {
        var now = Instant.now();
        var jwkAndSet = loadPrivateKey("../keystore/service/service.keystore.p12", "localhost", "password");
        return Jwts.builder()
            .header().keyId("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4").and()
            .subject("oidc.username")
            .claim("email", "username@oidc.org")
            .claim("nullValue", null)
            .claim("org", Map.of(
                "name", "openmainframe",
                "dep", Map.of(
                    "name", "zowe",
                    "team", "apiml",
                    "contributor", "contributor@apiml.zowe",
                    "nickname", ""
                )))
            .claim("memberOf", List.of("openmainframe", "zowe", "apiml"))
            .claim("groups", Map.of("memberOf", List.of("openmainframe", "zowe", "apiml")))
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plusSeconds(1200)))
            .issuer("API ML")
            .id(UUID.randomUUID().toString())
            .signWith(jwkAndSet.privateKey())
            .compact();
    }

    public static JwkAndSet loadPrivateKey(String path, String alias, String password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (FileInputStream fis = new FileInputStream(path)) {
            ks.load(fis, password.toCharArray());
        }
        Key key = ks.getKey(alias, password.toCharArray());
        var cert = ks.getCertificate(alias);
        var pubKey = cert.getPublicKey();
        if (pubKey instanceof RSAPublicKey rsaPublicKey) {
            var jwk = JsonWebKey.Factory.newJwk(rsaPublicKey);
            jwk.setKeyId("Lcxckkor94qkrunxHP7Tkib547rzmkXvsYV-nc6U-N4");
            return new JwkAndSet((PrivateKey) key, new JsonWebKeySet(jwk));
        }

        return new JwkAndSet((PrivateKey) key, null);
    }

    public record JwkAndSet(PrivateKey privateKey, JsonWebKeySet jwkSet) {
    }
}
