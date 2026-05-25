/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.auth;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.springframework.context.support.MessageSourceAccessor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.SpringSecurityMessageSource;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageService;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.Collections;

@Slf4j
public class BasicAuthenticationManager implements ReactiveAuthenticationManager {

    private final String userId;
    private final char[] password;
    private final String role;
    private final MessageSourceAccessor messages = SpringSecurityMessageSource.getAccessor();

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.of(BasicAuthenticationManager.class,
        new YamlMessageService("/security-common-log-messages.yml"));

    public BasicAuthenticationManager(String userId, char[] password, String role) {
        this.userId = userId;
        this.password = password;
        this.role = role;

        if (!isCredentialsSet()) {
            apimlLog.log("org.zowe.apiml.security.common.auth.missingDefaultCredentials");
        }
    }

    private boolean isCredentialsSet() {
        if (!StringUtils.isEmpty(userId) && !ArrayUtils.isEmpty(password)) {
            return true;
        }

        return false;
    }

    private char[] getPassword(Authentication authentication) {
        if (authentication.getCredentials() instanceof char[] password) {
            return password;
        }
        return String.valueOf(authentication.getCredentials()).toCharArray();
    }

    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        String username = authentication.getName();
        char[] password = getPassword(authentication);

        if (isCredentialsSet() && Strings.CS.equals(userId, username) &&
            Arrays.equals(this.password, password)) {
            // Return an authenticated token with a default role
            return Mono.just(new UsernamePasswordAuthenticationToken(username, password,
                Collections.singletonList(new SimpleGrantedAuthority(role))));
        }

        // Reject anything else
        log.warn("APIML credentials are not set. Please configure properties `apiml.service.http.userid` and `apiml.service.http.password`.");
        return Mono.error(new BadCredentialsException(this.messages
            .getMessage("AbstractUserDetailsAuthenticationProvider.badCredentials", "Bad credentials")));
    }
}
