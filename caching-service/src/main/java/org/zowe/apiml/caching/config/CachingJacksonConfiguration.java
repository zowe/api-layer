/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import org.springframework.boot.jackson.autoconfigure.JsonMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.MapperFeature;

/**
 * Restores the Jackson 2 deserialization semantics that the caching service's request model relies on.
 * <p>
 * Spring Boot 4 builds the web message converters from a Jackson 3 {@code JsonMapper}, and Jackson 3
 * changed the default of {@link MapperFeature#ALLOW_FINAL_FIELDS_AS_MUTATORS} from {@code true} to
 * {@code false}. Jackson 2 therefore used to populate final fields during deserialization, and Jackson 3
 * no longer does.
 * <p>
 * That matters for {@code org.zowe.apiml.caching.model.KeyValue}, which declares {@code key},
 * {@code value} and {@code created} as final and has a {@code @JsonCreator} no-argument constructor that
 * initialises them to empty strings. With Jackson 3 the request body is bound to nothing: the fields keep
 * the constructor defaults, so
 * <pre>{"key":"first-key","value":"anyValue"}</pre>
 * is stored as an entry with an empty key and an empty value, and reads return
 * <pre>{"":{"created":"..."}}</pre>
 * The endpoint still answers {@code 201}, so the corruption is silent.
 * <p>
 * Enabling the feature brings behaviour back in line with the pre-migration release, where the same
 * request produced {@code {"first-key":{"key":"first-key","value":"anyValue",...}}}. It is applied to the
 * Jackson 3 builder because that is what the HTTP converters use.
 */
@Configuration(proxyBeanMethods = false)
public class CachingJacksonConfiguration {

    /**
     * Allows Jackson to treat final fields as mutators, as Jackson 2 did before this upgrade.
     *
     * @return the customizer applied to the Jackson 3 mapper builder
     */
    @Bean
    JsonMapperBuilderCustomizer allowFinalFieldsAsMutators() {
        return jsonMapperBuilder -> jsonMapperBuilder
            .enable(MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS);
    }

}
