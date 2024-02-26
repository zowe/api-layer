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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Class is supposed to hold a value string with username,password pairs separated by ;
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuxiliaryUserList {
    private String value;

    public List<Credentials> getCredentials() {
        return Arrays.stream(value.split(";"))
            .map(s -> new Credentials(
                s.split(",")[0],
                s.split(",")[1],
                s.split(",")[2],
                null)
            )
            .collect(Collectors.toList());
    }

    public List<Credentials> getCredentials(String key) {
        if (key == null || key.isEmpty()) {
            throw new IllegalArgumentException("Key must not be null or empty");
        }
        return Arrays.stream(value.split(";"))
            .map(s -> new Credentials(
                s.split(",")[0],
                s.split(",")[1],
                s.split(",")[2],
                null)
            )
            .filter(credentials -> key.equals(credentials.getKey()))
            .collect(Collectors.toList());
    }
}
