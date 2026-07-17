/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth.saf;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authorization.AuthorityAuthorizationDecision;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.authorization.ReactiveAuthorizationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.List;

@RequiredArgsConstructor
public class SafAuthorizationManager<T> implements ReactiveAuthorizationManager<T> {

    private final SafResourceAccessVerifying safResourceAccessVerifying;
    private final String safResourceClass;
    private final String safResourceName;
    private final String safResourceAccess;

    @Override
    public Mono<AuthorizationDecision> check(Mono<Authentication> authentication, T object) {
        // @formatter:off
        return authentication.filter(Authentication::isAuthenticated)
                .filter(auth -> safResourceAccessVerifying.hasSafResourceAccess(auth, safResourceClass, safResourceName, safResourceAccess))
                .map(auth -> ((AuthorizationDecision) new AuthorityAuthorizationDecision(true, List.of(new SimpleGrantedAuthority(String.format("%s.%s:%s", safResourceClass, safResourceName, safResourceAccess))))))
                .defaultIfEmpty(new AuthorityAuthorizationDecision(false, Collections.emptyList()));
        // @formatter:on
    }

}
