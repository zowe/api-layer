/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.login.saf;

public interface PlatformUser {
    /**
     * Authenticates an user.
     *
     * If successful, a null object is returned. If not successful an instance of the PlatformReturned class is returned.
     */
    Object authenticate(java.lang.String userid, java.lang.String password);

    /**
     *If successful, a null object is returned. If NOT successful, an instance of the PlatformReturned class is returned
     * with the class variables errno, errno2 and errnoMsg set from the values returned by the OS/390 services __passwd,
     * strerror(errno), and __errno2().
     */
    Object changePassword(java.lang.String userid, java.lang.String password, java.lang.String newPassword);
}
