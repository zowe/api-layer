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

import org.springframework.stereotype.Service;
import org.zowe.apiml.zss.model.Authentication;
import org.zowe.apiml.zss.model.Token;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class SafIdtProvider {
    private Map<String, String> providedTokens;

    public SafIdtProvider() {
        providedTokens = new HashMap<>();
    }

    public SafIdtProvider(Map<String, String> providedTokens) {
        this.providedTokens = providedTokens;
    }

    public Optional<Token> authenticate(
        Authentication authentication
    ) {
        String token = authentication.getUsername() + ";" + UUID.randomUUID();
        providedTokens.put(authentication.getUsername(), token);

        Token result = new Token();
        result.setJwt(token);
        return Optional.of(result);
    }

    public boolean verify(
        Token token
    ) {
        String safToken = token.getJwt();
        String username = safToken.split(";")[0];

        return providedTokens.containsKey(username) && providedTokens.get(username).equals(safToken);
    }
}
