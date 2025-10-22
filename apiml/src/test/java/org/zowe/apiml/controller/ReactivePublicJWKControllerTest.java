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
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jose4j.jwk.JsonWebKey;
import org.jose4j.jwk.JsonWebKeySet;
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
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProvider;
import org.zowe.apiml.zaas.security.service.zosmf.ZosmfService;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReactivePublicJWKControllerTest {

    @Mock private JwtSecurity jwtSecurity;
    @Mock private ZosmfService zosmfService;
    @Mock private OIDCProvider oidcProvider;
    @Mock private MessageService messageService;

    @InjectMocks
    private ReactivePublicJWKController controller;

    private ObjectMapper mapper = new ObjectMapper();

    @Test
    void getAllPublicKeys_zosmfProducer_withOidc() throws Exception {
        var zosmfJwk = JsonWebKey.Factory.newJwk(generateKeyPair().getPublic());
        zosmfJwk.setKeyId("zosmfKey");
        var zosmfKeySet = new JsonWebKeySet(zosmfJwk);
        var apimlJwk = JsonWebKey.Factory.newJwk(generateKeyPair().getPublic());
        apimlJwk.setKeyId("apimlKey");
        var oidcJwk = JsonWebKey.Factory.newJwk(generateKeyPair().getPublic());
        oidcJwk.setKeyId("oidcKey");
        var oidcKeySet = new JsonWebKeySet(oidcJwk);

        OIDCTokenProvider mockOidcProviderJwk = mock(OIDCTokenProvider.class);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);


        new JsonWebKeySet();

        when(zosmfService.getPublicKeys()).thenReturn(zosmfKeySet);
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));
        var testControllerWithOidc = new ReactivePublicJWKController(mockOidcProviderJwk, jwtSecurity, zosmfService, messageService);

        when(mockOidcProviderJwk.getJwkSet()).thenReturn(oidcKeySet);

        var result = testControllerWithOidc.getAllPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                HashMap<String, Object> jsonObject;
                try {
                    jsonObject = mapper.readValue(responseEntity.getBody(), new TypeReference<HashMap<String,Object>>() {});
                    List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                    assertEquals(3, keys.size()); // zosmf, apiml, oidc
                    return keys.stream().anyMatch(k -> "zosmfKey".equals(k.get("kid"))) &&
                        keys.stream().anyMatch(k -> "apimlKey".equals(k.get("kid"))) &&
                        keys.stream().anyMatch(k -> "oidcKey".equals(k.get("kid")));
                } catch (JsonProcessingException e) {
                    fail(e);
                    return false;
                }
            })
            .verifyComplete();
    }

    @Test
    void getAllPublicKeys_apimlProducer_noOidc() throws Exception {
        var apimlJwk = JsonWebKey.Factory.newJwk(generateKeyPair().getPublic());
        apimlJwk.setKeyId("apimlKey");

        var testControllerNoOidc = new ReactivePublicJWKController(null, jwtSecurity, zosmfService, messageService);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getJwkPublicKey()).thenReturn(Optional.of(apimlJwk));

        var result = testControllerNoOidc.getAllPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                HashMap<String, Object> jsonObject;
                try {
                    jsonObject = mapper.readValue(responseEntity.getBody(), new TypeReference<HashMap<String,Object>>() {});
                } catch (JsonProcessingException e) {
                    fail(e);
                    return false;
                }
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "apimlKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
        verify(zosmfService, never()).getPublicKeys();
    }


    @Test
    void getCurrentPublicKeys_apimlProducer() throws Exception {
        var apimlJwk = JsonWebKey.Factory.newJwk(generateKeyPair().getPublic());
        apimlJwk.setKeyId("currentApimlKey");
        var apimlKeySet = new JsonWebKeySet(apimlJwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(apimlKeySet);

        var result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                HashMap<String, Object> jsonObject;
                try {
                    jsonObject = mapper.readValue(responseEntity.getBody(), new TypeReference<HashMap<String,Object>>() {});
                } catch (JsonProcessingException e) {
                    fail(e);
                    return false;
                }
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "currentApimlKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
    }

    @Test
    void getCurrentPublicKeys_zosmfProducer() throws Exception {
        var zosmfJwk = JsonWebKey.Factory.newJwk(generateKeyPair().getPublic());
        zosmfJwk.setKeyId("currentZosmfKey");
        var zosmfKeySet = new JsonWebKeySet(zosmfJwk);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.ZOSMF);
        when(zosmfService.getPublicKeys()).thenReturn(zosmfKeySet);

        var result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                HashMap<String, Object> jsonObject;
                try {
                    jsonObject = mapper.readValue(responseEntity.getBody(), new TypeReference<HashMap<String,Object>>() {});
                } catch (JsonProcessingException e) {
                    fail(e);
                    return false;
                }
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                assertEquals(1, keys.size());
                return "currentZosmfKey".equals(keys.get(0).get("kid"));
            })
            .verifyComplete();
    }

    @Test
    void getCurrentPublicKeys_unknownProducer() {
        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.UNKNOWN); // Or any other not APIML/ZOSMF

        var result = controller.getCurrentPublicKeys();

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> {
                HashMap<String, Object> jsonObject;
                try {
                    jsonObject = mapper.readValue(responseEntity.getBody(), new TypeReference<HashMap<String,Object>>() {});
                } catch (JsonProcessingException e) {
                    fail(e);
                    return false;
                }
                List<Map<String, Object>> keys = (List<Map<String, Object>>) jsonObject.get("keys");
                return keys.isEmpty();
            })
            .verifyComplete();
    }


    @Test
    void getPublicKeyUsedForSigning_success() throws Exception {
        var keyPair = generateKeyPair();
        var jwk = JsonWebKey.Factory.newJwk(keyPair.getPublic());
        jwk.setKeyId("signingKey");
        var keySet = new JsonWebKeySet(jwk);

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
        var kp1 = generateKeyPair();
        var kp2 = generateKeyPair();

        var jwk1 = JsonWebKey.Factory.newJwk(kp1.getPublic());
        jwk1.setKeyId("key1");
        var jwk2 = JsonWebKey.Factory.newJwk(kp2.getPublic());
        jwk2.setKeyId("key2");

        var keySet = new JsonWebKeySet(List.of(jwk1, jwk2));

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
    void whenNewJwkThrowsException_thenReturnsInternalServerError() throws Exception {
        byte[] badModulus = new byte[]{0};

        var badKey = mock(RSAPublicKey.class);
        when(badKey.getModulus()).thenReturn(new BigInteger(badModulus));
        when(badKey.getPublicExponent()).thenReturn(BigInteger.ONE);
        lenient().when(badKey.getAlgorithm()).thenReturn("RSA");
        lenient().when(badKey.getFormat()).thenReturn(null);
        lenient().when(badKey.getEncoded()).thenReturn(new byte[0]);

        var badJwk = JsonWebKey.Factory.newJwk(badKey);

        when(jwtSecurity.actualJwtProducer()).thenReturn(JwtSecurity.JwtProducer.APIML);
        when(jwtSecurity.getPublicKeyInSet()).thenReturn(new JsonWebKeySet(List.of(badJwk)));

        ApiMessage expectedApiMessage = new ApiMessage("org.zowe.apiml.zaas.keys.unknown", MessageType.ERROR, "ZWEAG717E", "cnt", null, null);
        var mockApiMessage = mock(Message.class);
        when(messageService.createMessage("org.zowe.apiml.zaas.keys.unknown")).thenReturn(mockApiMessage);
        when(mockApiMessage.mapToApiMessage()).thenReturn(expectedApiMessage);

        var result = controller.getPublicKeyUsedForSigning();

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
