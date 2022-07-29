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

import org.springframework.security.authentication.AccountStatusUserDetailsChecker;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.AuthenticationUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsChecker;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.stereotype.Component;

/**
 * This authentication provider is to marking authentication via certificate as authentication.
 */
@Component
public class CertificateAuthenticationProvider implements AuthenticationProvider {

    private AuthenticationUserDetailsService<PreAuthenticatedAuthenticationToken> userDetailsService = new SimpleUserDetailService();
    private UserDetailsChecker userDetailsChecker = new AccountStatusUserDetailsChecker();

    @Override
    public Authentication authenticate(Authentication authentication) {
        UserDetails userDetails = this.userDetailsService
            .loadUserDetails((PreAuthenticatedAuthenticationToken) authentication);
        this.userDetailsChecker.check(userDetails);
        PreAuthenticatedAuthenticationToken result = new PreAuthenticatedAuthenticationToken(userDetails,
            authentication.getCredentials(), userDetails.getAuthorities());
        result.setDetails(authentication.getDetails());
        result.setAuthenticated(true);
        return result;
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return PreAuthenticatedAuthenticationToken.class.isAssignableFrom(authentication);

    }

}
