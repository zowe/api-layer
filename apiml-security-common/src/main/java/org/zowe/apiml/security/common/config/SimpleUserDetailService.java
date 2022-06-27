/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.config;

import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

import java.util.Collections;

/**
 * User detail service that is usable in all x509 filter types
 * Assigns an authority to created user, which can be leveraged in method security expressions etc..
 */
public class SimpleUserDetailService implements AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken>, UserDetailsService {

    private UserDetails constructUserDetails(String username) {
        return new User(username, "", Collections.singletonList(new SimpleGrantedAuthority("TRUSTED_CERTIFICATE")));
    }

    @Override
    public UserDetails loadUserDetails(PreAuthenticatedAuthenticationToken token) throws UsernameNotFoundException {
        return constructUserDetails(token.getPrincipal().toString());
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return constructUserDetails(username);
    }
}
