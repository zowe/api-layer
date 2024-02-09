/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.core.convert.ConversionContext;
import io.micronaut.core.convert.TypeConverter;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.zowe.apiml.eurekaservice.client.config.Ssl;

import java.util.LinkedHashMap;
import java.util.Optional;

@Singleton
public class SslTypeConverter implements TypeConverter<LinkedHashMap<?, ?>, Ssl> {

    @Inject
    private ObjectMapper mapper;

    @Override
    public Optional<Ssl> convert(LinkedHashMap<?, ?> object, Class<Ssl> targetType, ConversionContext context) {
        return Optional.of(mapper.convertValue(object, Ssl.class));
    }

}
