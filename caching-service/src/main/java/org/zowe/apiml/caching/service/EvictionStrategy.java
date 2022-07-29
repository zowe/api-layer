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

public interface EvictionStrategy {
    /**
     * This method is called when some item should be evicted. The strategy decides what to do with it.
     * The reject one could use this method to throw the StorageException.
     */
    void evict(String key);
}
