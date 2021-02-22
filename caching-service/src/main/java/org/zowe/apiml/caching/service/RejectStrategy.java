/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.caching.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;

@RequiredArgsConstructor
@Slf4j
public class RejectStrategy implements EvictionStrategy {
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();


    @Override
    public void evict(String key) {
        apimlLog.log("org.zowe.apiml.cache.insufficientStorage", key);

        throw new StorageException(Messages.INSUFFICIENT_STORAGE.getKey(), Messages.INSUFFICIENT_STORAGE.getStatus(), key);
    }
}
