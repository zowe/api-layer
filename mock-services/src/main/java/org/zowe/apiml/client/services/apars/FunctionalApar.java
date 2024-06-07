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

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.client.model.LoginBody;
import org.zowe.apiml.client.services.JwtTokenService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"squid:S1452", "squid:S1172"})
public class FunctionalApar implements Apar {
    private static final String COOKIE_HEADER = "cookie";
    private static final String JWT_TOKEN_NAME = "jwtToken";
    private static final String LTPA_TOKEN_NAME = "LtpaToken2";

    protected static final String AUTHORIZATION_HEADER = "authorization";

    private final List<String> usernames;
    protected List<String> passwords;
    private JwtTokenService jwtTokenService;

    protected FunctionalApar(List<String> usernames, List<String> passwords) {
        this(usernames, passwords, new JwtTokenService(60));
    }

    protected FunctionalApar(List<String> usernames, List<String> passwords, JwtTokenService tokenService) {
        this.usernames = usernames;
        this.passwords = passwords;
        this.jwtTokenService = tokenService;
    }

    @Override
    public Optional<ResponseEntity<?>> apply(Object... parameters) {
        String calledService = (String) parameters[0];
        String calledMethod = (String) parameters[1];
        Optional<ResponseEntity<?>> originalResult = (Optional<ResponseEntity<?>>) parameters[2];
        HttpServletResponse response = (HttpServletResponse) parameters[3];
        Map<String, String> headers = (Map<String, String>) parameters[4];
        ResponseEntity<?> result = null;
        String token = jwtTokenService.extractToken(headers);
        String ltpaToken = jwtTokenService.extractLtpaToken(headers);
        if (calledService.equals("authentication")) {
            switch (calledMethod) {
                case "create":
                    result = handleAuthenticationCreate(headers, response);
                    break;
                case "verify":
                    result = handleAuthenticationVerify(headers, response);
                    break;
                case "update":
                    if (parameters.length > 4) {
                        LoginBody body = (LoginBody) parameters[5];
                        result = handleChangePassword(body);
                    }
                    break;
                case "delete":
                    result = handleAuthenticationDelete(headers);
                    if (ltpaToken != null) {
                        jwtTokenService.invalidateJwtToken(ltpaToken);
                    }
                    jwtTokenService.invalidateJwtToken(token);
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
        return null;
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
     * Override to provide a response entity when the update method for the authentication service is called
     * with proper authorization.
     */
    protected ResponseEntity<?> handleChangePassword(LoginBody body) {
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
        String basicAuth = getAuthorizationHeader(headers);
        String cookie = getAuthCookie(headers);
        return (basicAuth == null || basicAuth.isEmpty()) && (cookie == null || cookie.isEmpty());
    }

    protected boolean containsInvalidOrNoUser(Map<String, String> headers) {
        String authorization = getAuthorizationHeader(headers);
        if (authorization == null || authorization.isEmpty()) {
            return true;
        }

        String[] piecesOfCredentials = getPiecesOfCredentials(headers);
        return piecesOfCredentials.length <= 0 || (!usernames.contains(piecesOfCredentials[0]) ||
            (!passwords.contains(piecesOfCredentials[1]) && !piecesOfCredentials[1].contains("PASS_TICKET")));
    }

    private String getAuthorizationHeader(Map<String, String> headers) {
        return headers.get(AUTHORIZATION_HEADER) != null ? headers.get(AUTHORIZATION_HEADER) : headers.get(HttpHeaders.AUTHORIZATION);
    }

    protected String[] getPiecesOfCredentials(Map<String, String> headers) {
        String authorization = getAuthorizationHeader(headers);
        if (authorization != null) {
            byte[] decoded = Base64.getDecoder().decode(authorization.replace("Basic ", ""));
            String credentials = new String(decoded);
            return credentials.split(":");
        }

        String cookie = getAuthCookie(headers);
        if (cookie != null) {
            return cookie.split("=");
        }

        throw new IllegalArgumentException("Headers did not have cookie or authorization field");
    }

    protected boolean ltpaIsPresent(Map<String, String> headers) {
        String cookie = getAuthCookie(headers);
        return cookie != null && cookie.contains(LTPA_TOKEN_NAME);
    }

    protected boolean validLtpaCookie(Map<String, String> headers) {
        if (!ltpaIsPresent(headers)) {
            return false;
        }
        String token = jwtTokenService.extractLtpaToken(headers);
        return !jwtTokenService.containsToken(token);
    }

    protected boolean isValidJwtCookie(Map<String, String> headers) {
        String cookie = getAuthCookie(headers);
        if (cookie == null || !cookie.contains(JWT_TOKEN_NAME)) {
            return false;
        }
        String jwtToken = jwtTokenService.extractToken(headers);
        return jwtTokenService.validateJwtToken(jwtToken);

    }

    private String getAuthCookie(Map<String, String> headers) {
        return headers.get(COOKIE_HEADER) != null ? headers.get(COOKIE_HEADER) : headers.get(HttpHeaders.COOKIE);
    }

    protected void setLtpaToken(HttpServletResponse response) {
        Cookie ltpaToken = new Cookie(LTPA_TOKEN_NAME, "paMypL7yRO/IBroQtro21/uSC2LTrJvOuYebHaPc6JAUNWQ7lEHHt1l3CYeXa/nP6aKLFHTuyWy3qlRXvt10PjVdVl+7Q+wavgIsro7odz+PvTaJBp/+r0AH+DHYcdZikKe8dytGYZRH2c2gw8Gv3PliDIMd1iPEazY4HeYTU5VCFM5cBJkeIoTXCfL5ud9wTzrkY2c4h1PQPtx+hYCF4kEpiVkqIypVwjQLzWdJGV1Ihz7NqH/UU9MMJRXY1xMqsWZSibs2fX5MVK77dnyBrNYjVXA7PqYL6U/v5/1UCvuYQ/iEU9+Uy95J+xFEsnTX");

        ltpaToken.setSecure(true);
        ltpaToken.setHttpOnly(true);
        ltpaToken.setPath("/");

        response.addCookie(ltpaToken);
    }

    protected ResponseEntity<?> validJwtResponse(HttpServletResponse response, String username, String keystorePath) {
        String jwtToken;
        try {
            jwtToken = jwtTokenService.generateJwt(username);
        } catch (Exception e) {
            return new ResponseEntity<>("Not able to generate jwt. Message: " + e.getMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }


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

}
