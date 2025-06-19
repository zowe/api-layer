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

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderJWK;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.StringWriter;
import java.security.PublicKey;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.zowe.apiml.zaas.controllers.AuthController.ALL_PUBLIC_KEYS_PATH;
import static org.zowe.apiml.zaas.controllers.AuthController.CURRENT_PUBLIC_KEYS_PATH;
import static org.zowe.apiml.zaas.controllers.AuthController.PUBLIC_KEYS_PATH;

@RestController
@RequestMapping("/gateway/api/v1/auth")
@Slf4j
@RequiredArgsConstructor
public class ReactivePublicJWKController {

    @Nullable private final OIDCProvider oidcProvider;
    private final JwtSecurity jwtSecurity;
    private final ZosmfService zosmfService;
    private final MessageService messageService;

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

}
