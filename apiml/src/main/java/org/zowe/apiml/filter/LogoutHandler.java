/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netflix.discovery.EurekaClient;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.cloud.netflix.eureka.CloudEurekaClient;
import org.springframework.http.HttpCookie;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.logout.ServerLogoutHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.token.TokenFormatNotValidException;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;

import static org.zowe.apiml.constants.ApimlConstants.BEARER_AUTHENTICATION_PREFIX;

@RequiredArgsConstructor
@Component
public class LogoutHandler implements ServerLogoutHandler {

    private final AuthenticationService authenticationService;
    private final FailedAuthenticationWebHandler failure;
    private static final String CONTENT_TYPE = MediaType.APPLICATION_JSON_VALUE;
    private final CloudEurekaClient eurekaClient;
    private final ReactiveDiscoveryClient discoveryClient;
    private final PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;

    private final ObjectMapper mapper;
    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();
    @Override
    public Mono<Void> logout(WebFilterExchange exchange, Authentication authentication) {
        return getTokenFromRequest(exchange.getExchange()).flatMap(token -> invalidateJwtToken(token, exchange));
    }

    public static Mono<String> getTokenFromRequest(ServerWebExchange exchange) {
       return getCookieValue(exchange, "apimlAuthenticationToken")
            .switchIfEmpty(Mono.defer(() -> getBearerTokenFromHeaderReactive(exchange)));
    }

    public static Mono<String> getCookieValue(ServerWebExchange exchange, String cookieName) {
        return Mono.justOrEmpty(exchange)
            .map(ex -> ex.getRequest().getCookies().getFirst(cookieName))
            .map(HttpCookie::getValue);
    }

    private Mono<Void> invalidateJwtToken(String token, WebFilterExchange exchange) {
        if (Boolean.TRUE.equals(authenticationService.isInvalidated(token))) {
           return failure.onAuthenticationFailure(exchange,new TokenNotValidException("The token you are trying to logout is not valid"));
        } else {
            try {
                var app = peerAwareInstanceRegistry.getApplications().getRegisteredApplications(CoreService.GATEWAY.getServiceId());
                authenticationService.invalidateJwtTokenGateway(token, true, app);
            } catch (TokenNotValidException e) {
                // TokenNotValidException thrown in cases where the format is not valid
               return failure.onAuthenticationFailure(exchange,new TokenFormatNotValidException(e.getMessage()));
            } catch (AuthenticationException e) {
                return failure.onAuthenticationFailure(exchange, e);
            } catch (Exception e) {
                // Catch any issue like ServiceNotAccessibleException, throw TokenNotValidException
                // so a 401 is returned. Returning 500 gives information about the system and is thus avoided.
               return failure.onAuthenticationFailure(exchange, new TokenNotValidException("Error while logging out token"));
            }
            return Mono.empty();
        }
    }

    public static Mono<String> getBearerTokenFromHeaderReactive(ServerWebExchange exchange) {
        return Mono.justOrEmpty(exchange)
            .map(ex -> ex.getRequest().getHeaders().getFirst(BEARER_AUTHENTICATION_PREFIX))
            .filter(authHeader -> authHeader != null && authHeader.regionMatches(true, 0, BEARER_AUTHENTICATION_PREFIX, 0, BEARER_AUTHENTICATION_PREFIX.length()))
            .map(authHeader -> authHeader.substring(BEARER_AUTHENTICATION_PREFIX.length()))
            .filter(token -> !token.isBlank());
    }

}
