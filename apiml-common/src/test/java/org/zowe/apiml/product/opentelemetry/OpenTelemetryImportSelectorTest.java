/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenTelemetryImportSelectorTest {

    private OpenTelemetryImportSelector selector;

    @Mock
    private Environment environment;

    @BeforeEach
    void setUp() {
        selector = new OpenTelemetryImportSelector();
        selector.setEnvironment(environment);
    }

    @Test
    void whenOtelDisabled_thenDontImport() {
        when(environment.getProperty("otel.sdk.disabled", "true")).thenReturn("true");

        var imports = selector.selectImports(null);
        assertEquals(0, imports.length);
    }

    @Test
    void whenOtelEnabled_thenImport() {
        when(environment.getProperty("otel.sdk.disabled", "true")).thenReturn("false");

        var imports = selector.selectImports(null);
        assertEquals(1, imports.length);
        assertEquals("io.opentelemetry.instrumentation.spring.autoconfigure.OpenTelemetryAutoConfiguration", imports[0]);
    }

}
