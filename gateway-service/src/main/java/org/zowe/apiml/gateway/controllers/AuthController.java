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

import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import net.minidev.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurity;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.security.common.token.TokenNotValidException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;
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

    private final JwtSecurity jwtSecurityInitializer;
    private final ZosmfService zosmfService;

    public static final String CONTROLLER_PATH = "/gateway/auth";  // NOSONAR: URL is always using / to separate path segments
    public static final String INVALIDATE_PATH = "/invalidate/**";  // NOSONAR
    public static final String DISTRIBUTE_PATH = "/distribute/**";  // NOSONAR
    public static final String PUBLIC_KEYS_PATH = "/keys/public";  // NOSONAR
    public static final String ALL_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/all";
    public static final String CURRENT_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/current";

    @DeleteMapping(path = INVALIDATE_PATH)
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
    public JSONObject getAllPublicKeys() {
        final List<JWK> keys = new LinkedList<>();
        keys.addAll(zosmfService.getPublicKeys().getKeys());
        Optional<JWK> key = jwtSecurityInitializer.getJwkPublicKey();
        key.ifPresent(keys::add);
        return new JWKSet(keys).toJSONObject(true);
    }

    /**
     * Return key that's actually used. If there is one available from zOSMF, then this one is used otherwise the
     * configured one is used.
     *
     * @return The key actually used to verify the JWT tokens.
     */
    @GetMapping(path = CURRENT_PUBLIC_KEYS_PATH)
    @ResponseBody
    public JSONObject getCurrentPublicKeys(
        @RequestHeader(name = "X-Zowe-Key-Format", required = false) String keyFormat
    ) {
        // If either none header or a header X-Zowe-Key-Format is sent with value PEM
        //    Otherwise return 400 with information about the unsupported format for key

        // If the zOSMF is used to issue tokens
        //   If zOSMF is offline or unavailable
        //      Return 500 with message on zOSMF not available
        //   Load the key from zOSMF
        //   If X-Zowe-Key-Format is PEM
        //      Transform the received key to PEM
        // If internal Key is used
        //   Load the key from Keystore/Keyring
        //   If X-Zowe-Key-Format is PEM
        //      Transform to the PEM

        final List<JWK> keys = new LinkedList<>(zosmfService.getPublicKeys().getKeys());

        if (keys.isEmpty()) {
            Optional<JWK> key = jwtSecurityInitializer.getJwkPublicKey();
            key.ifPresent(keys::add);
        }
        return new JWKSet(keys).toJSONObject(true);
    }

}
