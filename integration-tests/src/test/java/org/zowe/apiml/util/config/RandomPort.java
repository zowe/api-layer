/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.util.config;

import lombok.Getter;

import java.util.Random;

/**
 * Ports that are open on internal Jenkins machine
 */
public class RandomPort {
    private final int lowerBoundary = 60000;
    private final int upperBoundary = 61000;

    @Getter
    private final int port;

    public RandomPort() {
        Random rand = new Random();
        port = lowerBoundary + rand.nextInt(upperBoundary - lowerBoundary);
    }
}
