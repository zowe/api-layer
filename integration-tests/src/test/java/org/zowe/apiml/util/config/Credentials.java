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

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Credentials {
    private String user;
    private String password;
    private Unauthorized unauthorized;

    public Credentials(String user, String password) {
        this.user = user;
        this.password = password;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Unauthorized {
        private String user;
        private String password;
    }

}
