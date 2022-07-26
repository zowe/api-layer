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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Represents the change password body request in JSON format
 */
@Data
@NoArgsConstructor
public class ChangePasswordRequest {
    @JsonProperty("userID")
    private String username;
    @JsonProperty("oldPwd")
    private String password;
    @JsonProperty("newPwd")
    private String newPassword;

    public ChangePasswordRequest(LoginRequest loginRequest) {
        this.username = loginRequest.getUsername();
        this.password = loginRequest.getPassword();
        this.newPassword = loginRequest.getNewPassword();
    }
}
