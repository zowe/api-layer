/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.zowe.apiml.auth.Authentication;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;

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

    private final IAuthenticationScheme defaultScheme;
    private final Map<AuthenticationScheme, IAuthenticationScheme> map;

    public AuthenticationSchemeFactory(@Autowired List<IAuthenticationScheme> schemes) {
        map = new EnumMap<>(AuthenticationScheme.class);

        IAuthenticationScheme foundDefaultScheme = null;

        // map beans to map, checking duplicity and find exactly one default bean, otherwise throw exception
        for (final IAuthenticationScheme aas : schemes) {
            final IAuthenticationScheme prev = map.put(aas.getScheme(), aas);

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

    public IAuthenticationScheme getSchema(AuthenticationScheme scheme) {
        if (scheme == null) return defaultScheme;

        final IAuthenticationScheme output = map.get(scheme);
        if (output == null) {
            throw new IllegalArgumentException("Unknown scheme : " + scheme);
        }
        return output;
    }

    public AuthenticationCommand getAuthenticationCommand(Authentication authentication) {
        final IAuthenticationScheme scheme;
        if ((authentication == null) || (authentication.getScheme() == null)) {
            scheme = defaultScheme;
        } else {
            scheme = getSchema(authentication.getScheme());
        }
        final AuthSource authSource = scheme.getAuthSource().orElse(null);
        return scheme.createCommand(authentication, authSource);
    }

}
