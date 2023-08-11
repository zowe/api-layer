package org.zowe.apiml.cloudgatewayservice.controller;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.cloudgatewayservice.security.AuthSourceSign;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.text.ParseException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@WebFluxTest(controllers = WellKnownRestController.class, excludeAutoConfiguration = {ReactiveSecurityAutoConfiguration.class})
class WellKnownRestControllerTest {
    private PublicKey publicKey;

    @Autowired
    WebTestClient webTestClient;

    @MockBean
    private AuthSourceSign mockAuthSourceSign;

    @Nested
    class GivenSinglePublicKeyIsAvailable {

        @BeforeEach
        void setUp() throws NoSuchAlgorithmException {
            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            publicKey = keyPair.getPublic();
            when(mockAuthSourceSign.getPublicKey()).thenReturn(publicKey);
        }

        @Test
        void thenJWKSetIsProvided() throws IOException, ParseException, JOSEException {
            FluxExchangeResult<String> result =
                webTestClient.get()
                    .uri("/gateway/.well-known/jwks.json")
                    .accept(MediaType.ALL)
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult(String.class);
            verify(mockAuthSourceSign, times(1)).getPublicKey();
            assertNotNull(result);
            InputStream jsonBody = new ByteArrayInputStream(result.getResponseBodyContent());
            List<JWK> keys = JWKSet.load(jsonBody).getKeys();
            assertEquals(1, keys.size());
            PublicKey resultPublicKey = keys.get(0).toRSAKey().toPublicKey();
            assertEquals(publicKey, resultPublicKey);
        }
    }

    @Nested
    class GivenNoPublicKeyExists {

        @BeforeEach
        void setUp() {
            when(mockAuthSourceSign.getPublicKey()).thenReturn(null);
        }

        @Test
        void thenReturnEmptyJWKSet() throws IOException, ParseException, JOSEException {
            FluxExchangeResult<String> result =
                webTestClient.get()
                    .uri("/gateway/.well-known/jwks.json")
                    .accept(MediaType.ALL)
                    .exchange()
                    .expectStatus().isOk()
                    .returnResult(String.class);
            verify(mockAuthSourceSign, times(1)).getPublicKey();
            assertNotNull(result);
            InputStream jsonBody = new ByteArrayInputStream(result.getResponseBodyContent());
            List<JWK> keys = JWKSet.load(jsonBody).getKeys();
            assertEquals(0, keys.size());
        }
    }
}
