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

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.ServerSocket;
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

    /**
     * Checks to see if a specific port is available.
     *
     * @param port the port to check for availability
     */
    public static boolean available(int port) {
        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }
}
