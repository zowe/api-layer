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

import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.bouncycastle.util.io.pem.PemObject;
import org.bouncycastle.util.io.pem.PemWriter;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurity;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.StringWriter;
import java.security.PublicKey;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.apache.http.HttpStatus.*;

/**
 * Controller offer method to control security. It can contains method for user and also method for calling services
 * by gateway to distribute state of authentication between nodes.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(AuthController.CONTROLLER_PATH)
public class AuthController {

    private final AuthenticationService authenticationService;

    private final JwtSecurity jwtSecurity;
    private final ZosmfService zosmfService;
    private final MessageService messageService;

    public static final String CONTROLLER_PATH = "/gateway/auth";  // NOSONAR: URL is always using / to separate path segments
    public static final String INVALIDATE_PATH = "/invalidate/**";  // NOSONAR
    public static final String DISTRIBUTE_PATH = "/distribute/**";  // NOSONAR
    public static final String PUBLIC_KEYS_PATH = "/keys/public";  // NOSONAR
    public static final String ALL_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/all";
    public static final String CURRENT_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/current";

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
     * @return List of keys composed of zOSMF and Gateway ones
     */
    @GetMapping(path = ALL_PUBLIC_KEYS_PATH)
    @ResponseBody
    @HystrixCommand
    public Map<String, Object> getAllPublicKeys() {
        final List<JWK> keys = new LinkedList<>(zosmfService.getPublicKeys().getKeys());
        Optional<JWK> key = jwtSecurity.getJwkPublicKey();
        key.ifPresent(keys::add);
        return new JWKSet(keys).toJSONObject(true);
    }

    /**
     * Return key that's actually used. If there is one available from zOSMF, then this one is used otherwise the
     * configured one is used.
     * @return The key actually used to verify the JWT tokens.
     */
    @GetMapping(path = CURRENT_PUBLIC_KEYS_PATH)
    @ResponseBody
    @HystrixCommand
    public Map<String, Object> getCurrentPublicKeys() {
        final List<JWK> keys = new LinkedList<>(zosmfService.getPublicKeys().getKeys());

        if (keys.isEmpty()) {
            Optional<JWK> key = jwtSecurity.getJwkPublicKey();
            key.ifPresent(keys::add);
        }
        return new JWKSet(keys).toJSONObject(true);
    }

    /**
     * Return key that's actually used. If there is one available from zOSMF, then this one is used otherwise the
     * configured one is used. The key is provided in the PEM format.
     *
     * Until the key to be produced is resolved, this returns 500 with the message code ZWEAG716.
     *
     * @return The key actually used to verify the JWT tokens.
     */
    @GetMapping(path = PUBLIC_KEYS_PATH)
    @ResponseBody
    @HystrixCommand
    public ResponseEntity<?> getPublicKeyUsedForSigning() {
        JwtSecurity.JwtProducer producer = jwtSecurity.actualJwtProducer();

        JWKSet currentKey = new JWKSet();
        switch (producer) {
            case ZOSMF:
                currentKey = zosmfService.getPublicKeys();
                break;
            case APIML:
                currentKey = jwtSecurity.getPublicKeyInSet();
                break;
            case UNKNOWN:
                //return 500 as we just don't know yet.
                return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.keys.unknownState").mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        List<JWK> publicKeys = currentKey.getKeys();
        if (publicKeys.size() != 1) {
            return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.keys.wrongAmount", publicKeys.size()).mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }

        try {
            PublicKey key = publicKeys.get(0)
                .toRSAKey()
                .toPublicKey();
            return new ResponseEntity<>(getPublicKeyAsPem(key), HttpStatus.OK);
        } catch (IOException | JOSEException ex) {
            return new ResponseEntity<>(messageService.createMessage("org.zowe.apiml.gateway.unknown").mapToApiMessage(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private String getPublicKeyAsPem(PublicKey publicKey) throws IOException {
        StringWriter writer = new StringWriter();
        PemWriter pemWriter = new PemWriter(writer);
        pemWriter.writeObject(new PemObject("PUBLIC KEY", publicKey.getEncoded()));
        pemWriter.flush();
        pemWriter.close();
        return writer.toString();
    }
}
