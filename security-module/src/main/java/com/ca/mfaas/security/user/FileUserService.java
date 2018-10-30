/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.security.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

@Slf4j
public class FileUserService implements UserDetailsService {
    private final List<User> users;

    public FileUserService(String fileName) {
        this.users = readUsersFromFile(fileName);
    }

    List<User> readUsersFromFile(String fileName) {
        List<User> loadedUsers = Collections.emptyList();

        try (InputStream in = FileUserService.class.getResourceAsStream(fileName)) {
            ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
            loadedUsers = mapper.readValue(in, new TypeReference<List<User>>(){});
        } catch (IOException e) {
            log.debug("File '{}' can't be parsed as List<User>", fileName);
        }

        return loadedUsers;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User existingUser = users.stream()
            .filter(user -> user.getUsername().equals(username)).findFirst()
            .orElseThrow(() -> {
                log.debug("User '{}' not found", username);
                return new UsernameNotFoundException("Authentication Failed. Username or Password is not valid.");
            });

        FileUserDetails userDetails = new FileUserDetails(existingUser);

        return userDetails;
    }
}
