/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

/**
 * This interface is implemented by beans where is using cache for services. If set of services will be
 * changed in discovery service, it will call those methods to evict caches. It is necessary to avoid
 * using old data after a change.
 */
public interface ServiceCacheEvict {

    /**
     * Evict all cached data for specific serviceId
     * @param serviceId Id of service with a change (to evict)
     */
    public void evictCacheService(String serviceId);

    /**
     * Evict all entries in caches about services and its instances
     */
    public void evictCacheAllService();

}
