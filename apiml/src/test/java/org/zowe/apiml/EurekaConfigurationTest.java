/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import jakarta.servlet.Filter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.Ordered;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
public class EurekaConfigurationTest {

    private EurekaConfiguration eurekaConfiguration;

    @BeforeEach
    void setUp() {
        this.eurekaConfiguration = new EurekaConfiguration();
    }

    @Test
    void traceFilterRegistration() {
        var filter = mock(Filter.class);
        var bean = eurekaConfiguration.traceFilterRegistration(filter);
        assertEquals(filter, bean.getFilter());
        assertEquals(Ordered.LOWEST_PRECEDENCE - 10, bean.getOrder());
    }



}
