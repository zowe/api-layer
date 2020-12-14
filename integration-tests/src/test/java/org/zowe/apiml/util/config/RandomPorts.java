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

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.zowe.apiml.util.config.RandomPort.available;

public class RandomPorts {
    public static List<Integer> generateUniquePorts(int size) {
        // Populate the array with random ports

        Integer[] result = new Integer[size];
        //     While the
        while (portsNotSatisfied(result)) {
            result = new Integer[size];
            generateCandidates(result);
        }

        return Arrays.asList(result);
    }

    public static void generateCandidates(Integer[] candidatePorts) {
        for ( int i = 0; i < candidatePorts.length; i++) {
            candidatePorts[i] = (new RandomPort()).getPort();
        }
    }

    public static boolean portsNotSatisfied(Integer[] ports) {
        return !portsAreDistinct(ports) || !portsAreAvailable(ports);
    }

    public static boolean portsAreAvailable(Integer[] ports) {
        for (Integer port : ports) {
            if (!available(port)) return false;
        }

        return true;
    }

    public static boolean portsAreDistinct(Integer[] ports) {
        Set<Integer> s = new HashSet<>(Arrays.asList(ports));
        return (s.size() == ports.length);
    }
}
