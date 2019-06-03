package com.ca.mfaas.gateway.security.service;

import com.ca.mfaas.security.HttpsConfigError;
import io.jsonwebtoken.SignatureAlgorithm;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.ConfigFileApplicationContextInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;


@RunWith(SpringRunner.class)
@TestPropertySource(locations = "/application.yml")
@ContextConfiguration(initializers = ConfigFileApplicationContextInitializer.class)
@Import(JwtSecurityInitializerTest.TestConfig.class)
public class JwtSecurityInitializerTest {

    @Autowired
    private JwtSecurityInitializer jwtSecurityInitializer;


    @Rule
    public final ExpectedException exception = ExpectedException.none();


    @Test
    public void shouldExtractSecretAndPublicKey() {
        jwtSecurityInitializer.init();
        Assert.assertEquals("RSA", jwtSecurityInitializer.getJwtSecret().getAlgorithm());
        Assert.assertEquals("RSA", jwtSecurityInitializer.getJwtPublicKey().getAlgorithm());
        Assert.assertEquals("PKCS#8", jwtSecurityInitializer.getJwtSecret().getFormat());
        Assert.assertEquals("X.509", jwtSecurityInitializer.getJwtPublicKey().getFormat());
    }

    @Test
    public void shouldThrowExceptionIfTheKeysAreNull() {
        jwtSecurityInitializer = new JwtSecurityInitializer();
        exception.expect(HttpsConfigError.class);
        exception.expectMessage("Not found 'null' key alias in the keystore 'null'.");
        jwtSecurityInitializer.init();
    }

    @Test
    public void shouldReturnSignatureAlgorithm() {
        jwtSecurityInitializer.init();
        Assert.assertEquals(SignatureAlgorithm.RS256, jwtSecurityInitializer.getSignatureAlgorithm());
    }

    @SpringBootConfiguration
    public static class TestConfig {

        @Bean
        public JwtSecurityInitializer jwtSecurityInitializer() {
            return new JwtSecurityInitializer();
        }

    }
}

