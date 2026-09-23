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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * The caching service answers a read with {@code {"key":"salt","value":"..."}}, and ZAAS binds that
 * body into {@link CachingServiceClient.KeyValue} with the Jackson 3 mapper Spring Boot 4 builds for
 * the {@code RestTemplate}.
 *
 * <p>Jackson 3 stopped populating final fields by default ({@code ALLOW_FINAL_FIELDS_AS_MUTATORS}
 * changed from {@code true} to {@code false}), so a DTO that declares its fields final and relies on a
 * no-argument constructor deserialises to empty strings. The salt then looks absent, ZAAS writes the
 * empty value back and tries to create the key again - which the caching service rejects with
 * {@code ZWECS133E} (409) because it is already there.
 *
 * <p>The mapper used here is a plain {@code JsonMapper} with default features, so the test fails if the
 * DTO ever depends on that feature again.
 */
class CachingServiceClientKeyValueSerializationTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Nested
    class GivenACachingServiceResponse {

        @Test
        void whenDeserialized_thenKeyAndValueAreBound() {
            CachingServiceClient.KeyValue keyValue = mapper.readValue(
                "{\"key\":\"salt\",\"value\":\"WB9CMtMEqZBqSPHywcBB2g==\"}",
                CachingServiceClient.KeyValue.class
            );

            assertEquals("salt", keyValue.getKey());
            assertEquals("WB9CMtMEqZBqSPHywcBB2g==", keyValue.getValue());
        }

    }

}
