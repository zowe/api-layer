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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.zowe.apiml.gateway.security.mapping.AuthenticationMapper;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.TokenCreationService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.QueryResponse;

import java.util.Optional;
import java.util.function.Function;

@RequiredArgsConstructor
@Slf4j
@Service
public class OIDCAuthSourceService extends TokenAuthSourceService {
    @InjectApimlLogger
    protected final ApimlLogger logger = ApimlLogger.empty();

    private final AuthenticationMapper mapper;
    private final AuthenticationService authenticationService;
    private final OIDCProvider oidcProvider;
    private final TokenCreationService tokenService;

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
        // should there be some specific cookie name/header name for the oidc token?
        return authenticationService.getJwtTokenFromRequest(context.getRequest());
    }

    @Override
    public boolean isValid(AuthSource authSource) {
        if (authSource instanceof OIDCAuthSource) {
            String token = ((OIDCAuthSource) authSource).getRawSource();
            logger.log(MessageType.DEBUG, "Validating OIDC token.");
            return token != null && oidcProvider.isValid(token);
        }
        return false;
    }

    @Override
    public AuthSource.Parsed parse(AuthSource authSource) {
        if (authSource instanceof OIDCAuthSource) {
            return isValid(authSource) ? parseOIDCToken((OIDCAuthSource) authSource, mapper) : null;
        }
        return null;
    }

    /**
     * Parse OIDC token
     *
     * @param oidcAuthSource{@link OIDCAuthSource} object which hold original source of authentication - OIDC token.
     * @param mapper     instance of {@link AuthenticationMapper} to use for parsing.
     * @return parsed authentication source.
     */
    private ParsedTokenAuthSource parseOIDCToken(OIDCAuthSource oidcAuthSource, AuthenticationMapper mapper) {
        String token = oidcAuthSource.getRawSource();

        // TODO get mapped user id
        String mappedUser = "";
//            String mappedUser = mapper.mapToMainframeUserId(OIDCAuthSource);
        // oidc is a jwt token, can be parsed using the method we already have?
        logger.log(MessageType.DEBUG, "Parsing OIDC token.");
        QueryResponse response = authenticationService.parseJwtWithSignature(token);

        AuthSource.Origin origin = AuthSource.Origin.valueByIssuer(response.getSource().name());
        return new ParsedTokenAuthSource(mappedUser, response.getCreation(), response.getExpiration(), origin);
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
