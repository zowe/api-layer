/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.schema.source;

import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.function.Function;

public abstract class TokenAuthSourceService implements AuthSourceService {

    protected abstract ApimlLogger getLogger();

    public abstract Function<String, AuthSource> getMapper();

    public abstract Optional<String> getToken(HttpServletRequest request);

    /**
     * Core method of the interface. Gets source of authentication (JWT token) from request.
     * <p>
     *
     * @return Optional<AuthSource> which hold original source of authentication (JWT token)
     * or Optional.empty() when no authentication source found.
     */
    public Optional<AuthSource> getAuthSourceFromRequest(HttpServletRequest request) {
        getLogger().log(MessageType.DEBUG, "Getting JWT token from request.");
        Optional<String> authToken = getToken(request);
        getLogger().log(MessageType.DEBUG, String.format("JWT token %s in request.", authToken.isPresent() ? "found" : "not found"));
        return authToken.map(getMapper());

    }
}
