/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.caching;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

public interface CachingServiceClient {

    Mono<Void> create(ApiKeyValue keyValue);

    Mono<Void> update(ApiKeyValue keyValue);

    Mono<ApiKeyValue> read(String key);

    Mono<Void> delete(String key);

    /**
     * Data POJO that represents entry in caching service
     */
    @RequiredArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    @Data
    class ApiKeyValue {
        private final String key;
        private final String value;

        @JsonCreator
        public ApiKeyValue() {
            key = "";
            value = "";
        }
    }

}
