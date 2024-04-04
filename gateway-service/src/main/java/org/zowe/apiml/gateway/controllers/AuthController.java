/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import io.swagger.v3.oas.annotations.Operation;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurity;
import org.zowe.apiml.gateway.security.service.token.OIDCTokenProvider;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.gateway.security.webfinger.WebFingerProvider;
import org.zowe.apiml.gateway.security.webfinger.WebFingerResponse;
import org.zowe.apiml.message.api.ApiMessageView;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.token.AccessTokenProvider;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.security.PublicKey;
import java.util.*;

import static org.apache.http.HttpStatus.*;

/**
 * Controller offer method to control security. It can contains method for user and also method for calling services
 * by gateway to distribute state of authentication between nodes.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(AuthController.CONTROLLER_PATH)
@Slf4j
public class AuthController {

    private final AuthenticationService authenticationService;

    private final JwtSecurity jwtSecurity;
    private final ZosmfService zosmfService;
    private final MessageService messageService;

    private final AccessTokenProvider tokenProvider;

    @Nullable
    private final OIDCTokenProvider oidcProvider;
    private final WebFingerProvider webFingerProvider;

    private static final String TOKEN_KEY = "token";
    private static final ObjectWriter writer = new ObjectMapper().writer();

    public static final String CONTROLLER_PATH = "/gateway/auth";  // NOSONAR: URL is always using / to separate path segments
    public static final String INVALIDATE_PATH = "/invalidate/**";  // NOSONAR
    public static final String DISTRIBUTE_PATH = "/distribute/**";  // NOSONAR
    public static final String PUBLIC_KEYS_PATH = "/keys/public";  // NOSONAR
    public static final String ACCESS_TOKEN_REVOKE = "/access-token/revoke"; // NOSONAR
    public static final String ACCESS_TOKEN_REVOKE_MULTIPLE = "/access-token/revoke/tokens"; // NOSONAR
    public static final String ACCESS_TOKEN_VALIDATE = "/access-token/validate"; // NOSONAR
    public static final String ACCESS_TOKEN_EVICT = "/access-token/evict"; // NOSONAR
    public static final String ALL_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/all";
    public static final String CURRENT_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/current";
    public static final String OIDC_TOKEN_VALIDATE = "/oidc-token/validate"; // NOSONAR
    public static final String OIDC_WEBFINGER_PATH = "/oidc/webfinger";

    @DeleteMapping(path = INVALIDATE_PATH)
    @HystrixCommand
    public void invalidateJwtToken(HttpServletRequest request, HttpServletResponse response) {
        final String endpoint = "/auth/invalidate/";
        final String uri = request.getRequestURI();
        final int index = uri.indexOf(endpoint);

        final String jwtToken = uri.substring(index + endpoint.length());
        try {
            final boolean invalidated = authenticationService.invalidateJwtToken(jwtToken, false);
            response.setStatus(invalidated ? SC_OK : SC_SERVICE_UNAVAILABLE);
        } catch (TokenNotValidException e) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }


    }

    @DeleteMapping(path = ACCESS_TOKEN_REVOKE)
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<String> revokeAccessToken(@RequestBody() Map<String, String> body) throws IOException {
        if (tokenProvider.isInvalidated(body.get(TOKEN_KEY))) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        tokenProvider.invalidateToken(body.get(TOKEN_KEY));
        return new ResponseEntity<>(HttpStatus.OK);
    }

    @DeleteMapping(path = ACCESS_TOKEN_REVOKE_MULTIPLE)
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<String> revokeAllUserAccessTokens(@RequestBody(required = false) RulesRequestModel rulesRequestModel) {
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
        }
        String userId = SecurityContextHolder.getContext().getAuthentication().getPrincipal().toString();
        long timeStamp = 0;
        if (rulesRequestModel != null) {
            timeStamp = rulesRequestModel.getTimestamp();
        }
        tokenProvider.invalidateAllTokensForUser(userId, timeStamp);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = ACCESS_TOKEN_REVOKE_MULTIPLE + "/user")
    @ResponseBody
    @HystrixCommand
    @PreAuthorize("hasSafServiceResourceAccess('SERVICES', 'READ')")
    public ResponseEntity<String> revokeAccessTokensForUser(@RequestBody() RulesRequestModel requestModel) throws JsonProcessingException {
        long timeStamp = requestModel.getTimestamp();
        String userId = requestModel.getUserId();
        if (userId == null) {
            return badRequestForPATInvalidation();
        }
        tokenProvider.invalidateAllTokensForUser(userId, timeStamp);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(path = ACCESS_TOKEN_REVOKE_MULTIPLE + "/scope")
    @ResponseBody
    @HystrixCommand
    @PreAuthorize("hasSafServiceResourceAccess('SERVICES', 'READ')")
    public ResponseEntity<String> revokeAccessTokensForScope(@RequestBody() RulesRequestModel requestModel) throws JsonProcessingException {
        long timeStamp = requestModel.getTimestamp();
        String serviceId = requestModel.getServiceId();
        if (serviceId == null) {
            return badRequestForPATInvalidation();
        }
        tokenProvider.invalidateAllTokensForService(serviceId, timeStamp);

        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @DeleteMapping(value = ACCESS_TOKEN_EVICT)
    @Operation(summary = "Remove invalidated tokens and rules which are not relevant anymore",
        description = "Will evict all the invalidated tokens which are not relevant anymore")
    @ResponseBody
    @PreAuthorize("hasSafServiceResourceAccess('SERVICES', 'UPDATE')")
    @HystrixCommand
    public ResponseEntity<String> evictNonRelevantTokensAndRules() {
        tokenProvider.evictNonRelevantTokensAndRules();
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    @PostMapping(path = ACCESS_TOKEN_VALIDATE)
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<String> validateAccessToken(@RequestBody ValidateRequestModel validateRequestModel) {
        String token = validateRequestModel.getToken();
        String serviceId = validateRequestModel.getServiceId();
        if (tokenProvider.isValidForScopes(token, serviceId) &&
            !tokenProvider.isInvalidated(token)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    @GetMapping(path = DISTRIBUTE_PATH)
    @HystrixCommand
    public void distributeInvalidate(HttpServletRequest request, HttpServletResponse response) {
        final String endpoint = "/auth/distribute/";
        final String uri = request.getRequestURI();
        final int index = uri.indexOf(endpoint);

        final String toInstanceId = uri.substring(index + endpoint.length());
        final boolean distributed = authenticationService.distributeInvalidate(toInstanceId);

        response.setStatus(distributed ? SC_OK : SC_NO_CONTENT);
    }

    /**
     * Return all public keys involved at the moment in the Gateway as well as in zOSMF. Keys used for verification of
     * tokens
     *
     * @return List of keys composed of zOSMF and Gateway ones
     */
    @GetMapping(path = ALL_PUBLIC_KEYS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @HystrixCommand
    public Map<String, Object> getAllPublicKeys() {
        List<JWK> keys;
        if (jwtSecurity.actualJwtProducer() == JwtSecurity.JwtProducer.ZOSMF) {
            keys = new LinkedList<>(zosmfService.getPublicKeys().getKeys());
        } else {
            keys = new LinkedList<>();
        }
        Optional<JWK> key = jwtSecurity.getJwkPublicKey();
        key.ifPresent(keys::add);
        keys.addAll(oidcProvider.getJwkSet().getKeys());
        return new JWKSet(keys).toJSONObject(true);
    }

    /**
     * Return key that's actually used. If there is one available from zOSMF, then this one is used otherwise the
     * configured one is used.
     *
     * @return The key actually used to verify the JWT tokens.
     */
    @GetMapping(path = CURRENT_PUBLIC_KEYS_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @HystrixCommand
    public Map<String, Object> getCurrentPublicKeys() {
        final List<JWK> keys = getCurrentKey();
        return new JWKSet(keys).toJSONObject(true);
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
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> getPublicKeyUsedForSigning() {
        List<JWK> publicKeys = getCurrentKey();
        if (publicKeys.isEmpty()) {
            log.debug("JWT setup was not yet initialized so there is no public key for response.");
            return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.keys.unknownState").mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        if (publicKeys.size() != 1) {
            log.error("There are incorrect number of public keys returned from JWT producer: {}. Number of entries: {}", jwtSecurity.actualJwtProducer(), publicKeys.size());
            return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.keys.wrongAmount", publicKeys.size()).mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
        try {
            PublicKey key = publicKeys.get(0)
                .toRSAKey()
                .toPublicKey();
            return new ResponseEntity<>(getPublicKeyAsPem(key), HttpStatus.OK);
        } catch (IOException | JOSEException ex) {
            log.error("It was not possible to get public key for JWK, exception message: {}", ex.getMessage());
            return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.unknown").mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
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

    @PostMapping(path = OIDC_TOKEN_VALIDATE)
    @HystrixCommand
    public ResponseEntity<String> validateOIDCToken(@RequestBody ValidateRequestModel validateRequestModel) {
        String token = validateRequestModel.getToken();
        if (oidcProvider != null && oidcProvider.isValid(token)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        return new ResponseEntity<>(HttpStatus.UNAUTHORIZED);
    }

    /**
     * Proof of concept of WebFinger provider for OIDC clients.
     *
     * @return List of link's relation type and the target URI for provided clientID
     */
    @GetMapping(path = OIDC_WEBFINGER_PATH, produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<Object> getWebFinger(@RequestParam(name = "resource") String clientId) throws JsonProcessingException {
        if (webFingerProvider.isEnabled()) {
            try {
                WebFingerResponse response = webFingerProvider.getWebFingerConfig(clientId);
                return ResponseEntity.ok(response);
            } catch (IOException e) {
                log.debug("Error while reading webfinger configuration from source.", e);
                final ApiMessageView message = messageService.createMessage("org.zowe.apiml.security.oidc.invalidWebfingerConfiguration").mapToView();
                return ResponseEntity.internalServerError().body(writer.writeValueAsString(message));
            }

        }
        return ResponseEntity.notFound().build();
    }

    private String getPublicKeyAsPem(PublicKey publicKey) throws IOException {
        StringWriter stringWriter = new StringWriter();
        PemWriter pemWriter = new PemWriter(stringWriter);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        pemWriter.flush();
        pemWriter.close();
        return stringWriter.toString();
    }

    private ResponseEntity<String> badRequestForPATInvalidation() throws JsonProcessingException {
        final ApiMessageView message = messageService.createMessage("org.zowe.apiml.security.query.invalidRevokeRequestBody").mapToView();
        return new ResponseEntity<>(writer.writeValueAsString(message), HttpStatus.BAD_REQUEST);
    }

    @Data
    private static class ValidateRequestModel {
        private String token;
        private String serviceId;
    }

    @Data
    private static class RulesRequestModel {
        private String serviceId;
        private String userId;
        private long timestamp;
    }
}
