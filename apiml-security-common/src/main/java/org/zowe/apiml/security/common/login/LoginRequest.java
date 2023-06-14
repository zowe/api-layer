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

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.Authentication;

import java.util.Arrays;

import static org.zowe.apiml.security.SecurityUtils.readPassword;

/**
 * Represents the login JSON with credentials
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    private String username;
    private char[] password;
    private char[] newPassword;

    public LoginRequest(String username, char[] password) {
        this.username = username;
        this.password = password;
    }

    public static char[] getPassword(Authentication authentication) {
        if (authentication.getCredentials() instanceof LoginRequest) {
            LoginRequest loginRequest = (LoginRequest) authentication.getCredentials();
            return readPassword(loginRequest.getPassword());
        }

        return readPassword(authentication.getCredentials());
    }

    public static char[] getNewPassword(Authentication authentication) {
        if (authentication.getCredentials() instanceof LoginRequest) {
            LoginRequest loginRequest = (LoginRequest) authentication.getCredentials();
            return readPassword(loginRequest.getNewPassword());
        }

        return new char[0];
    }

    public void evictSensitiveData() {
        if (this.password != null) {
            Arrays.fill(this.password, (char) 0);
            this.password = null;
        }

        if (this.newPassword != null) {
            Arrays.fill(this.newPassword, (char) 0);
            this.newPassword = null;
        }
    }

}
