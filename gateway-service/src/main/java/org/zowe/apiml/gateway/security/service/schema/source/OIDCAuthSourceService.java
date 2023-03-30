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
import org.apache.commons.lang3.StringUtils;
import org.springframework.cache.annotation.Cacheable;
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
        // TODO we should store the OIDC (including the PAT) in the apiml cookie in next future
        return authenticationService.getOIDCTokenFromRequest(context.getRequest());
    }

    @Override
    @Cacheable(value = "validationOIDCToken", key = "#oidcToken", condition = "#oidcToken != null")
    public boolean isValid(AuthSource authSource) {
        if (authSource instanceof OIDCAuthSource) {
            String token = ((OIDCAuthSource) authSource).getRawSource();
            if (StringUtils.isNotBlank(token)) {
                QueryResponse tokenClaims = authenticationService.parseJwtToken(token);
                logger.log(MessageType.DEBUG, "Validating OIDC token.");
                if (oidcProvider.isValid(token, tokenClaims.getIssuer())) {
                    logger.log(MessageType.DEBUG, "OIDC token is valid, set the distributed id to the auth source.");
                    ((OIDCAuthSource) authSource).setDistributedId(tokenClaims.getUserId());
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    @Cacheable(value = "parseOIDCToken", key = "#parsedOIDCToken", condition = "#parsedOIDCToken != null")
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
    private AuthSource.Parsed parseOIDCToken(OIDCAuthSource oidcAuthSource, AuthenticationMapper mapper) {
        String token = oidcAuthSource.getRawSource();

        String mappedUser = mapper.mapToMainframeUserId(oidcAuthSource);
        logger.log(MessageType.DEBUG, "Parsing OIDC token.");
        QueryResponse response = authenticationService.parseJwtToken(token);

        AuthSource.Origin origin = AuthSource.Origin.valueByIssuer(response.getSource().name());
        return new ParsedTokenAuthSource(mappedUser, response.getCreation(), response.getExpiration(), origin);
    }

    //this method should be removed from the unrelated auth sources
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
        AuthSource.Parsed parsed = parse(authSource);
        return tokenService.createJwtTokenWithoutCredentials(parsed.getUserId());
    }

    public AuthSource.Origin getTokenOrigin(String zosmfToken) {
        QueryResponse response = authenticationService.parseJwtToken(zosmfToken);
        return AuthSource.Origin.valueByIssuer(response.getSource().name());
    }

}
