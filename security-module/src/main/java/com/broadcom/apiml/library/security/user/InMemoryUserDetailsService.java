/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.security.user;

import org.springframework.beans.factory.annotation.Autowired;
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
@Deprecated // Delete once z/osmf authentication implemented
@Component
public class InMemoryUserDetailsService implements UserDetailsService {

    private final BCryptPasswordEncoder passwordEncoder;

    @Autowired
    public InMemoryUserDetailsService(BCryptPasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        // hard coding the users. All passwords must be encoded.
        final List<AppUser> users = Arrays.asList(
            new AppUser(1, "user", passwordEncoder.encode("user"), null),
            new AppUser(2, "expire", passwordEncoder.encode("expire"), null)
        );


        for (AppUser appUser : users) {
            if (appUser.getUsername().equals(username)) {

                // The "User" class is provided by Spring and represents a model class for user to be returned by UserDetailsService
                // And used by auth manager to verify and check user authentication.
                return new User(appUser.getUsername(), appUser.getPassword(), AuthorityUtils.NO_AUTHORITIES);
            }
        }

        // If user not found. Throw this exception.
        throw new UsernameNotFoundException("Username: " + username + " not found");
    }

    // A (temporary) class represent the user saved in the database.
    private static class AppUser {
        private Integer id;
        private String username, password;
        private String role;

        AppUser(Integer id, String username, String password, String role) {
            this.id = id;
            this.username = username;
            this.password = password;
            this.role = role;
        }

        public Integer getId() {
            return this.id;
        }

        public void setId(Integer id) {
            this.id = id;
        }

        public String getUsername() {
            return this.username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return this.password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getRole() {
            return this.role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public boolean equals(final Object o) {
            if (o == this) return true;
            if (!(o instanceof AppUser)) return false;
            final AppUser other = (AppUser) o;
            if (!other.canEqual((Object) this)) return false;
            final Object this$id = this.getId();
            final Object other$id = other.getId();
            if (this$id == null ? other$id != null : !this$id.equals(other$id)) return false;
            final Object this$username = this.getUsername();
            final Object other$username = other.getUsername();
            if (this$username == null ? other$username != null : !this$username.equals(other$username)) return false;
            final Object this$password = this.getPassword();
            final Object other$password = other.getPassword();
            if (this$password == null ? other$password != null : !this$password.equals(other$password)) return false;
            final Object this$role = this.getRole();
            final Object other$role = other.getRole();
            if (this$role == null ? other$role != null : !this$role.equals(other$role)) return false;
            return true;
        }

        protected boolean canEqual(final Object other) {
            return other instanceof AppUser;
        }

        public int hashCode() {
            final int PRIME = 59;
            int result = 1;
            final Object $id = this.getId();
            result = result * PRIME + ($id == null ? 43 : $id.hashCode());
            final Object $username = this.getUsername();
            result = result * PRIME + ($username == null ? 43 : $username.hashCode());
            final Object $password = this.getPassword();
            result = result * PRIME + ($password == null ? 43 : $password.hashCode());
            final Object $role = this.getRole();
            result = result * PRIME + ($role == null ? 43 : $role.hashCode());
            return result;
        }

        public String toString() {
            return "InMemoryUserDetailsService.AppUser(id=" + this.getId() + ", username=" + this.getUsername() + ", password=" + this.getPassword() + ", role=" + this.getRole() + ")";
        }
    }
}
