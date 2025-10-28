/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.client.service;

import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Optional;

public interface GatewaySecurity {

    /**
     * Logs into the gateway with username and password, and retrieves valid JWT token
     *
     * @param username Username
     * @param password Password
     * @return Valid JWT token for the supplied credentials
     */
    Optional<String> login(String username, char[] password, char[] newPassword);

    /**
     * Verifies JWT token validity and returns JWT token data
     *
     * @param token JWT token to be validated
     * @return JWT token data as {@link QueryResponse}
     */
    QueryResponse query(String token);

    QueryResponse verifyOidc(String token);

}
