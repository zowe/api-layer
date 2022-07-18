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
import io.jsonwebtoken.security.SignatureException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Optional;
import java.util.function.Function;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@RequiredArgsConstructor
@Slf4j
@Service
public class PATAuthSourceService extends TokenAuthSourceService {

    @InjectApimlLogger
    protected final ApimlLogger logger = ApimlLogger.empty();

    private final AuthenticationService authenticationService;
    private final AccessTokenProvider tokenProvider;
    private final TokenCreationService tokenService;

    @Override
    protected ApimlLogger getLogger() {
        return logger;
    }

    @Override
    public Function<String, AuthSource> getMapper() {
        return PATAuthSource::new;
    }

    @Override
    public Optional<String> getToken(RequestContext context) {
        return authenticationService.getPATFromRequest(context.getRequest());
    }

    @Override
    public boolean isValid(AuthSource authSource) {
        String token = (String) authSource.getRawSource();
        RequestContext context = RequestContext.getCurrentContext();
        String serviceId = (String) context.get(SERVICE_ID_KEY);
        boolean validForScopes = tokenProvider.isValidForScopes(token, serviceId);
        logger.log(MessageType.DEBUG, "PAT is %s for scope: %s ", validForScopes ? "valid" : "not valid", serviceId);
        boolean invalidate = tokenProvider.isInvalidated(token);
        logger.log(MessageType.DEBUG, "PAT was %s", invalidate ? "invalidated" : "not invalidated");
        try {
            return validForScopes && !invalidate;
        } catch (SignatureException e) {
            return false;
        }
    }

    @Override
    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof PATAuthSource) {
            String jwt = (String) authSource.getRawSource();
            QueryResponse response = authenticationService.parseJwtWithSignature(jwt);

            AuthSource.Origin origin = AuthSource.Origin.valueByIssuer(response.getSource().name());
            return new ParsedTokenAuthSource(response.getUserId(), response.getCreation(), response.getExpiration(), origin);
        }
        return null;
    }

    @Override
    public String getLtpaToken(AuthSource authSource) {
        String zosmfToken = getJWT(authSource);
        AuthSource.Origin origin = getTokenOrigin(zosmfToken);
        if (AuthSource.Origin.ZOWE.equals(origin)) {
            zosmfToken = authenticationService.getLtpaToken(zosmfToken);
        }
        return zosmfToken;
    }

    @Override
    public String getJWT(AuthSource authSource) {
        ParsedTokenAuthSource parsed = (ParsedTokenAuthSource) parse(authSource);
        return tokenService.createJwtTokenWithoutCredentials(parsed.getUserId());
    }

    public AuthSource.Origin getTokenOrigin(String zosmfToken) {
        QueryResponse response = authenticationService.parseJwtToken(zosmfToken);
        return AuthSource.Origin.valueByIssuer(response.getSource().name());
    }
}
