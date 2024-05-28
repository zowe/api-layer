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

import com.netflix.zuul.context.RequestContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.QueryResponse;

import javax.servlet.http.HttpServletRequest;
import java.util.Optional;
import java.util.function.Function;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

@RequiredArgsConstructor
@Slf4j
@Service
public class PATAuthSourceService extends TokenAuthSourceService {

    public static final String SERVICE_ID_HEADER = "X-Service-Id";

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
    public Optional<AuthSource> getAuthSourceFromRequest(HttpServletRequest request) {
        Optional<AuthSource> authSource = super.getAuthSourceFromRequest(request);

        if (authSource.isPresent()) {
            PATAuthSource patAuthSource = (PATAuthSource) authSource.get();
            String defaultServiceId = request.getHeader(SERVICE_ID_HEADER);
            patAuthSource.setDefaultServiceId(defaultServiceId);
        }

        return authSource;
    }

    @Override
    public Optional<String> getToken(HttpServletRequest request) {
        Optional<String> tokenOptional = authenticationService.getJwtTokenFromRequest(request);
        if (!tokenOptional.isPresent()) {
            // try to get token also from PAT specific cookie or header
            tokenOptional = authenticationService.getPATFromRequest(request);
        }
        if (tokenOptional.isPresent()) {
            AuthSource.Origin origin = authenticationService.getTokenOrigin(tokenOptional.get());
            if (AuthSource.Origin.ZOWE_PAT == origin) {
                return tokenOptional;
            }
        }
        return Optional.empty();
    }

    @Override
    public boolean isValid(AuthSource authSource) {
        try {
            String token = (String) authSource.getRawSource();
            RequestContext context = RequestContext.getCurrentContext();
            String serviceId = (String) context.get(SERVICE_ID_KEY);
            if (serviceId == null) {
                serviceId = ((PATAuthSource) authSource).getDefaultServiceId();
            }
            boolean validForScopes = tokenProvider.isValidForScopes(token, serviceId);
            logger.log(MessageType.DEBUG, "PAT is %s for scope: %s ", validForScopes ? "valid" : "not valid", serviceId);
            boolean invalidate = tokenProvider.isInvalidated(token);
            logger.log(MessageType.DEBUG, "PAT was %s", invalidate ? "invalidated" : "not invalidated");
            return validForScopes && !invalidate;
        } catch (Exception e) {
            logger.log(MessageType.ERROR, "PAT is not valid due to the exception: %s", e.getMessage());
            return false;
        }
    }

    @Override
    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof PATAuthSource) {
            String jwt = (String) authSource.getRawSource();
            QueryResponse response = authenticationService.parseJwtWithSignature(jwt);

            AuthSource.Origin origin = AuthSource.Origin.valueByTokenSource(response.getSource());
            return new ParsedTokenAuthSource(response.getUserId(), response.getCreation(), response.getExpiration(), origin);
        }
        return null;
    }

    @Override
    public String getLtpaToken(AuthSource authSource) {
        String zosmfToken = getJWT(authSource);
        AuthSource.Origin origin = authenticationService.getTokenOrigin(zosmfToken);
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

}
