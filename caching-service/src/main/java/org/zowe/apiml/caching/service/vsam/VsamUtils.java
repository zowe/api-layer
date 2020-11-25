/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.vsam;
import lombok.experimental.UtilityClass;

@UtilityClass
public class VsamUtils {

    /**
     * Pad a string with spaces to a specified length
     */
    static String padToLength(String s, int len) {
        StringBuilder sb = new StringBuilder(len);
        sb.append(s);
        for (int i = s.length(); i < len; i++) sb.append(' ');
        return sb.toString();
    }
}
