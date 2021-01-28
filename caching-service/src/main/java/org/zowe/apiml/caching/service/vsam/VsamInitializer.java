/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service.vsam;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;

/**
 * This performs the warmup of VSAM during startup with retry
 * It has to be externalized from the VsamStorage because of @Retryable limitations
 * This is needed for service to start reliably when the VSAM file is being used
 * by another process at the moment.
 */
@Service
@Slf4j
public class VsamInitializer {

    @Retryable(value = UnsupportedOperationException.class, maxAttempts = 10)
    public void storageWarmup(VsamConfig config) {
        try (VsamFile file = new VsamFile(config, VsamConfig.VsamOptions.WRITE, true)) {
            log.info("Vsam file open successful");
        }
    }
}
