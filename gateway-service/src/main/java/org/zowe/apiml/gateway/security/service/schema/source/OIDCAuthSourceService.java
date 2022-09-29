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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.AccessTokenProvider;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
@Service
public class OIDCAuthSourceService extends TokenAuthSourceService {
    @InjectApimlLogger
    protected final ApimlLogger logger = ApimlLogger.empty();

    private final AccessTokenProvider tokenProvider;

    @Override
    public boolean isValid(AuthSource authSource) {
        try {
            boolean validForScopes = tokenProvider.isValidForScopes(null, null);
            logger.log(MessageType.DEBUG, "OIDC Token is %s. ", validForScopes ? "valid" : "not valid");
            return validForScopes;
        } catch (Exception e) {
            logger.log(MessageType.ERROR, "OIDC Token is not valid due to the exception: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public AuthSource.Parsed parse(AuthSource authSource) {
        return null;
    }

    @Override
    public String getLtpaToken(AuthSource authSource) {
        return null;
    }

    @Override
    public String getJWT(AuthSource authSource) {
        return null;
    }

    @Override
    protected ApimlLogger getLogger() {
        return logger;
    }

    @Override
    public Function<String, AuthSource> getMapper() {
        return OIDCAuthSource::new;
    }

    @Override
    public Optional<String> getToken(RequestContext context) {
        return Optional.empty();
    }
}
