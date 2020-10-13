/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.saf;

public class MockPlatformUser implements PlatformUser {
    public static final String VALID_USERID = "USER";
    public static final String VALID_PASSWORD = "validPassword";
    public static final String INVALID_USERID = "notuser";
    public static final String INVALID_PASSWORD = "notuser";

    @Override
    public PlatformReturned authenticate(String userid, String password) {
        if (userid.equalsIgnoreCase(VALID_USERID) && password.equalsIgnoreCase(VALID_PASSWORD)) {
            return null;
        }
        else {
            return PlatformReturned.builder().success(false).build();
        }
    }

}
