/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.login;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;

/**
 * Represents the login JSON with credentials
 */
@Data
@NoArgsConstructor
public class LoginRequest {
    private String username;
    private String password;
    private String newPassword;

    public LoginRequest(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public LoginRequest(String username, String password, String newPassword) {
        this.username = username;
        this.password = password;
        this.newPassword = newPassword;
    }

    public static String getPassword(Authentication authentication) {
        String password;
        if (authentication.getCredentials() instanceof LoginRequest) {
            LoginRequest loginRequest = (LoginRequest) authentication.getCredentials();
            password = loginRequest.getPassword();
        } else {
            password = (String) authentication.getCredentials();
        }
        return password;
    }

    public static String getNewPassword(Authentication authentication) {
        String password;
        if (authentication.getCredentials() instanceof LoginRequest) {
            LoginRequest loginRequest = (LoginRequest) authentication.getCredentials();
            password = loginRequest.getNewPassword();
        } else {
            password = null;
        }
        return password;
    }
}
