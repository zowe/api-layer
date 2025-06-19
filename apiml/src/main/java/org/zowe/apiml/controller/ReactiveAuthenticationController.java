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

import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.ReactiveAuthenticationManagerAdapter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.login.LoginRequest;
import org.zowe.apiml.security.common.token.QueryResponse;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.config.CompoundAuthProvider;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.zowe.apiml.zaas.controllers.AuthController.INVALIDATE_PATH;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactiveAuthenticationController {

    private final AuthenticationService authenticationService;
    private final PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;
    private final HttpUtils httpUtils;
    private final CompoundAuthProvider compoundAuthProvider;

    /**
     * Endpoint to authenticate a user based on credentials from EITHER:
     * 1. HTTP Authorization header (Basic Auth), coming from filter already authenticated
     * 2. Request Body (JSON with username/password) processed by the controller
     * Sets a JWT in an HttpOnly cookie upon success.
     *
     * @param exchange The ServerWebExchange to access request headers, body, and response.
     * @return A Mono<ResponseEntity<Void>> indicating success or failure.
     */
    @PostMapping(value = "/login", consumes = MediaType.ALL_VALUE)
    public Mono<ResponseEntity<Object>> login(ServerWebExchange exchange, @RequestBody(required = false) LoginRequest request) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .filter(Authentication::isAuthenticated)
            .map(authentication -> replyWithJwt(exchange, authentication))
            .switchIfEmpty(Mono.<ResponseEntity<Object>>defer(() -> this.authWithBody(exchange, request)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(401)).build()));
    }

    private Mono<ResponseEntity<Object>> authWithBody(ServerWebExchange exchange, LoginRequest request) {
        if (request == null || StringUtils.isBlank(request.getUsername()) || request.getPassword() == null || request.getPassword().length == 0) {
            throw new AuthenticationCredentialsNotFoundException("Login object has wrong format.");
        }
        var providerManager = new ProviderManager(compoundAuthProvider);
        var authAdapter = new ReactiveAuthenticationManagerAdapter(providerManager);
        return authAdapter.authenticate(new UsernamePasswordAuthenticationToken(request.getUsername(), request))
            .map(authentication -> replyWithJwt(exchange, authentication));
    }

    private ResponseEntity<Object> replyWithJwt(ServerWebExchange exchange, Authentication authentication) {
        var jwt = ((TokenAuthentication) authentication).getCredentials();
        var jwtCookie = httpUtils.createResponseCookie(jwt);

        exchange.getResponse().addCookie(jwtCookie);
        log.debug("JWT Cookie set for user: {}", authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    public Mono<ResponseEntity<QueryResponse>> query() {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .filter(Authentication::isAuthenticated)
            .filter(TokenAuthentication.class::isInstance)
            .map(TokenAuthentication.class::cast)
            .map(tokenAuthentication -> ResponseEntity
                    .ok()
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(authenticationService.parseJwtToken(tokenAuthentication.getCredentials()))
            )
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(401)).build()));
    }

    @DeleteMapping(path = INVALIDATE_PATH)
    @Operation(summary = "Logout JWT token.",
        tags = {"Security"},
        operationId = "invalidateJwtToken",
        description = "Use the `/auth/invalidate` API to invalidate token on specific instance of Gateway.",
        security = {
            @SecurityRequirement(name = "ClientCert")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully invalidated"),
        @ApiResponse(responseCode = "400", description = "Invalid token"),
        @ApiResponse(responseCode = "503", description = "Authentication service is not available")
    })
    public Mono<ResponseEntity<Void>> invalidateJwtToken(ServerWebExchange exchange) {
        var endpoint = "/auth/invalidate/";
        var uri = exchange.getRequest().getURI().getPath();
        var index = uri.indexOf(endpoint);

        var jwtToken = uri.substring(index + endpoint.length());
        try {
            var app = peerAwareInstanceRegistry.getApplications().getRegisteredApplications(CoreService.GATEWAY.getServiceId());
            boolean invalidated = authenticationService.invalidateJwtTokenGateway(jwtToken, false, app);
            return Mono.just(ResponseEntity.status(invalidated ? SC_OK : SC_SERVICE_UNAVAILABLE).build());
        } catch (TokenNotValidException e) {
            return Mono.just(ResponseEntity.status(SC_SERVICE_UNAVAILABLE).build());
        }

    }

}
