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

import com.netflix.zuul.context.RequestContext;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class TokenAuthSourceService implements AuthSourceService {

    protected abstract ApimlLogger getLogger();

    public abstract AuthenticationService getAuthenticationService();

    public abstract Predicate<QueryResponse.Source> getPredicate();

    public abstract Function<String, AuthSource> getMapper();

    /**
     * Core method of the interface. Gets source of authentication (JWT token) from request.
     * <p>
     *
     * @return Optional<AuthSource> which hold original source of authentication (JWT token)
     * or Optional.empty() when no authentication source found.
     */
    public Optional<AuthSource> getAuthSourceFromRequest() {
        final RequestContext context = RequestContext.getCurrentContext();
        getLogger().log(MessageType.DEBUG, "Getting JWT token from request.");
        Optional<String> jwtToken = getAuthenticationService().getJwtTokenFromRequest(context.getRequest());
        getLogger().log(MessageType.DEBUG, String.format("JWT token %s in request.", jwtToken.isPresent() ? "found" : "not found"));
        return verifyTokenForAuthSource(jwtToken);

    }

    public Optional<AuthSource> verifyTokenForAuthSource(Optional<String> token) {
        if (token.isPresent()) {
            try {
                QueryResponse response = getAuthenticationService().parseJwtToken(token.get());
                if (getPredicate().test(response.getSource())) {
                    return Optional.empty();
                }
            } catch (Exception e) {
                return Optional.empty();
            }
        }
        return token.map(getMapper());
    }
}
