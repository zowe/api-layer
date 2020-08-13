/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.controllers;

import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.login.LoginProvider;
import org.zowe.apiml.gateway.security.config.CompoundAuthProvider;


// These end points are activated only for dev profile.
@Profile("dev")
@AllArgsConstructor
@RestController
@RequestMapping(AuthProviderController.CONTROLLER_PATH)
public class AuthProviderController {

    public static final String CONTROLLER_PATH = "/api/v1/gateway/authentication";

    private CompoundAuthProvider compoundAuthProvider;

    @PostMapping()
    @ResponseBody
    public ResponseEntity<Object> updateAuthProviderConfig(@RequestBody AuthProvider provider) {
        LoginProvider loginProvider = LoginProvider.getLoginProvider(provider.getProvider());
        compoundAuthProvider.setLoginAuthProvider(loginProvider);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @GetMapping()
    @ResponseBody
    public AuthProvider getAuthProviderConfig() {
        AuthProvider authProvider = new AuthProvider();
        authProvider.setProvider(compoundAuthProvider.getLoginAuthProviderName());
        return authProvider;
    }

    private static class AuthProvider {
        public String provider;

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getProvider() {
            return provider;
        }
    }
}
