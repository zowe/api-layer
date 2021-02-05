/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.client.services.apars;

import io.jsonwebtoken.Jwts;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.*;

public class PH12143 extends FunctionalApar {
    private final String keystorePath;

    public PH12143(List<String> usernames, List<String> passwords, String keystorePath) {
        super(usernames, passwords);
        this.keystorePath = keystorePath;
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        String authorization = headers.get("authorization");

        if (containsInvalidUser(authorization)) {
            return Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }

        String[] credentials = getPiecesOfCredentials(authorization);
        return Optional.of(validJwtResponse(response, credentials[0]));
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        String authorization = headers.get("authorization");

        if (containsInvalidUser(authorization)) {
            return Optional.of(new ResponseEntity<>(HttpStatus.UNAUTHORIZED));
        }

        String[] credentials = getPiecesOfCredentials(authorization);
        return Optional.of(validJwtResponse(response, credentials[0]));
    }

    @Override
    protected Optional<ResponseEntity<?>> handleAuthenticationDelete() {
        return Optional.of(new ResponseEntity<>(HttpStatus.NO_CONTENT));
    }

    private ResponseEntity<?> validJwtResponse(HttpServletResponse response, String username) {
        Date current = new Date();
        final int HOUR = 3600000;
        Date expiration = new Date(current.getTime() + 8 * HOUR);

        String jwtToken = Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(expiration)
            .setIssuer("zOSMF")
            .setId(UUID.randomUUID().toString())
            .signWith(getKeyForSigning())
            .compact();

        // Build a valid JWT token
        Cookie jwtCookie = new Cookie("jwtToken", jwtToken);
        jwtCookie.setSecure(true);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");

        response.addCookie(jwtCookie);

        Cookie ltpaToken = new Cookie("LtpaToken2", "paMypL7yRO/IBroQtro21/uSC2LTrJvOuYebHaPc6JAUNWQ7lEHHt1l3CYeXa/nP6aKLFHTuyWy3qlRXvt10PjVdVl+7Q+wavgIsro7odz+PvTaJBp/+r0AH+DHYcdZikKe8dytGYZRH2c2gw8Gv3PliDIMd1iPEazY4HeYTU5VCFM5cBJkeIoTXCfL5ud9wTzrkY2c4h1PQPtx+hYCF4kEpiVkqIypVwjQLzWdJGV1Ihz7NqH/UU9MMJRXY1xMqsWZSibs2fX5MVK77dnyBrNYjVXA7PqYL6U/v5/1UCvuYQ/iEU9+Uy95J+xFEsnTX");

        ltpaToken.setSecure(true);
        ltpaToken.setHttpOnly(true);
        ltpaToken.setPath("/");

        response.addCookie(ltpaToken);
        response.setContentType("application/json");
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    private Key getKeyForSigning() {
        try (FileInputStream keystore = new FileInputStream(new File(keystorePath))) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(
                keystore,
                "password".toCharArray()
            );
            return ks.getKey("localhost", "password".toCharArray());
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
