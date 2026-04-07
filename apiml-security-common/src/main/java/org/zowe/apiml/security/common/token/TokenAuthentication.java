/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.token;

import com.nimbusds.jwt.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.zowe.apiml.security.common.util.JwtUtils;
import org.zowe.apiml.security.common.login.LoginFilter;

import java.io.Serial;
import java.text.ParseException;
import java.util.*;

/**
 * This object is added to security context after successful authentication.
 * Contains username and valid JWT token.
 */
@EqualsAndHashCode(callSuper = false, onlyExplicitlyIncluded = true)
@Slf4j
public class TokenAuthentication extends AbstractAuthenticationToken {

    @Serial
    //TODO: regenerate
    private static final long serialVersionUID = 82346593850419807L;

    private static final String DOMAIN_CLAIM_NAME = "dom";
    private static final String SCOPES = "scopes";


    private final JWT jwt;
    private final JWTClaimsSet claims;
    private final QueryResponse queryResponse;

    @Getter
    private Type type;

    public TokenAuthentication(String tokenString) {
        this(tokenString, (Type) null);
    }

    public TokenAuthentication(String userId, String tokenString) {
        this(tokenString);
        checkUserId(userId);
    }

    public TokenAuthentication(String tokenString, Type type) {
        super(Collections.emptyList());

        try {
            this.jwt = JWTParser.parse(tokenString);
            this.claims = jwt.getJWTClaimsSet();
            this.queryResponse = parseQueryResponse(claims);
            this.type = type;
        } catch (ParseException ex) {
            throw JwtUtils.handleJwtParserException(ex);
        }
    }

    public TokenAuthentication(String userId, String tokenString, Type type) {
        this(tokenString, type);
        checkUserId(userId);
    }


        public static TokenAuthentication createAuthenticated(String tokenString, Type type) {
        var tokenAuthentication = new TokenAuthentication(tokenString, type);
        tokenAuthentication.setAuthenticated(true);
        return tokenAuthentication;
    }

    public static TokenAuthentication createAuthenticated(String tokenString, String type) {
        return createAuthenticated(tokenString, Type.valueOf(type));
    }

    public static TokenAuthentication createAuthenticated(String userId, String token, Type type) {
        var tokenAuthentication = new TokenAuthentication(userId, token, type);
        tokenAuthentication.setAuthenticated(true);
        return tokenAuthentication;
    }

    public JWT getJwt() {
        return jwt;
    }

    public boolean isExpired() {
        return queryResponse.isExpired();
    }

    public Date getExpiration() {
        return queryResponse.getExpiration();
    }

    public QueryResponse.Source getSource() {
        return queryResponse.getSource();
    }

    public String getClaimAsString(String claimName) throws ParseException {
        return claims.getClaimAsString(claimName);
    }

    public QueryResponse getQueryResponse() {
        return queryResponse;
    }

//    public TokenAuthenticationEnhanced(String token, Type type) {
//        this(null, token, type);
//    }
//
//    public TokenAuthenticationEnhanced(String username, String token) {
//        this(username, token, (Type) null);
//    }
//
//    public TokenAuthenticationEnhanced(String username, String token, Type type) {
//        super(Collections.emptyList());
//        this.token = token;
//        this.type = type;
//    }

    /**
     * @return the token that prove the username is correct
     */
    @Override
    @EqualsAndHashCode.Include
    public String getCredentials() {
        return jwt.getParsedString();
    }

    /**
     * @return the username being authenticated
     */
    @Override
    public String getPrincipal() {
        return queryResponse.getUserId();
    }

//    /**
//     * Creates the TokenAuthentication with fulfilled username (principal), token and marked as authenticated.
//     * @param username Username, who is authenticated
//     * @param token Token, which authenticate the user
//     * @return TokenAuthentication marked as authenticated with username, token
//     */
//    public static TokenAuthenticationEnhanced createAuthenticated(String username, String token, Type type) {
//        final TokenAuthenticationEnhanced out = new TokenAuthenticationEnhanced(username, token, type);
//        out.setAuthenticated(true);
//        return out;
//    }

    @SuppressWarnings("squid:S3655")
    public static TokenAuthentication createAuthenticatedFromHeader(String token, String authHeader) {
        var loginRequest = LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(authHeader));
        return createAuthenticated(loginRequest.get().getUsername(), token, Type.JWT);
    }

    public enum Type {
        JWT,
        OIDC
    }

    private QueryResponse parseQueryResponse(JWTClaimsSet claims) {
        Object scopesObject = claims.getClaim(SCOPES);
        List<String> scopes = Collections.emptyList();
        if (scopesObject instanceof List<?>) {
            scopes = (List<String>) scopesObject;
        }
        try {
            return new QueryResponse(
                claims.getClaimAsString(DOMAIN_CLAIM_NAME),
                claims.getSubject(),
                claims.getIssueTime(),
                claims.getExpirationTime(),
                claims.getIssuer(),
                scopes,
                QueryResponse.Source.valueByIssuer(claims.getIssuer())
            );
        } catch (ParseException e) {
            throw new TokenNotValidException(e.getMessage(), e);
        }
    }

    private void checkUserId(String userId) {
        var principal = getPrincipal();
        if (userId == null || !userId.equalsIgnoreCase(principal)) {
            log.debug("Username '{}' does not match the one in token '{}' or is null", userId, principal);
            throw new TokenNotValidException("Token is not valid for provided username");
        }
    }
}
