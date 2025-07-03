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

import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static io.restassured.module.mockmvc.RestAssuredMockMvc.standaloneSetup;
import static org.junit.jupiter.api.Assertions.*;

class OidcControllerTest {

    private OidcController oidcController = new OidcController();

    @BeforeEach
    void setUp() {
        standaloneSetup(oidcController);
    }

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

        @AfterEach
        void tearDown() {
            Arrays.stream(env).forEach(k -> getEnvMap().remove(k));
        }

        @Test
        void givenSystemEnv_whenInvokeOidcProviders_thenReturnTheList() {
            Arrays.stream(env).forEach(k -> getEnvMap().put(k, "anyValue"));
            List<String> oidcProviders = RestAssuredMockMvc.given()
                .when().get("/oidc/provider")
                .getBody().jsonPath().getList(".");
            assertEquals(2, oidcProviders.size());
            assertTrue(oidcProviders.contains("oidc1"));
            assertTrue(oidcProviders.contains("oidc2"));
        }

        @Test
        void givenNoSystemEnv_whenInvokeOidcProviders_thenReturnAnEmptyList() {
            List<String> oidcProviders = RestAssuredMockMvc.given()
                .when().get("/oidc/provider")
                .getBody().jsonPath().getList(".");
            assertEquals(0, oidcProviders.size());
        }

    }

}
