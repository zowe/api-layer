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

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.eurekaservice.client.config.Ssl;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class SslTypeConverterTest {

    private static final char[] PASSWORD = new char[]{ 'p', 'a', 's', 's', 'w', 'o', 'r', 'd' };

    @Inject
    private SslTypeConverter sslTypeConverter;

    @Test
    void givenTypeConverter_thenConvertCharArray() {
        LinkedHashMap<String, Object> sslMap = new LinkedHashMap<>();
        sslMap.put("keyPassword", "password");

        Optional<Ssl> ssl = sslTypeConverter.convert(sslMap, Ssl.class);
        assertTrue(ssl.isPresent());
        assertTrue(Arrays.equals(PASSWORD, ssl.get().getKeyPassword()));
    }
}
