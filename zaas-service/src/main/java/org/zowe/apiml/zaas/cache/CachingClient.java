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

import java.util.Map;

public interface CachingClient {

    void create(CachingServiceClient.KeyValue kv);

    void appendList(String mapKey, CachingServiceClient.KeyValue kv);

    Map<String, Map<String, String>> readAllMaps();

    void evictTokens(String key);

    void evictRules(String key);

    CachingServiceClient.KeyValue read(String key);

    void update(CachingServiceClient.KeyValue kv);

    void delete(String key);
}
