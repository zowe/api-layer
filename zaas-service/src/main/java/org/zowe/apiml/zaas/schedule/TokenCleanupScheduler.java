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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

@Component
@Slf4j
@RequiredArgsConstructor
public class TokenCleanupScheduler {

    private final AccessTokenProvider tokenProvider;

    @Scheduled(fixedRateString = "${apiml.security.tokenCleanup.intervalMs:300000}")
    public void cleanupExpiredTokens() {
        log.debug("Running scheduled token cleanup...");
        tokenProvider.evictNonRelevantTokensAndRules();
    }
}
