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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ApiKeyValueSerializationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Nested
    class GivenACachingServiceResponse {

        @Test
        void whenDeserialized_thenKeyAndValueAreBound() {
            CachingServiceClient.ApiKeyValue keyValue = mapper.readValue(
                "{\"key\":\"apiml.lb.cache.key\",\"value\":\"{\\\"instanceId\\\":\\\"host:service:10010\\\"}\"}",
                CachingServiceClient.ApiKeyValue.class
            );

            assertEquals("apiml.lb.cache.key", keyValue.getKey());
            assertEquals("{\"instanceId\":\"host:service:10010\"}", keyValue.getValue());
        }

        @Test
        void whenCreatedWithoutArguments_thenKeyAndValueAreEmpty() {
            CachingServiceClient.ApiKeyValue keyValue = new CachingServiceClient.ApiKeyValue();

            assertEquals("", keyValue.getKey());
            assertEquals("", keyValue.getValue());
        }

    }

}
