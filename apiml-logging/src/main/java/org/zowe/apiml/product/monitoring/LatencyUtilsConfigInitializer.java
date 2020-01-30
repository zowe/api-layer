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

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

import javax.annotation.Nonnull;

/**
 * This class initializes the system property that disables LatencyUtils thread that measures time by low level java api
 * This needs to be done before Spring context starts to initialize
 */
public class LatencyUtilsConfigInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    private static final String PROPERTY_KEY = "LatencyUtils.useActualTime";
    @Override
    public void initialize(@Nonnull ConfigurableApplicationContext applicationContext) {
        System.setProperty("org.apache.tomcat.util.buf.UDecoder.ALLOW_ENCODED_SLASH", "true");
        if (System.getProperties().getProperty(PROPERTY_KEY) == null) {
            System.getProperties().setProperty(PROPERTY_KEY, "false");
        }
    }
}
