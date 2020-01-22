/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service.schema;

import com.ca.apiml.security.common.auth.Authentication;
import com.ca.apiml.security.common.auth.AuthenticationScheme;
import com.ca.apiml.security.common.token.QueryResponse;
import com.ca.mfaas.gateway.security.service.AuthenticationException;
import com.ca.mfaas.gateway.security.service.AuthenticationService;
import com.netflix.zuul.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * This method is responsible for servering beans which can create AuthenticationCommand. This bean collect all scheme's
 * beans (one for each defined scheme).
 * On start bean check if all scheme's beans are valid (exactly one default bean, duplicities - by scheme type)
 */
@Service
public class AuthenticationSchemeFactory {

    private final AbstractAuthenticationScheme defaultScheme;
    private final Map<AuthenticationScheme, AbstractAuthenticationScheme> map;

    private AuthenticationService authenticationService;

    public AuthenticationSchemeFactory(@Autowired AuthenticationService authenticationService, @Autowired List<AbstractAuthenticationScheme> schemes) {
        this.authenticationService = authenticationService;

        map = new EnumMap<>(AuthenticationScheme.class);

        AbstractAuthenticationScheme foundDefaultScheme = null;

        // map beans to map, checking duplicity and find exactly one default bean, otherwise throw exception
        for (final AbstractAuthenticationScheme aas : schemes) {
            final AbstractAuthenticationScheme prev = map.put(aas.getScheme(), aas);

            if (prev != null) {
                throw new IllegalArgumentException("Multiple beans for scheme " + aas.getScheme() +
                    " : " + prev.getClass() + " x " + aas.getClass());
            }

            if (aas.isDefault()) {
                if (foundDefaultScheme != null) {
                    throw new IllegalArgumentException("Multiple scheme's beans are marked as default : " +
                        foundDefaultScheme.getScheme() + " x " + aas.getScheme());
                }

                foundDefaultScheme = aas;
            }
        }

        if (foundDefaultScheme == null) {
            throw new IllegalArgumentException("No scheme marked as default");
        }

        this.defaultScheme = foundDefaultScheme;
    }

    public AbstractAuthenticationScheme getSchema(AuthenticationScheme scheme) {
        if (scheme == null) return defaultScheme;

        final AbstractAuthenticationScheme output = map.get(scheme);
        if (output == null) {
            throw new IllegalArgumentException("Unknown scheme : " + scheme);
        }
        return output;
    }

    public AuthenticationCommand getAuthenticationCommand(Authentication authentication) throws AuthenticationException {
        final AbstractAuthenticationScheme scheme;
        if ((authentication == null) || (authentication.getScheme() == null)) {
            scheme = defaultScheme;
        } else {
            scheme = getSchema(authentication.getScheme());
        }

        final HttpServletRequest request = RequestContext.getCurrentContext().getRequest();
        final QueryResponse jwtQr = authenticationService.getJwtTokenFromRequest(request)
            .map(x -> authenticationService.parseJwtToken(x))
            .orElse(null);

        return scheme.createCommand(authentication, jwtQr);
    }

}
