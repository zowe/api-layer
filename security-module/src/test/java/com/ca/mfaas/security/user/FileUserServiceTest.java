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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Collections;
import java.util.List;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class FileUserServiceTest {
    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void testGettingExistingUserFromFile() {
        String existingFile = "/list-of-users.yml";
        String existingUsername = "someuser";
        String existingUserpassword = "somepassword";

        FileUserService userService = new FileUserService(existingFile);
        UserDetails userDetails = userService.loadUserByUsername(existingUsername);

        assertThat(userDetails.getPassword(), is(existingUserpassword));
    }

    @Test
    public void testGettingNotExistingUserFromFile() {
        String existingFile = "/list-of-users.yml";
        String notExistingUsername = "notExistingUser";
        exception.expect(UsernameNotFoundException.class);
        exception.expectMessage("Authentication Failed. Username or Password is not valid.");

        FileUserService userService = new FileUserService(existingFile);
        userService.loadUserByUsername(notExistingUsername);
    }

    @Test
    public void testLoadingNotExistingFile() {
        String notExistingFile = "/some-not-existing-file.yml";

        FileUserService userService = new FileUserService(notExistingFile);
        List<User> loadedUsers = userService.readUsersFromFile(notExistingFile);

        assertThat(loadedUsers, is(Collections.emptyList()));
    }

    @Test
    public void testLoadingNotValidFile() {
        String notValidUserFile = "/not-valid-list-of-users.yml";

        FileUserService userService = new FileUserService(notValidUserFile);
        List<User> loadedUsers = userService.readUsersFromFile(notValidUserFile);

        assertThat(loadedUsers, is(Collections.emptyList()));
    }
}
