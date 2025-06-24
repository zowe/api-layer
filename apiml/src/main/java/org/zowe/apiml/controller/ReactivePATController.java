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
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.audit.RauditxService;
import org.zowe.apiml.security.common.error.AccessTokenMissingBodyException;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.zaas.controllers.AuthController.RulesRequestModel;
import org.zowe.apiml.zaas.controllers.AuthController.ValidateRequestModel;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.http.HttpStatus.SC_SERVICE_UNAVAILABLE;
import static org.zowe.apiml.zaas.controllers.AuthController.ACCESS_TOKEN_REVOKE;
import static org.zowe.apiml.zaas.controllers.AuthController.ACCESS_TOKEN_REVOKE_MULTIPLE;
import static org.zowe.apiml.zaas.controllers.AuthController.ACCESS_TOKEN_VALIDATE;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactivePATController {

    private static final String TOKEN_KEY = "token";

    private final AccessTokenProvider tokenProvider;
    private final RauditxService rauditxService;
    private final MessageService messageService;
    private final ObjectMapper mapper;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AccessTokenRequest {
        private int validity;
        private Set<String> scopes;
    }

    @Operation(summary = "Authenticate mainframe credentials and return personal access token.",
        tags = {"Access token"},
        operationId = "access-token-generate-POST",
        description = """
            Use the `/access-token/generate` API to authenticate mainframe user credentials and return personal access token. It is also possible to authenticate using the x509 client certificate authentication, if enabled.

                **Request:**

                    The generate request requires the user credentials in one of the following formats:
                        * Basic access authentication
                        * HTTP header containing the client certificate

                **Response:**

                    The response contains a personal access token in the plain text.
        """,
        requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
            content = @Content(
                schema = @Schema(implementation = AccessTokenRequest.class)
            ),
            description = "Specifies the parameters of the requested personal access token."
        )
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Authenticated - Personal Access Token created"),
        @ApiResponse(responseCode = "401", description = "Invalid credentials")
    })
    @PostMapping("/access-token/generate")
    public Mono<ResponseEntity<String>> generatePat(@RequestBody AccessTokenRequest accessTokenRequest) {
        return ReactiveSecurityContextHolder.getContext()
            .map(SecurityContext::getAuthentication)
            .filter(Objects::nonNull)
            .<ResponseEntity<String>>handle((authentication, sink) -> {
                if (accessTokenRequest.getScopes() == null || accessTokenRequest.getScopes().isEmpty()) {
                    sink.error(new AccessTokenMissingBodyException("Missing required scopes in the request body."));
                    return;
                }

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
                    sink.error(e);
                    return;
                } finally {
                    rauditBuilder.issue();
                }
                sink.next(ResponseEntity.ok(pat));
            })
            .switchIfEmpty(Mono.just(ResponseEntity.status(HttpStatusCode.valueOf(401)).build()));
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

    private Mono<ResponseEntity<String>> badRequestForPATInvalidation() throws JsonProcessingException {
        final ApiMessageView message = messageService.createMessage("org.zowe.apiml.security.query.invalidRevokeRequestBody").mapToView();
        return Mono.just(new ResponseEntity<>(mapper.writeValueAsString(message), HttpStatus.BAD_REQUEST));
    }

}
