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
import io.jsonwebtoken.SignatureAlgorithm;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;

import java.security.Key;
import java.util.Date;
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
            .setSubject(username)
            .claim("dom", domain)
            .claim("ltpa", ltpaToken)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer(issuer)
            .setId(UUID.randomUUID().toString())
            .signWith(jwtSecret, SignatureAlgorithm.RS256)
            .compact();
    }
}
