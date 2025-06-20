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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.message.api.ApiMessage;
import org.zowe.apiml.message.core.Message;
import org.zowe.apiml.message.core.MessageService;
import org.zowe.apiml.message.core.MessageType;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.zaas.security.service.JwtSecurity;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProviderJWK;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactivePublicJWKControllerTest {

    @Mock private JwtSecurity jwtSecurity;
    @Mock private ZosmfService zosmfService;
    @Mock private OIDCProvider oidcProvider;
    @Mock private MessageService messageService;

    @InjectMocks
    private ReactivePublicJWKController controller;

    @Test
    void getAllPublicKeys_zosmfProducer_withOidc() throws Exception {
        JWK zosmfJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("zosmfKey").build();
        JWKSet zosmfKeySet = new JWKSet(zosmfJwk);
        JWK apimlJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("apimlKey").build();
        JWK oidcJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("oidcKey").build();
        JWKSet oidcKeySet = new JWKSet(oidcJwk);

        OIDCTokenProviderJWK mockOidcProviderJwk = mock(OIDCTokenProviderJWK.class);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
        when(zosmfService.getPublicKeys()).thenReturn(zosmfKeySet);
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));
        var testControllerWithOidc = new ReactivePublicJWKController(mockOidcProviderJwk, jwtSecurity, zosmfService, messageService);

        when(mockOidcProviderJwk.getJwkSet()).thenReturn(oidcKeySet);

        Mono<Map<String, Object>> result = testControllerWithOidc.getAllPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(3, keys.size()); // zosmf, apiml, oidc
                return keys.stream().anyMatch(k -> "zosmfKey".equals(k.get("kid"))) &&
                    keys.stream().anyMatch(k -> "apimlKey".equals(k.get("kid"))) &&
                    keys.stream().anyMatch(k -> "oidcKey".equals(k.get("kid")));
            })
            .verifyComplete();
    }

    @Test
    void getAllPublicKeys_apimlProducer_noOidc() throws Exception {
        var apimlJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("apimlKey").build();

        var testControllerNoOidc = new ReactivePublicJWKController(null, jwtSecurity, zosmfService, messageService);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));

        Mono<Map<String, Object>> result = testControllerNoOidc.getAllPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "apimlKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
        verify(zosmfService, never()).getPublicKeys();
    }


    @Test
    void getCurrentPublicKeys_apimlProducer() throws Exception {
        JWK apimlJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("currentApimlKey").build();
        JWKSet apimlKeySet = new JWKSet(apimlJwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(apimlKeySet);

        Mono<Map<String, Object>> result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "currentApimlKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
    }

    @Test
    void getCurrentPublicKeys_zosmfProducer() throws Exception {
        JWK zosmfJwk = new RSAKey.Builder((RSAPublicKey) generateKeyPair().getPublic()).keyID("currentZosmfKey").build();
        JWKSet zosmfKeySet = new JWKSet(zosmfJwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
        when(zosmfService.getPublicKeys()).thenReturn(zosmfKeySet);

        Mono<Map<String, Object>> result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "currentZosmfKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
    }

    @Test
    void getCurrentPublicKeys_unknownProducer() {
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN); // Or any other not APIML/ZOSMF

        Mono<Map<String, Object>> result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(jsonObject -> {
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                return keys.isEmpty();
            })
            .verifyComplete();
    }


    @Test
    void getPublicKeyUsedForSigning_success() throws Exception {
        KeyPair keyPair = generateKeyPair();
        JWK jwk = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).keyID("signingKey").build();
        JWKSet keySet = new JWKSet(jwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(keySet);

        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                String pem = (String) responseEntity.getBody();
                return pem.startsWith("-----BEGIN PUBLIC KEY-----") && pem.endsWith("-----END PUBLIC KEY-----\n");
            })
            .verifyComplete();
    }

    @Test
    void getPublicKeyUsedForSigning_noKeyAvailable() {
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN); // results in empty list from getCurrentKey
        var mockApiMessage = mock(Message.class);
        when(messageService.createMessage("org.zowe.apiml.zaas.keys.unknownState")).thenReturn(mockApiMessage);


        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.INTERNAL_SERVER_ERROR.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void givenMultipleKeys_thenReturnErrorWithCorrectMessage() throws Exception {
        KeyPair kp1 = generateKeyPair();
        KeyPair kp2 = generateKeyPair();
        JWK jwk1 = new RSAKey.Builder((RSAPublicKey) kp1.getPublic()).keyID("key1").build();
        JWK jwk2 = new RSAKey.Builder((RSAPublicKey) kp2.getPublic()).keyID("key2").build();
        JWKSet keySet = new JWKSet(List.of(jwk1, jwk2));

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(keySet);
        var mockApiMessage = mock(Message.class);
        when(messageService.createMessage(
            "org.zowe.apiml.zaas.keys.wrongAmount",
            2
        )).thenReturn(mockApiMessage);
        ApiMessage expectedApiMessage = new ApiMessage("org.zowe.apiml.zaas.keys.wrongAmount", MessageType.ERROR, "ZWEAG715E", "cnt", null, null);

        lenient().when(mockApiMessage.mapToApiMessage()).thenReturn(expectedApiMessage);


        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
                assertNotNull(responseEntity.getBody());
                return ((ApiMessage) responseEntity.getBody()).getMessageNumber().equals("ZWEAG715E");
            })
            .verifyComplete();
    }

    @Test
    void getPublicKeyUsedForSigning_joseException() throws Exception {
        KeyPair keyPair = KeyPairGenerator.getInstance("RSA").generateKeyPair();
        RSAKey realRsaKey = new RSAKey.Builder((RSAPublicKey) keyPair.getPublic()).build();

        RSAKey spyRsaKey = spy(realRsaKey);
        doThrow(new JOSEException("Test JOSE Exception")).when(spyRsaKey).toPublicKey();

        JWKSet keySet = new JWKSet(List.of(spyRsaKey));

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(keySet);

        ApiMessage expectedApiMessage = new ApiMessage("org.zowe.apiml.zaas.keys.unknown", MessageType.ERROR, "ZWEAG717E", "cnt", null, null);
        var mockApiMessage = mock(Message.class);
        when(messageService.createMessage("org.zowe.apiml.zaas.keys.unknown")).thenReturn(mockApiMessage);
        when(mockApiMessage.mapToApiMessage()).thenReturn(expectedApiMessage);

        Mono<ResponseEntity<Object>> result = controller.getPublicKeyUsedForSigning();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, responseEntity.getStatusCode());
                assertNotNull(responseEntity.getBody());
                return ((ApiMessage) responseEntity.getBody()).getMessageNumber().equals("ZWEAG717E");
            })
            .verifyComplete();
    }

    private KeyPair generateKeyPair() throws NoSuchAlgorithmException {
        KeyPairGenerator kpg = KeyPairGenerator.getInstance("RSA");
        kpg.initialize(2048);
        return kpg.generateKeyPair();
    }

}
