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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurityInitializer;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;

import static org.apache.http.HttpStatus.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
class AuthControllerTest {

    private AuthController authController;
    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private JwtSecurityInitializer jwtSecurityInitializer;

    @Mock
    private ZosmfService zosmfService;

    private JWK jwk1, jwk2, jwk3;

    @BeforeEach
    void setUp() throws ParseException {
        authController = new AuthController(authenticationService, jwtSecurityInitializer, zosmfService);
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();

        jwk1 = getJwk(1);
        jwk2 = getJwk(2);
        jwk3 = getJwk(3);
    }

    @Test
    void invalidateJwtToken() throws Exception {
        when(authenticationService.invalidateJwtToken("a/b", false)).thenReturn(Boolean.TRUE);
        this.mockMvc.perform(delete("/gateway/auth/invalidate/a/b")).andExpect(status().is(SC_OK));

        when(authenticationService.invalidateJwtToken("abcde", false)).thenReturn(Boolean.TRUE);
        this.mockMvc.perform(delete("/gateway/auth/invalidate/abcde")).andExpect(status().is(SC_OK));

        this.mockMvc.perform(delete("/gateway/auth/invalidate/xyz")).andExpect(status().is(SC_SERVICE_UNAVAILABLE));

        verify(authenticationService, times(1)).invalidateJwtToken("abcde", false);
        verify(authenticationService, times(1)).invalidateJwtToken("a/b", false);
    }

    @Test
    void distributeInvalidate() throws Exception {
        when(authenticationService.distributeInvalidate("instance/1")).thenReturn(true);
        this.mockMvc.perform(get("/gateway/auth/distribute/instance/1")).andExpect(status().is(SC_OK));

        when(authenticationService.distributeInvalidate("instance2")).thenReturn(false);
        this.mockMvc.perform(get("/gateway/auth/distribute/instance2")).andExpect(status().is(SC_NO_CONTENT));
    }

    private JWK getJwk(int i) throws ParseException {
        return JWK.parse("{" +
            "\"e\":\"AQAB\"," +
            "\"n\":\"kWp2zRA23Z3vTL4uoe8kTFptxBVFunIoP4t_8TDYJrOb7D1iZNDXVeEsYKp6ppmrTZDAgd-cNOTKLd4M39WJc5FN0maTAVKJc7NxklDeKc4dMe1BGvTZNG4MpWBo-taKULlYUu0ltYJuLzOjIrTHfarucrGoRWqM0sl3z2-fv9k\",\n" +
            "\"kty\":\"RSA\",\n" +
            "\"kid\":\"" + i + "\"" +
        "}");
    }

    private void initPublicKeys(boolean zosmfKeys) {
        JWKSet zosmf = mock(JWKSet.class);
        when(zosmf.getKeys()).thenReturn(
            zosmfKeys ? Arrays.asList(jwk1, jwk2) : Collections.emptyList()
        );
        when(zosmfService.getPublicKeys()).thenReturn(zosmf);
        when(jwtSecurityInitializer.getJwkPublicKey()).thenReturn(jwk3);
    }

    @Test
    void testGetAllPublicKeys() throws Exception {
        initPublicKeys(true);
        JWKSet jwkSet = new JWKSet(Arrays.asList(jwk1, jwk2, jwk3));
        this.mockMvc.perform(get("/gateway/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

    @Test
    void testGetActivePublicKeys_useZoweJwt() throws Exception {
        initPublicKeys(true);
        authController.setUseZosmfJwtToken(false);
        JWKSet jwkSet = new JWKSet(Collections.singletonList(jwk3));
        this.mockMvc.perform(get("/gateway/auth/keys/public/current"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

    @Test
    void testGetActivePublicKeys_useBoth() throws Exception {
        initPublicKeys(true);
        authController.setUseZosmfJwtToken(true);
        JWKSet jwkSet = new JWKSet(Arrays.asList(jwk1, jwk2));
        this.mockMvc.perform(get("/gateway/auth/keys/public/current"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

    @Test
    void testGetActivePublicKeys_missingZosmf() throws Exception {
        initPublicKeys(false);
        authController.setUseZosmfJwtToken(true);
        JWKSet jwkSet = new JWKSet(Collections.singletonList(jwk3));
        this.mockMvc.perform(get("/gateway/auth/keys/public/current"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

}
