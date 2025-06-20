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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.zowe.apiml.security.common.token.OIDCProvider;
import org.zowe.apiml.zaas.controllers.AuthController;
import org.zowe.apiml.zaas.security.webfinger.WebFingerProvider;
import org.zowe.apiml.zaas.security.webfinger.WebFingerResponse;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReactiveOIDCControllerTest {

    @Mock private WebFingerProvider webFingerProvider;
    @Mock private OIDCProvider oidcProvider;

    @InjectMocks
    private ReactiveOIDCController controller;

    @Test
    void getWebFinger_disabled() throws IOException {
        String clientId = "testClient";
        when(webFingerProvider.isEnabled()).thenReturn(false);

        Mono<ResponseEntity<Object>> result = controller.getWebFinger(clientId);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NOT_FOUND.equals(responseEntity.getStatusCode()))
            .verifyComplete();
        verify(webFingerProvider, never()).getWebFingerConfig(anyString());
    }

    @Test
    void getWebFinger_enabled_ioException() throws IOException {
        String clientId = "testClient";
        when(webFingerProvider.isEnabled()).thenReturn(true);
        when(webFingerProvider.getWebFingerConfig(clientId)).thenThrow(new IOException("Config read error"));

        var result = controller.getWebFinger(clientId);

        StepVerifier.create(result)
            .expectErrorMatches(InvalidWebFingerConfigurationException.class::isInstance)
            .verify();
    }

    private AuthController.ValidateRequestModel createValidateRequestModel(String token, String serviceId) {
        AuthController.ValidateRequestModel model = new AuthController.ValidateRequestModel();
        model.setToken(token);
        model.setServiceId(serviceId);
        return model;
    }

    @Test
    void validateOIDCToken_valid() {
        var testControllerWithOidc = new ReactiveOIDCController(webFingerProvider, oidcProvider);
        var requestModel = createValidateRequestModel("valid-oidc-token", null);
        when(oidcProvider.isValid("valid-oidc-token")).thenReturn(true);

        Mono<ResponseEntity<Void>> result = testControllerWithOidc.validateOIDCToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.NO_CONTENT.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void validateOIDCToken_invalid() {
        var testControllerWithOidc = new ReactiveOIDCController(webFingerProvider, oidcProvider);
        var requestModel = createValidateRequestModel("invalid-oidc-token", null);
        when(oidcProvider.isValid("invalid-oidc-token")).thenReturn(false);

        Mono<ResponseEntity<Void>> result = testControllerWithOidc.validateOIDCToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void validateOIDCToken_providerNull() {
        var testControllerNoOidc = new ReactiveOIDCController(webFingerProvider, null);
        AuthController.ValidateRequestModel requestModel = createValidateRequestModel("any-token", null);

        Mono<ResponseEntity<Void>> result = testControllerNoOidc.validateOIDCToken(requestModel);

        StepVerifier.create(result)
            .expectNextMatches(responseEntity -> HttpStatus.UNAUTHORIZED.equals(responseEntity.getStatusCode()))
            .verifyComplete();
    }

    @Test
    void getWebFinger_enabled_success() throws IOException {
        var clientId = "testClient";
        var mockResponse = new WebFingerResponse(); // Populate if necessary
        when(webFingerProvider.isEnabled()).thenReturn(true);
        when(webFingerProvider.getWebFingerConfig(clientId)).thenReturn(mockResponse);

        StepVerifier.create(controller.getWebFinger(clientId))
            .expectNextMatches(responseEntity -> {
                assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
                assertEquals(mockResponse, responseEntity.getBody());
                return true;
            })
            .verifyComplete();
    }

}
