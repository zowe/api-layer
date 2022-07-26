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

import org.apache.commons.lang3.NotImplementedException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

import java.util.Set;

@Configuration
public class AccessTokenProviderConfig {

    public static final String NOT_IMPLEMENTED = "Function is not implemented.";

    @Bean
    @ConditionalOnMissingBean
    public AccessTokenProvider accessTokenProvider() {
        return new AccessTokenProvider() {
            @Override
            public void invalidateToken(String token) {
                throw new NotImplementedException(NOT_IMPLEMENTED);
            }

            @Override
            public boolean isInvalidated(String token) {
                throw new NotImplementedException(NOT_IMPLEMENTED);
            }

            @Override
            public String getToken(String username, int expirationTime, Set<String> scopes) {
                throw new NotImplementedException(NOT_IMPLEMENTED);
            }

            @Override
            public boolean isValidForScopes(String token, String serviceId) {
                throw new NotImplementedException(NOT_IMPLEMENTED);
            }

            @Override
            public void invalidateAllTokensForUser(String userId, long timestamp) {
                throw new NotImplementedException(NOT_IMPLEMENTED);
            }

            @Override
            public void invalidateAllTokensForService(String serviceId, long timestamp) {
                throw new NotImplementedException(NOT_IMPLEMENTED);
            }
        };
    }
}
