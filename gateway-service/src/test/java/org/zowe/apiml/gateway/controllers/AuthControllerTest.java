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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.zowe.apiml.gateway.security.service.AuthenticationService;
import org.zowe.apiml.gateway.security.service.JwtSecurity;
import org.zowe.apiml.gateway.security.service.zosmf.ZosmfService;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.yaml.YamlMessageService;

import java.text.ParseException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.apache.http.HttpStatus.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
class AuthControllerTest {

    private AuthController authController;
    private MockMvc mockMvc;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private JwtSecurity jwtSecurity;

    @Mock
    private ZosmfService zosmfService;

    private MessageService messageService;

    private JWK jwk1, jwk2, jwk3;

    @BeforeEach
    void setUp() throws ParseException {
        messageService = new YamlMessageService("/gateway-log-messages.yml");
        authController = new AuthController(authenticationService, jwtSecurity, zosmfService, messageService);
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
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(jwk3));
    }

    @Test
    void testGetAllPublicKeys() throws Exception {
        initPublicKeys(true);
        JWKSet jwkSet = new JWKSet(Arrays.asList(jwk1, jwk2, jwk3));
        this.mockMvc.perform(get("/gateway/auth/keys/public/all"))
            .andExpect(status().is(SC_OK))
            .andExpect(content().json(jwkSet.toString()));
    }

    @Nested
    class WhenGettingActiveKey {
        @Test
        void useZoweJwt() throws Exception {
            initPublicKeys(false);
            JWKSet jwkSet = new JWKSet(Collections.singletonList(jwk3));
            mockMvc.perform(get("/gateway/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toString()));
        }

        @Test
        void useBoth() throws Exception {
            initPublicKeys(true);
            JWKSet jwkSet = new JWKSet(Arrays.asList(jwk1, jwk2));
            mockMvc.perform(get("/gateway/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toString()));
        }

        @Test
        void missingZosmf() throws Exception {
            initPublicKeys(false);
            JWKSet jwkSet = new JWKSet(Collections.singletonList(jwk3));
            mockMvc.perform(get("/gateway/auth/keys/public/current"))
                .andExpect(status().is(SC_OK))
                .andExpect(content().json(jwkSet.toString()));
        }
    }

    @Nested
    class GetPublicKeyUsedForSigning {
        @Nested
        class GivenZosmfIsProducer {
            @Test
            void whenOnlineAndSupportJwt_returnValidPemKey() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
                when(zosmfService.getPublicKeys()).thenReturn(new JWKSet(getJwk(0)));

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_OK));
            }

            @Test
            void whenNotReady_returnInternalServerError() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN);

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messageNumber", is("ZWEAG716E")));
            }

            @Test
            void whenZosmfReturnsIncorrectAmountOfKeys_returnInternalServerError() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
                when(zosmfService.getPublicKeys()).thenReturn(new JWKSet());

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_INTERNAL_SERVER_ERROR))
                    .andExpect(jsonPath("$.messageNumber", is("ZWEAG715E")));
            }
        }

        @Nested
        class GivenApiMlIsProducer {
            @Test
            void returnValidPemKey() throws Exception {
                when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
                when(jwtSecurity.getPublicKeyInSet()).thenReturn(new JWKSet(getJwk(0)));

                mockMvc.perform(get("/gateway/auth/keys/public"))
                    .andExpect(status().is(SC_OK));
            }
        }
    }
}
