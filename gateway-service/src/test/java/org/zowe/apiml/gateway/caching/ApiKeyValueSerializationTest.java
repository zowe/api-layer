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

/**
 * The gateway binds caching service responses into {@link CachingServiceClient.ApiKeyValue} with the
 * Jackson 3 mapper Spring Boot 4 builds for its HTTP client. Jackson 3 no longer populates final fields
 * by default, so the DTO needs an explicit creator - otherwise every response deserialises to empty
 * strings and cached entries look missing.
 *
 * <p>The mapper used here has default features, so the test fails if the DTO depends on
 * {@code ALLOW_FINAL_FIELDS_AS_MUTATORS} being enabled.
 */
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

    }

}
