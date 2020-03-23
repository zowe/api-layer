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
import lombok.Setter;
import net.minidev.json.JSONObject;
import org.springframework.web.bind.annotation.*;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurityInitializer;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfServiceFacade;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.LinkedList;
import java.util.List;

import static org.apache.http.HttpStatus.*;

/**
 * Controller offer method to control security. It can contains method for user and also method for calling services
 * by gateway to distribute state of authentication between nodes.
 */
@RequiredArgsConstructor
@RestController
@RequestMapping(AuthController.CONTROLLER_PATH)
public class AuthController {

    @Setter
    @org.springframework.beans.factory.annotation.Value("${apiml.security.zosmf.useJwtToken:true}")
    protected boolean useZosmfJwtToken;

    private final AuthenticationService authenticationService;

    private final JwtSecurityInitializer jwtSecurityInitializer;
    private final ZosmfServiceFacade zosmfServiceFacade;

    public static final String CONTROLLER_PATH = "/api/v1/gateway/auth";
    public static final String INVALIDATE_PATH = "/invalidate/**";
    public static final String DISTRIBUTE_PATH = "/distribute/**";
    public static final String PUBLIC_KEYS_PATH = "/keys/public";
    public static final String ALL_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/all";
    public static final String CURRENT_PUBLIC_KEYS_PATH = PUBLIC_KEYS_PATH + "/current";

    @DeleteMapping(path = INVALIDATE_PATH)
    public void invalidateJwtToken(HttpServletRequest request, HttpServletResponse response) {
        final String endpoint = "/auth/invalidate/";
        final String uri = request.getRequestURI();
        final int index = uri.indexOf(endpoint);

        final String jwtToken = uri.substring(index + endpoint.length());
        final boolean invalidated = authenticationService.invalidateJwtToken(jwtToken, false);

        response.setStatus(invalidated ? SC_OK : SC_SERVICE_UNAVAILABLE);
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

    @GetMapping(path = ALL_PUBLIC_KEYS_PATH)
    @ResponseBody
    public JSONObject getAllPublicKeys() {
        final List<JWK> keys = new LinkedList<>();
        keys.addAll(zosmfServiceFacade.getPublicKeys().getKeys());
        keys.add(jwtSecurityInitializer.getJwkPublicKey());
        return new JWKSet(keys).toJSONObject(true);
    }

    @GetMapping(path = CURRENT_PUBLIC_KEYS_PATH)
    @ResponseBody
    public JSONObject getCurrentPublicKeys() {
        final List<JWK> keys = new LinkedList<>();
        if (useZosmfJwtToken) {
            keys.addAll(zosmfServiceFacade.getPublicKeys().getKeys());
        }
        if (keys.isEmpty()) {
            keys.add(jwtSecurityInitializer.getJwkPublicKey());
        }
        return new JWKSet(keys).toJSONObject(true);
    }

}
