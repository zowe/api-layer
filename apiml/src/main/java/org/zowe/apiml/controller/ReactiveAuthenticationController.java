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
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
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
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Objects;

import static org.apache.http.HttpStatus.SC_BAD_REQUEST;
import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
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
    private final ObjectMapper objectMapper;
    private final TokenCreationService tokenCreationService;

    /**
     * Endpoint to authenticate a user based on credentials from EITHER:
     * 1. HTTP Authorization header (Basic Auth), coming from filter already authenticated
     * 2. Request Body (JSON with username/password) processed by the controller
     * Sets a JWT in an HttpOnly cookie upon success.
     *
     * @param exchange The ServerWebExchange to access request headers, body, and response.
     * @return A Mono<ResponseEntity<Void>> indicating success or failure.
     */
    @PostMapping(value = "/login")
    @Operation(summary = "Authenticate mainframe user credentials and return authentication token.",
        tags = {"Security"},
        operationId = "loginUsingPOST",
        description = """
            Use the `/login` API to authenticate mainframe user credentials and return authentication token. It is also possible to authenticate using the x509 client certificate authentication, if enabled.
            **Request:**
                The login request requires the user credentials in one of the following formats:
                    * Basic access authentication
                    * JSON body, which provides an object with the user credentials
                    * HTTP header containing the client certificate
            **Response:**
                The response is an empty body and a token in a secure HttpOnly cookie named `apimlAuthenticationToken`.
        """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = LoginRequest.class)
            ),
            description = "Specifies the user credentials to be authenticated. If newPassword is provided and the password is valid, the password is changed to newPassword"
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Cookie named apimlAuthenticationToken contains authentication\n" + //
            "token."),
        @ApiResponse(responseCode = "401", description = "Invalid credentials.")
    })
    public Mono<ResponseEntity<Object>> login(ServerWebExchange exchange, ServerHttpRequest request) { // To maintain support for wrongly-formed requests (with content-type but no content for example which are used)
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .filter(Authentication::isAuthenticated)
            .map(authentication -> replyWithJwt(exchange, authentication))
            .switchIfEmpty(Mono.<ResponseEntity<Object>>defer(() -> this.authWithBody(exchange, request)))
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(401)).build()));
    }

    private Mono<ResponseEntity<Object>> authWithBody(ServerWebExchange exchange, ServerHttpRequest request) {
        return readLoginRequestFromBody(request)
            .flatMap(loginRequest -> {
                if (loginRequest == null || StringUtils.isBlank(loginRequest.getUsername()) || loginRequest.getPassword() == null || loginRequest.getPassword().length == 0) {
                    throw new AuthenticationCredentialsNotFoundException("Login object has wrong format.");
                }
                var providerManager = new ProviderManager(compoundAuthProvider);
                var authAdapter = new ReactiveAuthenticationManagerAdapter(providerManager);
                return authAdapter.authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(), loginRequest))
                    .map(authentication -> replyWithJwt(exchange, authentication));
            });
    }

    private Mono<LoginRequest> readLoginRequestFromBody(ServerHttpRequest request) {
        return DataBufferUtils.join(request.getBody())
            .map(buffer -> {
                var bytes = new byte[buffer.readableByteCount()];
                buffer.read(bytes);
                DataBufferUtils.release(buffer);
                return bytes;
            })
            .map(body -> {
                try {
                    return objectMapper.readValue(body, LoginRequest.class);
                } catch (IOException e) {
                    throw new AuthenticationCredentialsNotFoundException("Login object has wrong format.", e);
                }
            });
    }

    private ResponseEntity<Object> replyWithJwt(ServerWebExchange exchange, Authentication authentication) {
        var jwt = ((TokenAuthentication) authentication).getCredentials();
        var jwtCookie = httpUtils.createResponseCookie(jwt);

        exchange.getResponse().addCookie(jwtCookie);
        log.debug("JWT Cookie set for user: {}", authentication.getName());

        return ResponseEntity.noContent().build();
    }

    @GetMapping("/query")
    @Operation(summary = "Validate the authentication token.",
        tags = {"Security"},
        operationId = "validateUsingGET",
        description = """
            Use the `/query` API to validate the token and retrieve the information associated with the token."
                **HTTP Headers:**
                    The query request requires the token in one of the following formats:
                        * Cookie named `apimlAuthenticationToken`.
                        * Bearer authentication

                        "*Header example:* Authorization: Bearer *token*

            **Request payload:**
            The request body is empty.

            **Response Payload:**
            The response is a JSON object, which contains information associated with the token.
        """
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "OK",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = Authentication.class)
            )),
        @ApiResponse(responseCode = "401", description = "Invalid credentials.")
    })
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
            .switchIfEmpty(Mono.just(ResponseEntity.status(SC_UNAUTHORIZED).build()));
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
            return Mono.just(ResponseEntity.status(SC_BAD_REQUEST).build());
        }

    }

    @Operation(summary = "Refresh authentication token.",
        tags = {"Security"},
        operationId = "RefreshTokenUsingPOST",
        description = """
            **Note:** This endpoint is disabled by default.

            Use the `/refresh` API to request a new JWT authentication token for the user associated with provided token.
            The old token is invalidated and new token is issued with refreshed expiration time.

            This endpoint is protect by a client certificate.

            **HTTP Headers:**

                The ticket request requires the token in one of the following formats:
                    * Cookie named `apimlAuthenticationToken`.
                    * Bearer authentication.

            *Header example:* Authorization: Bearer *token*
        """
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Authenticated - Refreshed Personal Access Token"),
        @ApiResponse(responseCode = "401", description = "Zowe token is not provided, is invalid or is expired."),
        @ApiResponse(responseCode = "403", description = "A client certificate is not provided or is expired."),
        @ApiResponse(responseCode = "404", description = "Not Found. The endpoint is not enabled or not properly configured"),
        @ApiResponse(responseCode = "500", description = "Process of refreshing token has failed unexpectedly.")
    })
    @ConditionalOnProperty(value = "apiml.security.allowTokenRefresh", havingValue = "true")
    @PostMapping("/refresh")
    public Mono<ResponseEntity<Object>> refreshAccessToken(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .filter(Authentication::isAuthenticated)
            .filter(TokenAuthentication.class::isInstance)
            .map(TokenAuthentication.class::cast)
            .map(tokenAuthentication -> {
                var gateway = peerAwareInstanceRegistry.getApplications().getRegisteredApplications(CoreService.GATEWAY.getServiceId());
                authenticationService.invalidateJwtTokenGateway(tokenAuthentication.getCredentials(), true, gateway);
                var newToken = tokenCreationService.createJwtTokenWithoutCredentials(tokenAuthentication.getPrincipal());
                exchange.getResponse().addCookie(httpUtils.createResponseCookie(newToken));
                return ResponseEntity.ok().build();
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(401)).build()));
    }

}
