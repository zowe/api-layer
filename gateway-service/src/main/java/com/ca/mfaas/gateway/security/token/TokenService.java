/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.token;

import com.ca.mfaas.gateway.security.controller.exception.GatewayLoginRequestFormatException;
import com.ca.mfaas.gateway.security.service.DummyUserService;
import io.apiml.security.gateway.login.GatewayLoginRequest;
import io.apiml.security.gateway.login.GatewayLoginResponse;
import io.apiml.security.gateway.query.QueryResponse;
import io.jsonwebtoken.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.UUID;

@Service
@Slf4j
public class TokenService {
    private static final long TOKEN_VALID_IN_SECONDS = 3000;
    private static final String SECRET = "secret";
    private final DummyUserService userService;

    public TokenService(DummyUserService userService) {
        this.userService = userService;
    }

    public GatewayLoginResponse login(GatewayLoginRequest loginRequest) {
        validateLoginRequest(loginRequest);
        String username = loginRequest.getUsername();
        String password = loginRequest.getPassword();
        String existingUsername = userService.login(username, password);
        String token = createToken(existingUsername);
        return new GatewayLoginResponse(token);
    }

    private void validateLoginRequest(GatewayLoginRequest loginRequest) {
        log.debug("User with username: '{}' is trying to login to gateway", loginRequest.getUsername());
        if (StringUtils.isBlank(loginRequest.getUsername()) || StringUtils.isBlank(loginRequest.getPassword())) {
            throw new GatewayLoginRequestFormatException("Login object format is not valid");
        } else {
            return;
        }
    }

    public QueryResponse validate(String token) {
        try {
            Claims claims = Jwts.parser()
                .setSigningKey(SECRET)
                .parseClaimsJws(token)
                .getBody();

            long expiration = claims.getExpiration().getTime();
            long creation = claims.getIssuedAt().getTime();
            String username = claims.getSubject();
            String domain = claims.getIssuer();
            return new QueryResponse(username, domain, creation, expiration);
        } catch (ExpiredJwtException exception) {
            log.debug("Authentication: Token with id '{}' for user '{}' is expired", exception.getClaims().getId(), exception.getClaims().getSubject());
            throw new TokenExpireException("Token is expired");
        } catch (JwtException exception) {
            log.debug("Authentication: Token is not valid due to: {}", exception.getMessage());
            throw new TokenNotValidException("Token is not valid");
        } catch (Exception exception) {
            log.debug("Authentication: Token is not valid due to: {}", exception.getMessage());
            throw new TokenNotValidException("Token is not valid");
        }
    }

    public String createToken(String username) {
        long now = System.currentTimeMillis();
        long expiration = calculateExpiration(now);

        return Jwts.builder()
            .setSubject(username)
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer("gateway")
            .setId(UUID.randomUUID().toString())
            .signWith(SignatureAlgorithm.HS512, SECRET)
            .compact();
    }

    private long calculateExpiration(long now) {
        long expiration = now + (TOKEN_VALID_IN_SECONDS * 1000);
        return expiration;
    }
}
