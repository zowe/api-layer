/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.schedule;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TokenCleanupSchedulerTest {

    @Mock
    private AccessTokenProvider tokenProvider;

    @InjectMocks
    private TokenCleanupScheduler tokenCleanupScheduler;

    @Test
    void whenCleanupExpiredTokensCalled_thenEvictNonRelevantTokensAndRulesIsCalled() {
        tokenCleanupScheduler.cleanupExpiredTokens();

        verify(tokenProvider, times(1)).evictNonRelevantTokensAndRules();
    }
}
