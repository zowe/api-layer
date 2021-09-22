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
import org.zowe.apiml.client.services.MockZosmfException;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyStore;
import java.util.*;

@SuppressWarnings({"squid:S1452", "squid:S1172"})
public class FunctionalApar implements Apar {
    private static final String COOKIE_HEADER = "cookie";
    private static final String JWT_TOKEN_NAME = "jwtToken";
    private static final String LTPA_TOKEN_NAME = "LtpaToken2";

    protected static final String AUTHORIZATION_HEADER = "authorization";

    private final List<String> usernames;
    private final List<String> passwords;

    protected FunctionalApar(List<String> usernames, List<String> passwords) {
        this.usernames = usernames;
        this.passwords = passwords;
    }

    @Override
    public Optional<ResponseEntity<?>> apply(Object... parameters) {
        String calledService = (String) parameters[0];
        String calledMethod = (String) parameters[1];
        Optional<ResponseEntity<?>> originalResult = (Optional<ResponseEntity<?>>) parameters[2];
        HttpServletResponse response = (HttpServletResponse) parameters[3];
        Map<String, String> headers = (Map<String, String>) parameters[4];
        ResponseEntity<?> result = null;

        if (calledService.equals("authentication")) {
            switch (calledMethod) {
                case "create":
                    result = handleAuthenticationCreate(headers, response);
                    break;
                case "verify":
                    result = handleAuthenticationVerify(headers, response);
                    break;
                case "delete":
                    result = handleAuthenticationDelete(headers);
                    break;
                default:
                    result = handleAuthenticationDefault(headers);
                    break;
            }
        }

        if (calledService.equals("information")) {
            result = handleInformation(headers, response);
        }

        if (calledService.equals("files")) {
            result = handleFiles(headers);
        }

        if (calledService.equals("jwtKeys")) {
            result = handleJwtKeys();
        }

        return result == null ? originalResult : Optional.of(result);
    }

    /**
     * Override to provide response entity when the JWT keys are requested from the zOSMF
     */
    protected ResponseEntity<?> handleJwtKeys() {
        return new ResponseEntity<>(HttpStatus.NOT_FOUND);
    }

    /**
     * Override to provide a response entity, or set fields (like cookies) in the HTTP response when the create method
     * for the authentication service is called with proper authorization.
     */
    protected ResponseEntity<?> handleAuthenticationCreate(Map<String, String> headers, HttpServletResponse response) {
        return null;
    }

    /**
     * Override to provide a response entity, or set fields (like cookies) in the HTTP response when the verify method
     * for the authentication service is called with proper authorization.
     */
    protected ResponseEntity<?> handleAuthenticationVerify(Map<String, String> headers, HttpServletResponse response) {
        return null;
    }

    /**
     * Override to provide a response entity when the delete method for the authentication service is called
     * with proper authorization.
     */
    protected ResponseEntity<?> handleAuthenticationDelete(Map<String, String> headers) {
        return null;
    }

    /**
     * Override to provide a response entity when the authentication service with proper authorization and the method
     * is not explicitly handled.
     */
    protected ResponseEntity<?> handleAuthenticationDefault(Map<String, String> headers) {
        return null;
    }

    /**
     * Override to provide a response entity when the information service is called with proper authorization.
     */
    protected ResponseEntity<?> handleInformation(Map<String, String> headers, HttpServletResponse response) {
        return null;
    }

    /**
     * Override to provide a response entity when the files service is called with proper authorization.
     */
    protected ResponseEntity<?> handleFiles(Map<String, String> headers) {
        return null;
    }

    protected boolean noAuthentication(Map<String, String> headers) {
        String basicAuth = headers.get(AUTHORIZATION_HEADER);
        String cookie = headers.get(COOKIE_HEADER);
        return (basicAuth == null || basicAuth.isEmpty()) && (cookie == null || cookie.isEmpty());
    }

    protected boolean containsInvalidOrNoUser(Map<String, String> headers) {
        String authorization = headers.get(AUTHORIZATION_HEADER);
        if (authorization == null || authorization.isEmpty()) {
            return true;
        }

        String[] piecesOfCredentials = getPiecesOfCredentials(headers);
        return piecesOfCredentials.length <= 0 || (!usernames.contains(piecesOfCredentials[0]) ||
            (!passwords.contains(piecesOfCredentials[1]) && !piecesOfCredentials[1].contains("PASS_TICKET")));
    }

    protected String[] getPiecesOfCredentials(Map<String, String> headers) {
        String authorization = headers.get(AUTHORIZATION_HEADER);
        if (authorization != null) {
            byte[] decoded = Base64.getDecoder().decode(authorization.replace("Basic ", ""));
            String credentials = new String(decoded);
            return credentials.split(":");
        }

        String cookie = headers.get(COOKIE_HEADER);
        if (cookie != null) {
            return cookie.split("=");
        }

        throw new IllegalArgumentException("Headers did not have cookie or authorization field");
    }

    protected boolean noLtpaCookie(Map<String, String> headers) {
        String cookie = headers.get(COOKIE_HEADER);
        return cookie == null || !cookie.contains(LTPA_TOKEN_NAME);
    }

    protected boolean noJwtCookie(Map<String, String> headers) {
        String cookie = headers.get(COOKIE_HEADER);
        return cookie == null || !cookie.contains(JWT_TOKEN_NAME);
    }

    protected void setLtpaToken(HttpServletResponse response) {
        Cookie ltpaToken = new Cookie(LTPA_TOKEN_NAME, "paMypL7yRO/IBroQtro21/uSC2LTrJvOuYebHaPc6JAUNWQ7lEHHt1l3CYeXa/nP6aKLFHTuyWy3qlRXvt10PjVdVl+7Q+wavgIsro7odz+PvTaJBp/+r0AH+DHYcdZikKe8dytGYZRH2c2gw8Gv3PliDIMd1iPEazY4HeYTU5VCFM5cBJkeIoTXCfL5ud9wTzrkY2c4h1PQPtx+hYCF4kEpiVkqIypVwjQLzWdJGV1Ihz7NqH/UU9MMJRXY1xMqsWZSibs2fX5MVK77dnyBrNYjVXA7PqYL6U/v5/1UCvuYQ/iEU9+Uy95J+xFEsnTX");

        ltpaToken.setSecure(true);
        ltpaToken.setHttpOnly(true);
        ltpaToken.setPath("/");

        response.addCookie(ltpaToken);
    }

    protected ResponseEntity<?> validJwtResponse(HttpServletResponse response, String username, String keystorePath) {
        Date current = new Date();
        final int HOUR = 3600000;
        Date expiration = new Date(current.getTime() + 8 * HOUR);

        String jwtToken = Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date())
            .setExpiration(expiration)
            .setIssuer("zOSMF")
            .setId(UUID.randomUUID().toString())
            .signWith(getKeyForSigning(keystorePath))
            .compact();

        // Build a valid JWT token
        Cookie jwtCookie = new Cookie(JWT_TOKEN_NAME, jwtToken);
        jwtCookie.setSecure(true);
        jwtCookie.setHttpOnly(true);
        jwtCookie.setPath("/");
        response.addCookie(jwtCookie);

        setLtpaToken(response);

        response.setContentType("application/json");
        return new ResponseEntity<>("{}", HttpStatus.OK);
    }

    private Key getKeyForSigning(String keystorePath) {
        try (FileInputStream keystore = new FileInputStream(new File(keystorePath))) {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            ks.load(
                keystore,
                "password".toCharArray()
            );
            return ks.getKey("localhost", "password".toCharArray());
        } catch (Exception ex) {
            throw new MockZosmfException(ex);
        }
    }
}
