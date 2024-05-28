/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login.dummy;

import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Dummy service to provide user information
 */
@Component
@Qualifier("dummyService")
@ConditionalOnProperty(value = "apiml.security.auth.provider", havingValue = "dummy")
public class InMemoryUserDetailsService implements UserDetailsService {
    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public InMemoryUserDetailsService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    private char[] toCharArray(String i) {
        if (i == null) return new char[0];
        return i.toCharArray();
    }

    /**
     * Find user by username and return information
     *
     * @param username the username
     * @return user information
     */
    @Override
    public UserDetails loadUserByUsername(String username) {

        // Hard coding the users. All passwords must be encoded.
        final List<AppUser> users = Arrays.asList(
            new AppUser(1, "user", toCharArray(passwordEncoder.encode("user"))), // NOSONAR
            new AppUser(2, "expire", toCharArray(passwordEncoder.encode("expire"))) // NOSONAR
        );

        return users.stream()
            .filter(f -> f.getUsername().equals(username))
            .map(appUser ->
                // The "User" class is provided by Spring and represents a model class for user to be returned by UserDetailsService
                // And used by auth manager to verify and check user authentication.
                new User(appUser.getUsername(), new String(appUser.getPassword()), AuthorityUtils.NO_AUTHORITIES) // NOSONAR
            )
            .findFirst()
            .orElseThrow(() -> new UsernameNotFoundException("Username: " + username + " not found."));
    }

    // A class represent the user saved in the database.
    @Data
    @AllArgsConstructor
    private static class AppUser {
        private Integer id;
        private String username;
        private char[] password;

    }
}
