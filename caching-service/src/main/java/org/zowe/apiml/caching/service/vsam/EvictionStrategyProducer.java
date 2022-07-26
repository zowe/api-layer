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

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zowe.apiml.caching.config.GeneralConfig;
import org.zowe.apiml.caching.service.EvictionStrategy;
import org.zowe.apiml.caching.service.RejectStrategy;
import org.zowe.apiml.caching.service.Strategies;
import org.zowe.apiml.caching.service.vsam.config.VsamConfig;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.log.ApimlLogger;

@RequiredArgsConstructor
@Service
public class EvictionStrategyProducer {
    private final GeneralConfig generalConfig;
    private final VsamConfig vsamConfig;
    private final MessageService messageService;

    private RejectStrategy rejectStrategy;

    EvictionStrategy evictionStrategy(VsamFile vsamFile) {
        if (generalConfig.getEvictionStrategy().equals(Strategies.REJECT.getKey())) {
            if (rejectStrategy == null) {
                rejectStrategy = new RejectStrategy(ApimlLogger.of(RejectStrategy.class, messageService));
            }

            return rejectStrategy;
        } else {
            return new RemoveOldestStrategy(vsamConfig, vsamFile);
        }
    }
}
