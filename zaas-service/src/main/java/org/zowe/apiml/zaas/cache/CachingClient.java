/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.cache;

import java.util.Collection;
import java.util.Map;

public interface CachingClient {

    void create(CachingServiceClient.KeyValue kv);

    void appendList(String mapKey, CachingServiceClient.KeyValue kv);

    /**
     * Reads every map in full.
     *
     * @deprecated on this release's caching service this returns the per-item layout, and it is only still
     *     reachable as the fallback for a caching service that predates {@link #getMapItems(Map)}.
     *     Use {@link #getMapItems(Map)} for anything on the request path.
     */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    Map<String, Map<String, String>> readAllMaps();

    /**
     * Looks up only the given item keys, grouped by map key, and returns only the entries that exist.
     * <p>
     * This is the personal access token validation read: a handful of point lookups instead of a download of
     * the whole revocation store. A missing entry means "not revoked"; anything that goes wrong throws, so
     * the caller can fail closed.
     */
    Map<String, Map<String, String>> getMapItems(Map<String, Collection<String>> keysByMapKey);

    /**
     * Reads the pre-cutover, whole-map revocation layout, which this release never writes to.
     *
     * @deprecated consulted only for tokens issued before the cutover; removed once they have all expired.
     */
    @Deprecated(since = "3.6.0") // scheduled for removal with the legacy read path
    Map<String, Map<String, String>> readAllLegacyMaps();

    /**
     * Whether the caching service on the other end understands {@link #getMapItems(Map)}.
     * <p>
     * Zowe components are installed individually, so ZAAS on this release can be pointed at a caching service
     * that predates the endpoint. Rejecting every personal access token in that case would be a fleet-wide
     * outage diagnosable only from a stack trace, so the answer here routes to the slower legacy read instead.
     */
    boolean supportsMapItemQuery();

    void evictTokens(String key);

    void evictRules(String key);

    CachingServiceClient.KeyValue read(String key);

    void update(CachingServiceClient.KeyValue kv);

    void delete(String key);
}
