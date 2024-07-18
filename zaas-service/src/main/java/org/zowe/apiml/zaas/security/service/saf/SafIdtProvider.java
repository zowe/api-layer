/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.saf;

/**
 * It's possible to configure various SafIdtProviders. At the moment only one configured at the time is allowed.
 * If there are multiple providers configured, the behavior can be unpredictable.
 */
public interface SafIdtProvider {

    /**
     * If the current user has the proper rights generate the SAF token on its behalf and return it back.
     *
     * @param username userId
     * @param password or passticket.
     * @param applId   of service requesting the token.
     * @return Either empty answer meaning the user is either unauthenticated or doesn't have the proper rights.
     */
    String generate(String username, char[] password, String applId);

    /**
     * Verify that the provided saf token is valid.
     *
     * @param safToken Token to validate.
     * @param applid   of service validating the token.
     * @return true if the token is valid, false if it is invalid
     */
    boolean verify(String safToken, String applid);

}
