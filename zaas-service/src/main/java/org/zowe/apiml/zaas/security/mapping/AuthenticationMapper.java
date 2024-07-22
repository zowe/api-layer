/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.mapping;

import org.zowe.apiml.zaas.security.service.schema.source.AuthSource;

/**
 * Interface for any authentication mapper which retrieves mainframe used id based on the authentication source
 */
public interface AuthenticationMapper {

    /**
     * Parse the information about the auth source and return mainframe user id if there is one associated with the provided
     * auth source.
     *
     * @param authSource The authentication source to map
     * @return Either valid user id or null.
     */
    String mapToMainframeUserId(AuthSource authSource);
}
