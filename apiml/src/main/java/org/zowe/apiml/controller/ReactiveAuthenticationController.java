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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.netflix.eureka.registry.PeerAwareInstanceRegistryImpl;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.media.SchemaProperty;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.product.constants.CoreService;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.TokenAuthentication;
import org.zowe.apiml.security.common.token.TokenNotValidException;
import org.zowe.apiml.util.HttpUtils;
import org.zowe.apiml.zaas.controllers.AuthController;
import org.zowe.apiml.zaas.security.service.AuthenticationService;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderJWK;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import org.zowe.apiml.zaas.security.webfinger.WebFingerProvider;
import org.zowe.apiml.zaas.security.webfinger.WebFingerResponse;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PublicKey;
import java.util.*;

import static org.apache.http.HttpStatus.SC_OK;
import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.zowe.apiml.security.common.filter.StoreAccessTokenInfoFilter.TOKEN_REQUEST;
import static org.zowe.apiml.zaas.controllers.AuthController.*;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactiveAuthenticationController {

    private static final String TOKEN_KEY = "token";
    private static final ObjectWriter WRITER = new ObjectMapper().writer();

    private final JwtSecurity jwtSecurity;
    private final ZosmfService zosmfService;
    private final MessageService messageService;
    private final AuthenticationService authenticationService;
    private final PeerAwareInstanceRegistryImpl peerAwareInstanceRegistry;
    private final HttpUtils httpUtils;
    private final AccessTokenProvider tokenProvider;
    private final WebFingerProvider webFingerProvider;
    private final RauditxService rauditxService;
    @Nullable
    private final OIDCProvider oidcProvider;

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
    public Mono<ResponseEntity<Object>> login(ServerWebExchange exchange) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .map(authentication -> {
                var jwt = ((TokenAuthentication) authentication).getCredentials();
                // Create the HttpOnly cookie containing the JWT
                var jwtCookie = httpUtils.createResponseCookie(jwt);

                // Add the cookie to the response headers
                exchange.getResponse().addCookie(jwtCookie);
                log.debug("JWT Cookie set for user: {}", authentication.getName());

                // Return an OK response
                return ResponseEntity.noContent().build();
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(401)).build()));
    }
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessTokenRequest {
        private int validity;
        private Set<String> scopes;
    }

    @PostMapping("/access-token/generate")
    public Mono<ResponseEntity<String>> generatePat(@RequestAttribute(TOKEN_REQUEST) AccessTokenRequest accessTokenRequest) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .map(authentication -> {
                var userId = authentication.getName();

                log.debug("Generating access token for user {}", userId);

                RauditxService.RauditxBuilder rauditBuilder = rauditxService.builder()
                    .userId(userId)
                    .messageSegment("An attempt to generate PAT")
                    .alwaysLogSuccesses()
                    .alwaysLogFailures();

                String pat;
                try {
                    pat = tokenProvider.getToken(userId, accessTokenRequest.getValidity(), accessTokenRequest.getScopes());
                    rauditBuilder.success();
                } catch (RuntimeException e) {
                    rauditBuilder.failure();
                    rauditBuilder.issue();
                    throw e;
                }
                return ResponseEntity.ok(pat);
            })
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

    /**
     * Validates whether a personal access token is currently valid and authorized for the specified service ID.
     * The request must contain a valid token and the associated service ID. If the token is valid and has not been
     * invalidated, a 204 No Content response is returned. Otherwise, a 401 Unauthorized response is returned.
     * <p>
     * Request body example:
     * {
     *   "token": "pat-token",
     *   "serviceId": "target-service"
     * }
     * <p>
     * Responses:
     * - 204 No Content – Token is valid and active
     * - 401 Unauthorized – Token is invalid or revoked
     *
     * @param validateRequestModel Object containing the token and target service ID
     * @return Mono with HTTP response indicating token validity
     */
    @PostMapping(path = ACCESS_TOKEN_VALIDATE)
    @Operation(summary = "Validate personal access token.",
        tags = {"Access token"},
        operationId = "accessTokenValidatePOST",
        description = "Use the `/access-token/validate` API to verify that personal access token is valid. \n\n**Response:**\n\nThe response is a plain text body.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = ValidateRequestModel.class)
            ),
            description = "Specifies the personal access token and service ID for validation."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Valid token"),
        @ApiResponse(responseCode = "401", description = "Invalid token")
    })
    public Mono<ResponseEntity<Object>> validateAccessToken(@RequestBody ValidateRequestModel validateRequestModel) {
        var token = validateRequestModel.getToken();
        var serviceId = validateRequestModel.getServiceId();
        if (tokenProvider.isValidForScopes(token, serviceId) &&
            !tokenProvider.isInvalidated(token)) {
            return Mono.just(ResponseEntity.noContent().build());
        }
        return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
    }

    /**
     * Invalidates a specific personal access token. Requires the token to be provided in the request body.
     * Request body:
     * {
     *   "token": "your_access_token"
     * }
     * Responses:
     * - 204 No Content – Token successfully invalidated
     * - 400 Bad Request – Token missing or empty
     * - 401 Unauthorized – Token already invalidated
     * - 503 Service Unavailable – Invalidation failed
     *
     * @param bodyMono Mono containing a map with the token to invalidate
     * @return Mono with the appropriate HTTP response
     */
    @DeleteMapping(path = ACCESS_TOKEN_REVOKE)
    @Operation(
        summary = "Invalidate personal access token.",
        tags = {"Access token"},
        operationId = "accessTokenInvalidateDELETE",
        description = "Use the `/access-token/revoke` API to invalidate a specific personal access token. \n\n**Response:**\n\nThe response is no content.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schemaProperties = {
                    @SchemaProperty(name = TOKEN_KEY, schema = @Schema(type = "string"))
                }
            ),
            description = "Specifies the personal access token."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully revoked"),
        @ApiResponse(responseCode = "400", description = "Missing or invalid token"),
        @ApiResponse(responseCode = "401", description = "Invalid token"),
        @ApiResponse(responseCode = "503", description = "Token invalidation failed")
    })
    public Mono<ResponseEntity<Object>> revokeAccessToken(@RequestBody Mono<Map<String, String>> bodyMono) {
        return bodyMono
            .map(body -> body.get(TOKEN_KEY))
            .flatMap(token -> {
                if (token == null || token.trim().isEmpty()) {
                    return Mono.just(ResponseEntity.badRequest().build());
                }

                if (tokenProvider.isInvalidated(token)) {
                    return Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build());
                }

                return Mono.fromCallable(() -> {
                    tokenProvider.invalidateToken(token);
                    return ResponseEntity.noContent().build();
                }).onErrorResume(IOException.class, e -> {
                    log.error("Token invalidation failed", e);
                    return Mono.just(ResponseEntity.status(SC_SERVICE_UNAVAILABLE).build());
                });
            });
    }

    /**
     * Invalidates all PATs for the currently authenticated user. Uses the authenticated principal from the security context.
     * Timestamp in the body is optional. If not provided, the current time is used.
     * <p>
     * Request body (optional):
     * {
     *   "timestamp": 1710000000000
     * }
     * <p>
     * Responses:
     * - 204 No Content – Tokens successfully invalidated
     * - 401 Unauthorized – No authentication present
     *
     * @param rulesRequestModel Optional model containing the timestamp
     * @return Mono with the appropriate HTTP response
     */
    @DeleteMapping(path = ACCESS_TOKEN_REVOKE_MULTIPLE)
    @Operation(summary = "Invalidate multiple personal access tokens.",
        tags = {"Access token"},
        operationId = "accessTokensInvalidateDELETE",
        description = "Use the `/access-token/revoke/token` API to invalidate multiple personal access tokens issued for your user ID. \n\n**Request:**\n\nThe revoke request requires the user credentials in one of the following formats:\n  * Cookie named `apimlAuthenticationToken`.\n * Bearer authentication \n*Header example:* Authorization: Bearer *token* \n* Client certificate \n\n**Response:**\n\nThe response is no content.",
        security = {
            @SecurityRequirement(name = "Bearer"),
            @SecurityRequirement(name = "CookieAuth"),
            @SecurityRequirement(name = "ClientCert")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schemaProperties = {
                    @SchemaProperty(name = "timestamp", schema = @Schema(type = "number"))
                }
            ),
            description = "Specifies the time until which the tokens will remain invalid."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully revoked")
    })
    public Mono<ResponseEntity<Object>> revokeAllUserAccessTokens(@RequestBody(required = false) RulesRequestModel rulesRequestModel) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .flatMap(authentication -> {
                var userId = authentication.getPrincipal().toString();
                log.debug("revokeAllUserAccessTokens: userId={}", userId);

                long timeStamp = 0;
                if (rulesRequestModel != null) {
                    timeStamp = rulesRequestModel.getTimestamp();
                }

                tokenProvider.invalidateAllTokensForUser(userId, timeStamp);
                return Mono.just(ResponseEntity.noContent().build());
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()));
    }

    //todo fix: if no body is passed at all, it currently returns
    // The service has encountered a situation it doesn't know how to handle. Please contact support for further assistance. More details are available in the log under the provided message instance ID.
    /**
     * Admin-only: Invalidates all PATs for a specific user ID. Requires SAF authorization and a valid userId in the request body.
     * <p>
     * Request body:
     * {
     *   "userId": "target_user",
     *   "timestamp": 1710000000000
     * }
     * <p>
     * Responses:
     * - 204 No Content – Tokens successfully invalidated
     * - 400 Bad Request – Missing userId
     *
     * @param requestModel Model containing the userId and optional timestamp
     * @return Mono with the appropriate HTTP response
     * @throws JsonProcessingException if the input cannot be parsed
     */
    @DeleteMapping(path = ACCESS_TOKEN_REVOKE_MULTIPLE + "/user")
    @PreAuthorize("@safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('SERVICES', 'READ',#root)")
    @Operation(summary = "Invalidate personal access tokens by user ID.",
        tags = {"Access token"},
        operationId = "accessTokensInvalidateAdminDELETE",
        description = "Use the `/access-token/revoke/token/user` API to invalidate multiple personal access tokens issued for a user ID.\n\n**Request:**\n\nThe revoke user ID request requires the user credentials in one of the following formats:\n\n* Basic authentication\n* Client certificate \n\n**Response:**\n\nThe response is no content.",
        security = {
            @SecurityRequirement(name = "Bearer"),
            @SecurityRequirement(name = "CookieAuth"),
            @SecurityRequirement(name = "LoginBasicAuth"),
            @SecurityRequirement(name = "ClientCert")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schemaProperties = {
                    @SchemaProperty(name = "user", schema = @Schema(type = "string")),
                    @SchemaProperty(name = "timestamp", schema = @Schema(type = "number"))
                }
            ),
            description = "Specifies the user ID and time until which the tokens will remain invalid."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully revoked")
    })
    public Mono<ResponseEntity<String>> revokeAccessTokensForUser(@RequestBody RulesRequestModel requestModel) throws JsonProcessingException {
        long timeStamp = requestModel.getTimestamp();
        String userId = requestModel.getUserId();
        if (userId == null) {
            return badRequestForPATInvalidation();
        }
        log.debug("revokeAccessTokensForUser: userId={}", userId);
        tokenProvider.invalidateAllTokensForUser(userId, timeStamp);

        return Mono.just(ResponseEntity.noContent().build());
    }

    /**
     * Admin-only: Invalidates all personal access tokens for a specific service ID (scope).
     * Requires SAF permission for SERVICES:READ and a valid serviceId in the request body.
     * <p>
     * Request body:
     * {
     *   "serviceId": "target_service",
     *   "timestamp": 1710000000000 // optional
     * }
     * <p>
     * Responses:
     * - 204 No Content – Tokens successfully invalidated
     * - 400 Bad Request – Missing serviceId
     *
     * @param requestModel Model containing the serviceId and optional timestamp
     * @return Mono with the appropriate HTTP response
     * @throws JsonProcessingException if input parsing fails
     */
    @DeleteMapping(path = ACCESS_TOKEN_REVOKE_MULTIPLE + "/scope")
    @PreAuthorize("@safMethodSecurityExpressionRoot.hasSafServiceResourceAccess('SERVICES', 'READ',#root)")
    @Operation(summary = "Invalidate multiple personal access tokens by service ID.",
        tags = {"Access token"},
        operationId = "accessTokensInvalidateAdminScopeDELETE",
        description = "Use the `/access-token/revoke/token/scope` API to invalidate multiple personal access tokens issued for service ID.\n\n**Request:**\n\nThe revoke scope request requires the user credentials in one of the following formats:\n\n* Basic authentication\n* Client certificate  \n\n**Response:**\n\nThe response is no content.",
        security = {
            @SecurityRequirement(name = "Bearer"),
            @SecurityRequirement(name = "CookieAuth"),
            @SecurityRequirement(name = "LoginBasicAuth"),
            @SecurityRequirement(name = "ClientCert")
        },
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schemaProperties = {
                    @SchemaProperty(name = "serviceId", schema = @Schema(type = "string")),
                    @SchemaProperty(name = "timestamp", schema = @Schema(type = "number"))
                }
            ),
            description = "Specifies the service ID and time until which the tokens will remain invalid."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Successfully revoked")
    })
    public Mono<ResponseEntity<String>> revokeAccessTokensForScope(@RequestBody() RulesRequestModel requestModel) throws JsonProcessingException {
        long timeStamp = requestModel.getTimestamp();
        String serviceId = requestModel.getServiceId();
        if (serviceId == null) {
            return badRequestForPATInvalidation();
        }
        tokenProvider.invalidateAllTokensForService(serviceId, timeStamp);

        return Mono.just(ResponseEntity.noContent().build());
    }

    /**
     * Return all public keys involved at the moment in the ZAAS as well as in zOSMF. Keys used for verification of
     * tokens
     *
     * @return Map of keys composed of zOSMF and ZAAS ones
     */
    @GetMapping(path = ALL_PUBLIC_KEYS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Returns all public keys to verify JWT tokens validity",
        tags = {"Security"},
        operationId = "GetAllPublicKeysUsingGET",
        description = "This endpoint returns all possible JWKs, which can verify sign outside the Gateway. It can contain public keys of Zowe and z/OSMF."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JWKSet.class)
            )
        )
    })
    public Mono<Map<String, Object>> getAllPublicKeys() {
        return Mono.fromSupplier(() -> {
            List<JWK> keys;
            if (jwtSecurity.actualJwtProducer() == JwtSecurity.JwtProducer.ZOSMF) {
                keys = new LinkedList<>(zosmfService.getPublicKeys().getKeys());
            } else {
                keys = new LinkedList<>();
            }
            Optional<JWK> key = jwtSecurity.getJwkPublicKey();
            key.ifPresent(keys::add);
        if ((oidcProvider != null) && (oidcProvider instanceof OIDCTokenProviderJWK oidcTokenProviderJwk)) {
            JWKSet oidcSet = oidcTokenProviderJwk.getJwkSet();
            if (oidcSet != null) {
                keys.addAll(oidcSet.getKeys());
            }
        }
            return new JWKSet(keys).toJSONObject(true);
        });
    }

    /**
     * Return key that's actually used. If there is one available from zOSMF, then this one is used otherwise the
     * configured one is used.
     *
     * @return The key actually used to verify the JWT tokens.
     */
    @GetMapping(path = CURRENT_PUBLIC_KEYS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Returns public keys to verify JWT tokens, which can be generated now",
        tags = {"Security"},
        operationId = "GetCurrentPublicKeysUsingGET",
        description = "This endpoint returns all possible JWKs, which can verify signature outside the Gateway for this moment. It filters JWK by current settings of Zowe and z/OSMF."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(implementation = JWKSet.class)
            )
        )
    })
    public Mono<Map<String, Object>> getCurrentPublicKeys() {
        return Mono.fromSupplier(() -> {
            final List<JWK> keys = getCurrentKey();
            return new JWKSet(keys).toJSONObject(true);
        });
    }

    /**
     * Return key that's actually used. If there is one available from zOSMF, then this one is used otherwise the
     * configured one is used. The key is provided in the PEM format.
     * <p>
     * Until the key to be produced is resolved, this returns 500 with the message code ZWEAG716.
     *
     * @return The key actually used to verify the JWT tokens.
     */
    @GetMapping(path = PUBLIC_KEYS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Get the public key of certificate that is used by the Gateway to sign tokens",
        tags = {"Security"},
        operationId = "getCurrentPublicKeys",
        description = "This endpoint returns JWK of currently used key, which can verify sign outside the Gateway for this moment. It filters JWK by current settings of Zowe and z/OSMF."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(
                mediaType = MediaType.APPLICATION_JSON_VALUE,
                schema = @Schema(type = "string", description = "Certificate in the PEM format")
            )
        )
    })
    public Mono<ResponseEntity<Object>> getPublicKeyUsedForSigning() {
       return Mono.fromSupplier(() -> {
           List<JWK> publicKeys = getCurrentKey().stream()
               .filter(RSAKey.class::isInstance)
               .toList();
            if (publicKeys.isEmpty()) {
                log.debug("JWT setup was not yet initialized so there is no public key for response.");
                return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.zaas.keys.unknownState").mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            if (publicKeys.size() != 1) {
                log.error("There are incorrect number of public keys returned from JWT producer: {}. Number of entries: {}", jwtSecurity.actualJwtProducer(), publicKeys.size());
                return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.zaas.keys.wrongAmount", publicKeys.size()).mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
            try {
                PublicKey key = publicKeys.get(0)
                    .toRSAKey()
                    .toPublicKey();
                return new ResponseEntity<>(getPublicKeyAsPem(key), HttpStatus.OK);
            } catch (IOException | JOSEException ex) {
                log.error("It was not possible to get public key for JWK, exception message: {}", ex.getMessage());
                return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.zaas.keys.unknown").mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
            }
        });

    }
    private String getPublicKeyAsPem(PublicKey publicKey) throws IOException {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        pemWriter.flush();
        pemWriter.close();
        return stringWriter.toString();
    }

    private List<JWK> getCurrentKey() {
        JwtSecurity.JwtProducer producer = jwtSecurity.actualJwtProducer();

        JWKSet currentKey;
        switch (producer) {
            case ZOSMF:
                currentKey = zosmfService.getPublicKeys();
                break;
            case APIML:
                currentKey = jwtSecurity.getPublicKeyInSet();
                break;
            default:
                //return 500 as we just don't know yet.
                return Collections.emptyList();
        }
        return currentKey.getKeys();
    }

    @Data
    public static class RulesRequestModel {
        private String serviceId;
        private String userId;
        private long timestamp;
    }

    private Mono<ResponseEntity<String>> badRequestForPATInvalidation() throws JsonProcessingException {
        final ApiMessageView message = messageService.createMessage("org.zowe.apiml.security.query.invalidRevokeRequestBody").mapToView();
        return Mono.just(new ResponseEntity<>(WRITER.writeValueAsString(message), HttpStatus.BAD_REQUEST));
    }

    @PostMapping(path = OIDC_TOKEN_VALIDATE)
    @Operation(summary = "Validate OIDC token",
        tags = {"OIDC"},
        operationId = "validateOIDCToken",
        description = "Use the `/oidc-token/validate` API to validate token against configured OIDC provider. " +
            "The Gateway can verify token locally or remotely depends on API Mediation Layer configuration.",
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = AuthController.ValidateRequestModel.class)
            ),
            description = "Specifies the OIDC token for validation without scopes (serviceId will be ignored)."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Valid token"),
        @ApiResponse(responseCode = "401", description = "Invalid token or OIDC provider is not defined")
    })
    public Mono<ResponseEntity<Void>> validateOIDCToken(@RequestBody AuthController.ValidateRequestModel validateRequestModel) {
        return Mono.fromSupplier(() -> {
            log.debug("Validating OIDC token using provider {}", oidcProvider);
            var token = validateRequestModel.getToken();
            if (oidcProvider != null && oidcProvider.isValid(token)) {
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            }
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        });
    }

    /**
     * Proof of concept of WebFinger provider for OIDC clients.
     *
     * @return List of link's relation type and the target URI for provided clientID
     */
    @GetMapping(path = OIDC_WEBFINGER_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "List of link's relation type and the target URI for provided clientID",
        tags = {"OIDC"},
        operationId = "getWebFinger",
        description = "[EXPERIMENTAL] The endpoint can be used to obtain links to authenticate against OIDC provider based on clientID provided in the request. " +
            "The links are defined in the configuration of the API Mediation Layer.",
        security = {
            @SecurityRequirement(name = "Bearer"),
            @SecurityRequirement(name = "CookieAuth"),
            @SecurityRequirement(name = "LoginBasicAuth")
        })
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "OK"),
        @ApiResponse(responseCode = "404", description = "WebFinger is disabled"),
    })
    public Mono<ResponseEntity<Object>> getWebFinger(@RequestParam(name = "resource") String clientId) {
        return Mono.fromSupplier(() -> {
            if (webFingerProvider.isEnabled()) {
                try {
                    WebFingerResponse response = webFingerProvider.getWebFingerConfig(clientId);
                    return ResponseEntity.ok(response);
                } catch (IOException e) {
                    log.debug("Error while reading webfinger configuration from source.", e);
                    final ApiMessageView message = messageService.createMessage("org.zowe.apiml.security.oidc.invalidWebfingerConfiguration").mapToView();
                    try {
                        return ResponseEntity.internalServerError().body(WRITER.writeValueAsString(message));
                    } catch (JsonProcessingException ex) {
                        return ResponseEntity.internalServerError().build();
                    }
                }

            }
            return ResponseEntity.notFound().build();
        });

    }

}
