/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package com.ca.mfaas.monitoring;

import org.junit.Test;

import static org.junit.Assert.*;

public class LatencyUtilsConfigInitializerTest {
    private final String PROPERTY_KEY = "LatencyUtils.useActualTime";

    @Test
    public void ShouldSetSystemPropertyWhenPropertyNotSet() {
        System.getProperties().remove(PROPERTY_KEY);
        assertNull( System.getProperties().getProperty(PROPERTY_KEY) );

        LatencyUtilsConfigInitializer latencyUtilsConfigInitializer = new LatencyUtilsConfigInitializer();
        latencyUtilsConfigInitializer.initialize(null);

        assertEquals(System.getProperties().getProperty(PROPERTY_KEY),"false");
    }

    @Test
    public void ShouldNotSetSystemPropertyWhenPropertyIsSetFromBefore() {
        System.getProperties().remove(PROPERTY_KEY);
        String value = "RandomValue";
        System.getProperties().setProperty(PROPERTY_KEY, value);
        assertEquals( System.getProperties().getProperty(PROPERTY_KEY), value );

        LatencyUtilsConfigInitializer latencyUtilsConfigInitializer = new LatencyUtilsConfigInitializer();
        latencyUtilsConfigInitializer.initialize(null);

        assertEquals( System.getProperties().getProperty(PROPERTY_KEY), value );
    }

}
