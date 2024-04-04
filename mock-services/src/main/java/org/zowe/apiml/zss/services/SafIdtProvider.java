/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zss.services;

import io.jsonwebtoken.Jwts;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.zowe.apiml.zss.model.Authentication;
import org.zowe.apiml.zss.model.Token;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
public class SafIdtProvider {
    private final Map<String, String> providedTokens;

    public SafIdtProvider() {
        providedTokens = new HashMap<>();
    }

    public SafIdtProvider(Map<String, String> providedTokens) {
        this.providedTokens = providedTokens;
    }

    public Optional<Token> authenticate(
            Authentication authentication
    ) {
        String token = Jwts.builder()
                .setSubject(authentication.getUsername())
                .setExpiration(DateUtils.addMinutes(new Date(), 10))
                .compact(); //NOSONAR - No signature in mock allows easier verification. DO NOT COPY TO PRODUCTION!
        providedTokens.put(authentication.getUsername(), token);

        Token result = new Token(token);
        return Optional.of(result);
    }

    public boolean verify(
            Token token
    ) {
        String safToken = token.getJwt();
        String username = Jwts.parser()
            .unsecured()
                .build()
                .parseClaimsJwt(safToken)
                .getBody()
                .getSubject();

        return providedTokens.containsKey(username) && providedTokens.get(username).equals(safToken);
    }
}
