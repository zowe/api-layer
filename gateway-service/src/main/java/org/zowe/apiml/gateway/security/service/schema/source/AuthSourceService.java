/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.service.schema.source;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;

/**
 * Interface represents main methods of service which gets the source of authentication and process it.
 */
public interface AuthSourceService {
    /**
     * Core method of the interface. Gets specific source of authentication from request and defines precedence
     * in case if more than one source is present.
     * @return AuthSource object which hold original source of authentication (JWT token, client certificate etc.)
     */
    Optional<AuthSource> getAuthSourceFromRequest(HttpServletRequest request);

    /**
     * Implements validation logic for specific source of authentication.
     * @param authSource AuthSource object which hold original source of authentication (JWT token, client certificate etc.)
     * @return true if authentication source is valid
     */
    boolean isValid(AuthSource authSource);

    /**
     * Parses the source of authentication and provides basic details like userId or expiration date.
     * @param authSource AuthSource object which hold original source of authentication (JWT token, client certificate etc.)
     * @return authentication source in parsed form
     */
    AuthSource.Parsed parse(AuthSource authSource);

    /**
     * Generates LTPA token from current source of authentication.
     * @param authSource AuthSource object which hold original source of authentication (JWT token, client certificate etc.)
     * @return LTPA token
     */
    String getLtpaToken(AuthSource authSource);

    String getJWT(AuthSource authSource);
}
