/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.security.service;

import com.ca.mfaas.gateway.security.controller.exception.IncorrectUsernameOrPasswordException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DummyUserService implements UserService {
    private final Map<String, String> users = new HashMap<>();

    public DummyUserService() {
        init();
    }

    public String login(String username, String password) {
        String storedPassword = users.get(username);
        if (storedPassword != null && storedPassword.equals(password)) {
            return username;
        } else {
            throw new IncorrectUsernameOrPasswordException("Username or password is incorrect");
        }
    }

    private void init() {
        users.put("apimtst", "password");
        users.put("user", "user");
        users.put("expire", "expire");
    }
}
