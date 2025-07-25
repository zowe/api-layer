/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@WebFluxTest(controllers = OidcControllerMicroservice.class, excludeAutoConfiguration = ReactiveSecurityAutoConfiguration.class)
@ContextConfiguration(classes = OidcControllerMicroservice.class)
class OidcControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private OidcControllerMicroservice oidcController;

    @Nested
    class OidcProviders {

        private String[] env = {
            "ZWE_components_gateway_spring_security_oauth2_client_provider_oidc1_authorizationUri",
            "ZWE_components_gateway_spring_security_oauth2_client_registration_oidc2_clientId",
            "ZWE_components_gateway_spring_security_oauth2_client_provider_oidc1_tokenUri"
        };

        Map<String, String> getEnvMap() {
            try {
                Class<?> envVarClass = System.getenv().getClass();
                Field mField = envVarClass.getDeclaredField("m");
                mField.setAccessible(true);
                return (Map<String, String>) mField.get(System.getenv());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                fail(e);
                return null;
            }
        }

        @BeforeEach
        @AfterEach
        void cleanUp() {
            Arrays.stream(env).forEach(k -> getEnvMap().remove(k));
            ((AtomicReference<List<String>>) ReflectionTestUtils.getField(oidcController, "oidcProviderCache")).set(null);
        }

        @Test
        void givenSystemEnv_whenInvokeOidcProviders_thenReturnTheList() {
            Arrays.stream(env).forEach(k -> getEnvMap().put(k, "anyValue"));
            List<String> oidcProviders = webTestClient
                .get().uri("/apicatalog/oidc/provider").exchange()
                .returnResult(List.class).getResponseBody().blockFirst();
            assertEquals(2, oidcProviders.size());
            assertTrue(oidcProviders.contains("oidc1"));
            assertTrue(oidcProviders.contains("oidc2"));
        }

        @Test
        void givenNoSystemEnv_whenInvokeOidcProviders_thenReturnAnEmptyList() {
            List<String> oidcProviders = webTestClient
                .get().uri("/apicatalog/oidc/provider").exchange()
                .returnResult(List.class).getResponseBody().blockFirst();
            assertEquals(0, oidcProviders.size());
        }

    }

}
