/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.zowe.apiml.security.client.service.GatewaySecurity;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.service.AuthenticationService;

import java.util.Optional;

import static org.zowe.apiml.security.common.error.ErrorType.TOKEN_NOT_VALID;

@Service
@Primary
@RequiredArgsConstructor
public class GatewaySecurityApi implements GatewaySecurity {

    private final CompoundAuthProvider compoundAuthProvider;
    private final AuthenticationService authenticationService;
    @Nullable
    private final OIDCProvider oidcProvider;

    @Override
    public Optional<String> login(String username, char[] password, char[] newPassword) {
        if (StringUtils.isBlank(username) || ArrayUtils.isEmpty(password)) {
            throw new AuthenticationCredentialsNotFoundException("Username or password not provided.");
        }

        var loginRequest = new LoginRequest(username, password, newPassword);
        Authentication authentication = new UsernamePasswordAuthenticationToken(username, loginRequest);
        authentication = compoundAuthProvider.authenticate(authentication);
        if (authentication.isAuthenticated() ) {
            return Optional.ofNullable((String) authentication.getCredentials());
        }
        return Optional.empty();
    }

    @Override
    public QueryResponse query(String token) {
        var authentication = authenticationService.validateJwtToken(token);
        if (authentication.isAuthenticated()) {
            return authenticationService.parseJwtToken(token);
        }
        throw new TokenNotValidException(TOKEN_NOT_VALID.getDefaultMessage());
    }

    @Override
    public QueryResponse verifyOidc(String token) {
        if (oidcProvider != null && oidcProvider.isValid(token)) {
            return new QueryResponse();
        }
        throw new TokenNotValidException(TOKEN_NOT_VALID.getDefaultMessage());
    }

}
