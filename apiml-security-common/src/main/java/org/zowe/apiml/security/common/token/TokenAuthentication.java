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
import org.apache.commons.lang3.StringUtils;
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
    private static final long serialVersionUID = 82346593850419807L;

    private static final String DOMAIN_CLAIM_NAME = "dom";
    private static final String SCOPES = "scopes";

    @Getter
    private final JWT jwt;
    private final JWTClaimsSet claims;
    @Getter
    private final QueryResponse queryResponse;
    @Getter
    private final Type type;

    public enum Type {
        JWT,
        OIDC
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

    public TokenAuthentication(String tokenString) {
        this(tokenString, Type.JWT);
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

    public static TokenAuthentication createAuthenticated(String userId, String token, Type type) {
        var tokenAuthentication = new TokenAuthentication(userId, token, type);
        tokenAuthentication.setAuthenticated(true);
        return tokenAuthentication;
    }

    @SuppressWarnings("squid:S3655")
    public static TokenAuthentication createAuthenticatedFromHeader(String token, String authHeader) {
        var loginRequest = LoginFilter.getCredentialFromAuthorizationHeader(Optional.of(authHeader));
        return createAuthenticated(loginRequest.get().getUsername(), token, Type.JWT);
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

    @Override
    public void setAuthenticated(boolean authenticated) {
        if (authenticated && isExpired()) {
            throw new TokenExpireException(
                "Unable to set authentication as true because the token ...%s expired on %s"
                    .formatted(StringUtils.right(jwt.getParsedString(), 15), getExpiration()));
        }
        super.setAuthenticated(authenticated);
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
