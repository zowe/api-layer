/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.config;

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.discovery.EurekaClientConfig;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import io.netty.handler.ssl.SslContextBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.web.server.Ssl;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.util.CorsUtils;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.io.IOException;
import java.lang.reflect.Field;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ComponentScan(basePackages = "org.zowe.apiml.cloudgatewayservice")
@ExtendWith(MockitoExtension.class)
class ConnectionsConfigTest {

    @Autowired
    private ConnectionsConfig connectionsConfig;

    @Autowired
    private RoutingConfig routingConfig;

    @Nested
    class WhenCreateEurekaJerseyClientBuilder {
        @Test
        void thenIsNotNull() {
            assertThat(connectionsConfig).isNotNull();
            assertThat(connectionsConfig.getEurekaJerseyClient()).isNotNull();
        }
    }

    @Nested
    class WhenInitializeEurekaClient {
        @Mock
        private ApplicationInfoManager manager;

        @Mock
        private EurekaClientConfig config;

        @Mock
        private EurekaJerseyClient eurekaJerseyClient;

        @Mock
        private HealthCheckHandler healthCheckHandler;

        @Test
        void thenCreateIt() {
            assertThat(connectionsConfig.primaryEurekaClient(manager, config, eurekaJerseyClient, healthCheckHandler)).isNotNull();
        }
    }

    @Nested
    class KeyringFormatAndPasswordUpdate {

        ApplicationContext context;

        @BeforeEach
        void setup() {
            context = mock(ApplicationContext.class);
            ServerProperties properties = new ServerProperties();
            properties.setSsl(new Ssl());
            when(context.getBean(ServerProperties.class)).thenReturn(properties);
        }

        @Test
        void whenKeyringHasWrongFormatAndMissingPasswords_thenFixIt() {
            ConnectionsConfig connectionsConfig = new ConnectionsConfig(null);
            ReflectionTestUtils.setField(connectionsConfig, "keyStorePath", "safkeyring:///userId/ringId1");
            ReflectionTestUtils.setField(connectionsConfig, "trustStorePath", "safkeyring:////userId/ringId2");
            ReflectionTestUtils.setField(connectionsConfig, "context", context);
            connectionsConfig.updateConfigParameters();

            assertThat(ReflectionTestUtils.getField(connectionsConfig, "keyStorePath")).isEqualTo("safkeyring://userId/ringId1");
            assertThat(ReflectionTestUtils.getField(connectionsConfig, "trustStorePath")).isEqualTo("safkeyring://userId/ringId2");
            assertThat((char[]) ReflectionTestUtils.getField(connectionsConfig, "keyStorePassword")).isEqualTo("password".toCharArray());
            assertThat((char[]) ReflectionTestUtils.getField(connectionsConfig, "trustStorePassword")).isEqualTo("password".toCharArray());
        }

        @Test
        void whenKeystore_thenDoNothing() {
            ConnectionsConfig connectionsConfig = new ConnectionsConfig(null);
            ReflectionTestUtils.setField(connectionsConfig, "keyStorePath", "/path1");
            ReflectionTestUtils.setField(connectionsConfig, "trustStorePath", "/path2");
            ReflectionTestUtils.setField(connectionsConfig, "context", context);
            connectionsConfig.updateConfigParameters();

            assertThat(ReflectionTestUtils.getField(connectionsConfig, "keyStorePath")).isEqualTo("/path1");
            assertThat(ReflectionTestUtils.getField(connectionsConfig, "trustStorePath")).isEqualTo("/path2");
            assertThat(ReflectionTestUtils.getField(connectionsConfig, "keyStorePassword")).isNull();
            assertThat(ReflectionTestUtils.getField(connectionsConfig, "trustStorePassword")).isNull();
        }
    }

    @ExtendWith(MockitoExtension.class)
    @Nested
    class ConnectionUtilTest {

        @Mock
        private HttpConfig httpConfig;
        @Mock
        private SslContextBuilder builder;

        @BeforeEach
        void setUp() {
            when(httpConfig.getTrustStoreType()).thenReturn("PKCS12");
            when(httpConfig.getTrustStore()).thenReturn("../keystore/localhost/localhost.truststore.p12");
            when(httpConfig.getTrustStorePassword()).thenReturn("password".toCharArray()); //NOSONAR
        }

        @Test
        void onGetSslContextWithKeystore_thenUse() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            when(httpConfig.getKeyStoreType()).thenReturn("PKCS12");
            when(httpConfig.getKeyStore()).thenReturn("../keystore/localhost/localhost.keystore.p12");
            when(httpConfig.getKeyStorePassword()).thenReturn("password".toCharArray()); //NOSONAR

            try (MockedStatic<SslContextBuilder> sslContextBuilder = Mockito.mockStatic(SslContextBuilder.class)) {
                sslContextBuilder.when(SslContextBuilder::forClient).thenReturn(builder);

                connectionsConfig.sslContext(true);

                verify(builder, times(1)).trustManager(any(TrustManagerFactory.class));
                verify(builder, never()).endpointIdentificationAlgorithm(any());
                verify(builder, times(1)).keyManager(any(X509KeyManager.class));
                verify(builder, times(1)).keyManager((X509KeyManager) argThat(x509KeyManager -> {
                    X509KeyManager m = (X509KeyManager) x509KeyManager;
                    assertNotNull(m.getPrivateKey("localhost"));
                    assertTrue(m.getCertificateChain("localhost").length > 0);
                    return true;
                }));

            }

        }

        @Test
        void onGetSslContextWithoutKeystore_thenEmpty() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            try (MockedStatic<SslContextBuilder> sslContextBuilder = Mockito.mockStatic(SslContextBuilder.class)) {
                sslContextBuilder.when(SslContextBuilder::forClient).thenReturn(builder);

                connectionsConfig.sslContext(false);

                verify(builder, times(1)).trustManager(any(TrustManagerFactory.class));
                verify(builder, never()).endpointIdentificationAlgorithm(any());
                verify(builder, times(1)).keyManager(any(KeyManagerFactory.class));
                verify(builder, times(1)).keyManager((KeyManagerFactory) argThat(keyManagerFactory -> {
                    KeyManagerFactory f = (KeyManagerFactory) keyManagerFactory;
                    assertTrue(f.getKeyManagers().length > 0);
                    return true;
                }));

            }

        }

        @Test
        void whenNonStrict_thenDisableEndpointIdentificationAlgorithm() throws UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
            when(httpConfig.isVerifySslCertificatesOfServices()).thenReturn(true);
            when(httpConfig.isNonStrictVerifySslCertificatesOfServices()).thenReturn(true);

            try (MockedStatic<SslContextBuilder> sslContextBuilder = Mockito.mockStatic(SslContextBuilder.class)) {
                sslContextBuilder.when(SslContextBuilder::forClient).thenReturn(builder);

                connectionsConfig.sslContext(false);

                verify(builder, times(1)).trustManager(any(TrustManagerFactory.class));
                verify(builder, times(1)).endpointIdentificationAlgorithm(isNull());
            }

        }

    }

    @Nested
    @SpringBootTest(
        properties = {"apiml.service.corsEnabled=true"}
    )
    class GivenCorsEnabled {

        @Nested
        public class WhenCorsAllowedMethodsIsNotSet {

            @Autowired
            private ConnectionsConfig connectionsConfig;

            @Test
            void validateDefaultCorsAllowedMethods() throws NoSuchFieldException, IllegalAccessException {
                CorsUtils corsUtils = connectionsConfig.corsUtils();

                Field field = corsUtils.getClass().getDeclaredField("allowedCorsHttpMethods");
                field.setAccessible(true);
                List<String> corsAllowedMethods = (List<String>) field.get(corsUtils);
                assertEquals(7, corsAllowedMethods.size());
            }
        }

        @Nested
        @TestPropertySource(properties = {
            "apiml.service.corsAllowedMethods=GET,POST, PATCH"
        })
        @DirtiesContext
        public class WhenCorsAllowedMethodsIsSet {

            @Autowired
            private ConnectionsConfig connectionsConfig;

            @Test
            void validateCorsAllowedMethods() throws NoSuchFieldException, IllegalAccessException {
                CorsUtils corsUtils = connectionsConfig.corsUtils();

                Field field = corsUtils.getClass().getDeclaredField("allowedCorsHttpMethods");
                field.setAccessible(true);
                List<String> corsAllowedMethods = (List<String>) field.get(corsUtils);
                assertEquals(3, corsAllowedMethods.size());
                assertEquals("GET", corsAllowedMethods.get(0));
                assertEquals("POST", corsAllowedMethods.get(1));
                assertEquals("PATCH", corsAllowedMethods.get(2));
            }
        }
    }

}

