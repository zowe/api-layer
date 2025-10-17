/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.utils;

import io.jsonwebtoken.Jwts;
import lombok.SneakyThrows;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;

import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class JWTUtils {

    public static String createZoweJwtToken(String username, String domain, String ltpaToken, HttpsConfig config) {
        return createToken(username, domain, ltpaToken, config, "APIML");
    }

    public static String createZosmfJwtToken(String username, String domain, String ltpaToken, HttpsConfig config) {
        return createToken(username, domain, ltpaToken, config, "zOSMF");
    }

    public static String createToken(String username, String domain, String ltpaToken, HttpsConfig config, String issuer) {
        long now = System.currentTimeMillis();
        long expiration = now + 100_000L;
        Key jwtSecret = SecurityUtils.loadKey(config);

        return Jwts.builder()
            .subject(username)
            .claim("dom", domain)
            .claim("ltpa", ltpaToken)
            .issuedAt(new Date(now))
            .expiration(new Date(expiration))
            .issuer(issuer)
            .id(UUID.randomUUID().toString())
            .signWith(jwtSecret)
            .compact();
    }

    @SneakyThrows
    public static String createTokenWithUserFields() {
        var now = Instant.now();
        var jwkAndSet = loadPrivateKey("../keystore/localhost/localhost.keystore.p12", "localhost", "password");
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
