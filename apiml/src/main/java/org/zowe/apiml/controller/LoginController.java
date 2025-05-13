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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.config.AuthConfigurationProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import reactor.core.publisher.Mono;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.zowe.apiml.zaas.controllers.AuthController.INVALIDATE_PATH;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class LoginController {

    private final AuthConfigurationProperties authConfigurationProperties;
    private final AuthenticationService authenticationService;
    private final PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;
    private final HttpUtils httpUtils;



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
            // Create the HttpOnly cookie containing the JWT
            ResponseCookie jwtCookie = httpUtils.createResponseCookie(jwt);

            // Add the cookie to the response headers
            exchange.getResponse().addCookie(jwtCookie);
            log.debug("JWT Cookie set for user: {}", authentication.getName());

            // Return an OK response
            return Mono.just(ResponseEntity.ok().<Void>build());
        });
    }

    @DeleteMapping(path = INVALIDATE_PATH)
//    @Operation(summary = "Logout JWT token.",
//        tags = {"Security"},
//        operationId = "invalidateJwtToken",
//        description = "Use the `/auth/invalidate` API to invalidate token on specific instance of Gateway.",
//        security = {
//            @SecurityRequirement(name = "ClientCert")
//        })
//    @ApiResponses(value = {
//        @ApiResponse(responseCode = "200", description = "Successfully invalidated"),
//        @ApiResponse(responseCode = "400", description = "Invalid token"),
//        @ApiResponse(responseCode = "503", description = "Authentication service is not available")
//    })
    public Mono<ResponseEntity<Void>> invalidateJwtToken(ServerWebExchange exchange) {
        final String endpoint = "/auth/invalidate/";
        final String uri = exchange.getRequest().getURI().getPath();
        final int index = uri.indexOf(endpoint);

        final String jwtToken = uri.substring(index + endpoint.length());
        try {
            var app = peerAwareInstanceRegistry.getApplications().getRegisteredApplications(CoreService.GATEWAY.getServiceId());
            final boolean invalidated = authenticationService.invalidateJwtTokenGateway(jwtToken, false, app);
            return Mono.just(ResponseEntity.status(invalidated ? SC_OK : SC_SERVICE_UNAVAILABLE).build());
        } catch (TokenNotValidException e) {
            return Mono.just(ResponseEntity.status(SC_SERVICE_UNAVAILABLE).build());
        }
    }




}
