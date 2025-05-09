/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.*;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.handler.FailedAuthenticationHandler;
import org.zowe.apiml.security.common.login.LoginFilter;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.*;
import org.zowe.apiml.util.CookieUtil;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.zowe.apiml.constants.ApimlConstants.BEARER_AUTHENTICATION_PREFIX;
import static org.zowe.apiml.zaas.controllers.AuthController.INVALIDATE_PATH;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class LoginController {

    private final AuthConfigurationProperties authConfigurationProperties;



    /**
     * Endpoint to authenticate a user based on credentials from EITHER:
     * 1. HTTP Authorization header (Basic Auth)
     * 2. Request Body (JSON with username/password)
     * Sets a JWT in an HttpOnly cookie upon success.
     *
     * @param exchange The ServerWebExchange to access request headers, body, and response.
     * @return A Mono<ResponseEntity<Void>> indicating success or failure.
     */
    @PostMapping("/login")
    public Mono<ResponseEntity<Void>> login(ServerWebExchange exchange) {
      return   ReactiveSecurityContextHolder.getContext().flatMap(con ->{
            var authentication = con.getAuthentication();
            String jwt = ((TokenAuthentication) authentication).getCredentials();

            AuthConfigurationProperties.CookieProperties cp = authConfigurationProperties.getCookieProperties();

            // Create the HttpOnly cookie containing the JWT
            ResponseCookie jwtCookie = ResponseCookie.from(cp.getCookieName(), jwt)
                .path(cp.getCookiePath())
                .sameSite(cp.getCookieSameSite().getValue())
                .maxAge(cp.getCookieMaxAge() != null ? cp.getCookieMaxAge() : -1)
                .httpOnly(true)
                .secure(cp.isCookieSecure())
                .build();

            // Add the cookie to the response headers
            exchange.getResponse().addCookie(jwtCookie);
            log.debug("JWT Cookie set for user: {}", authentication.getName());

            // Return an OK response
            return Mono.just(ResponseEntity.ok().<Void>build());
        });
    }

//    @PostMapping(path = "/logout")
////    @Hidden
////    @Operation(summary = "Logout JWT token.",
////        tags = {"Security"},
////        operationId = "invalidateJwtToken",
////        description = "Use the `/auth/invalidate` API to invalidate token on specific instance of Gateway.",
////        security = {
////            @SecurityRequirement(name = "ClientCert")
////        })
////    @ApiResponses(value = {
////        @ApiResponse(responseCode = "200", description = "Successfully invalidated"),
////        @ApiResponse(responseCode = "400", description = "Invalid token"),
////        @ApiResponse(responseCode = "503", description = "Authentication service is not available")
////    })
//    public Mono<ResponseEntity<Void>> invalidateJwtToken(ServerWebExchange exchange) {
//        return getCookieValue(exchange, "apimlAuthenticationToken")
//            .switchIfEmpty(Mono.defer(() -> getBearerTokenFromHeaderReactive(exchange))).flatMap(token -> {
//                invalidateJwtToken(token, exchange);
//                return Mono.just(ResponseEntity.ok().<Void>build());
//            });
//    }
//
//    public Mono<String> getBearerTokenFromHeaderReactive(ServerWebExchange exchange) {
//        return Mono.justOrEmpty(exchange)
//            .map(ex -> ex.getRequest().getHeaders().getFirst(BEARER_AUTHENTICATION_PREFIX))
//            .filter(authHeader -> authHeader != null && authHeader.regionMatches(true, 0, BEARER_AUTHENTICATION_PREFIX, 0, BEARER_AUTHENTICATION_PREFIX.length()))
//            .map(authHeader -> authHeader.substring(BEARER_AUTHENTICATION_PREFIX.length()))
//            .filter(token -> !token.isBlank());
//    }
//
//
//
//
//
//
//    public Mono<String> getCookieValue(ServerWebExchange exchange, String cookieName) {
//        return Mono.justOrEmpty(exchange)
//                .map(ex -> ex.getRequest().getCookies().getFirst(cookieName))
//                .map(HttpCookie::getValue);
//    }




}
