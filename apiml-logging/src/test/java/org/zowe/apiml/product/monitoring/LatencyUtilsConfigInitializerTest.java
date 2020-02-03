/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.monitoring;

import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class LatencyUtilsConfigInitializerTest {
    private static final String PROPERTY_KEY = "LatencyUtils.useActualTime";
    private final ConfigurableApplicationContext applicationContext = new GenericApplicationContext();

    @Test
    public void shouldSetSystemPropertyWhenPropertyNotSet() {
        System.getProperties().remove(PROPERTY_KEY);
        assertNull(System.getProperties().getProperty(PROPERTY_KEY));

        LatencyUtilsConfigInitializer latencyUtilsConfigInitializer = new LatencyUtilsConfigInitializer();
        latencyUtilsConfigInitializer.initialize(applicationContext);

        assertEquals("false", System.getProperties().getProperty(PROPERTY_KEY));
    }

    @Test
    public void shouldNotSetSystemPropertyWhenPropertyIsSetFromBefore() {
        System.getProperties().remove(PROPERTY_KEY);
        String value = "RandomValue";
        System.getProperties().setProperty(PROPERTY_KEY, value);
        assertEquals(value, System.getProperties().getProperty(PROPERTY_KEY));

        LatencyUtilsConfigInitializer latencyUtilsConfigInitializer = new LatencyUtilsConfigInitializer();
        latencyUtilsConfigInitializer.initialize(applicationContext);

        assertEquals(value, System.getProperties().getProperty(PROPERTY_KEY));
    }

}
