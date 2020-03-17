/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.service;

import org.zowe.apiml.security.HttpsConfigError;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(SpringExtension.class)
@TestPropertySource(locations = "/application.yml")
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@Import(JwtSecurityInitializerTest.TestConfig.class)
public class JwtSecurityInitializerTest {

    @Autowired
    private JwtSecurityInitializer jwtSecurityInitializer;


    @Test
    public void shouldExtractSecretAndPublicKey() {
        jwtSecurityInitializer.init();
        assertEquals("RSA", jwtSecurityInitializer.getJwtSecret().getAlgorithm());
        assertEquals("RSA", jwtSecurityInitializer.getJwtPublicKey().getAlgorithm());
        assertEquals("PKCS#8", jwtSecurityInitializer.getJwtSecret().getFormat());
        assertEquals("X.509", jwtSecurityInitializer.getJwtPublicKey().getFormat());
    }

    @Test
    public void shouldThrowExceptionIfTheKeysAreNull() {
        jwtSecurityInitializer = new JwtSecurityInitializer();
        Exception exception = assertThrows(HttpsConfigError.class,
            () -> jwtSecurityInitializer.init(),
            "Expected exception is not HttpsConfigError");

        assertEquals("Not found 'null' key alias in the keystore 'null'.", exception.getMessage());
    }

    @Test
    public void shouldReturnSignatureAlgorithm() {
        jwtSecurityInitializer.init();
        assertEquals(SignatureAlgorithm.RS256, jwtSecurityInitializer.getSignatureAlgorithm());
    }

    @Test
    public void testGetJwkPublicKey() {
        assertEquals("RSA", jwtSecurityInitializer.getJwkPublicKey().getKeyType().getValue());
    }

    @SpringBootConfiguration
    public static class TestConfig {

        @Bean
        public JwtSecurityInitializer jwtSecurityInitializer() {
            return new JwtSecurityInitializer();
        }

    }

}

