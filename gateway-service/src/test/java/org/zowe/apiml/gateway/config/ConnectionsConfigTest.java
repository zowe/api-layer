/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import io.netty.handler.ssl.util.KeyManagerFactoryWrapper;
import org.apache.hc.client5.http.io.HttpClientConnectionManager;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.invocation.InvocationOnMock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilter;
import org.zowe.apiml.gateway.GatewayServiceApplication;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.common.util.ConnectionUtil;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ConnectionsConfigTest {

    @Nested
    @SpringBootTest
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    @ActiveProfiles("test")
    class WhenCreateEurekaJerseyClientBuilder {

        @Autowired
        private ConnectionsConfig connectionsConfig;

        @Test
        void thenIsNotNull() {
            assertThat(connectionsConfig).isNotNull();
        }

    }

    @Nested
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.port=-1"},
        classes = {GatewayServiceApplication.class, ConnectionsConfigTest.SslDetectorConfig.class}
    )
    @ActiveProfiles("test")
    class ChooseAlias {

        @LocalServerPort
        protected int port;

        @Nested
        class UsingX509KeyManagerSelectedAlias {

            @Value("${server.ssl.keyAlias}")
            private String keyAlias;

            @MockitoSpyBean
            private ConnectionsConfig connectionsConfig;

            @Autowired
            private HttpConfig httpConfig;

            @Test
            void whenAliasIsSet_thenReturnItByX509KeyManagerSelectedAlias() throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
                AtomicReference<X509KeyManager> returnValue = new AtomicReference<>();
                try (MockedStatic<ConnectionUtil> connectionUtilMockedStatic = mockStatic(ConnectionUtil.class, InvocationOnMock::callRealMethod)) {
                    connectionUtilMockedStatic.when(() -> ConnectionUtil.x509KeyManagerSelectedAlias(any(), any())).then(answer -> {
                        if (returnValue.get() == null) {
                            returnValue.set(spy((X509KeyManager) answer.callRealMethod()));
                        }
                        return returnValue.get();
                    });

                    var sslContext = ConnectionUtil.getSslContext(httpConfig, true);
                    var sslProvider = SslProvider.builder().sslContext(sslContext).build();
                    var httpClient = HttpClient.create().secure(sslProvider);
                    reset(returnValue.get());
                    httpClient.get()
                        .uri(String.format("https://localhost:%d/", port))
                        .response().block();
                    assertNotNull(SslDetectorConfig.sslInfoHolder.get());

                    verify(returnValue.get(), atLeastOnce()).chooseClientAlias(any(), any(), any());
                    assertEquals(keyAlias, returnValue.get().chooseClientAlias(null, null, null));
                }
            }

        }

        @Nested
        class Negative {

            @MockitoSpyBean
            private HttpConfig httpConfig;

            @Test
            void whenAliasIsInvalid_thenNoCertificateProvided() throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
                when(httpConfig.getKeyAlias()).thenReturn("invalid");

                var sslContext = ConnectionUtil.getSslContext(httpConfig, true);
                var sslProvider = SslProvider.builder().sslContext(sslContext).build();
                var httpClient = HttpClient.create().secure(sslProvider);
                httpClient.get()
                    .uri(String.format("https://localhost:%d/", port))
                    .response().block();

                assertNull(SslDetectorConfig.sslInfoHolder.get());
            }

        }

        @Nested
        class Wrapper {

            private static final String CONFIG_ALIAS = "configAlias";
            private static final String ALIAS = "alias";
            private static final String[] ALIASES = new String[]{"alias"};
            private static final String KEY_TYPE = "keyType";
            private static final String[] KEY_TYPES = new String[]{KEY_TYPE};
            private static final Principal[] ISSUERS = new Principal[0];
            private static final Socket SOCKET = mock(Socket.class);
            private static final X509Certificate[] CERTIFICATES = new X509Certificate[0];
            private static final PrivateKey PRIVATE_KEY = mock(PrivateKey.class);

            private final X509KeyManager origKeyManager = mock(X509KeyManager.class);
            private final KeyManagerFactory origKeyManagerFactory = new KeyManagerFactoryWrapper(origKeyManager);

            @Test
            void whenGetClientAliases_thenRecall() {
                doReturn(ALIASES).when(origKeyManager).getClientAliases(KEY_TYPE, ISSUERS);
                assertSame(ALIASES,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getClientAliases(KEY_TYPE, ISSUERS)
                );
                verify(origKeyManager).getClientAliases(KEY_TYPE, ISSUERS);
            }

            @Test
            void givenNoAlias_whenChooseClientAlias_thenRecall() {
                doReturn(ALIAS).when(origKeyManager).chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET);
                assertSame(ALIAS,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, null)
                        .chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET)
                );
                verify(origKeyManager).chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET);
            }

            @Test
            void givenAlias_whenChooseClientAlias_thenReturnAlias() {
                assertSame(CONFIG_ALIAS,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET)
                );
                verify(origKeyManager, never()).chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET);
            }

            @Test
            void whenGetServerAliases_thenRecall() {
                doReturn(ALIASES).when(origKeyManager).getServerAliases(KEY_TYPE, ISSUERS);
                assertSame(ALIASES,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getServerAliases(KEY_TYPE, ISSUERS)
                );
                verify(origKeyManager).getServerAliases(KEY_TYPE, ISSUERS);
            }

            @Test
            void givenNoAlias_whenChooseServerAlias_thenRecall() {
                doReturn(ALIAS).when(origKeyManager).chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET);
                assertSame(ALIAS,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, null)
                        .chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET)
                );
                verify(origKeyManager).chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET);
            }

            @Test
            void givenAlias_whenChooseServerAlias_thenReturnAlias() {
                assertSame(CONFIG_ALIAS,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET)
                );
                verify(origKeyManager, never()).chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET);
            }

            @Test
            void whenGetCertificateChain_thenRecall() {
                doReturn(CERTIFICATES).when(origKeyManager).getCertificateChain(ALIAS);
                assertSame(CERTIFICATES,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getCertificateChain(ALIAS)
                );
                verify(origKeyManager).getCertificateChain(ALIAS);
            }

            @Test
            void whenGetPrivateKey_thenRecall() {
                doReturn(PRIVATE_KEY).when(origKeyManager).getPrivateKey(ALIAS);
                assertSame(PRIVATE_KEY,
                    new ConnectionUtil.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getPrivateKey(ALIAS)
                );
                verify(origKeyManager).getPrivateKey(ALIAS);
            }

        }

    }

    static class SslDetectorConfig {

        static final AtomicReference<SslInfo> sslInfoHolder = new AtomicReference<>();

        @Bean
        WebFilter sslDetector() {
            return (exchange, chain) -> {
                sslInfoHolder.set(exchange.getRequest().getSslInfo());
                return chain.filter(exchange);
            };
        }

    }

    @Nested
    @SpringBootTest(
        properties = {"apiml.service.corsEnabled=true"}
    )
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    @ActiveProfiles("test")
    class GivenCorsEnabled {

        @Nested
        public class WhenCorsAllowedMethodsIsNotSet {

            @Autowired
            private ConnectionsConfig connectionsConfig;

            @Test
            void validateDefaultCors() {
                var corsUtils = connectionsConfig.corsUtils();

                @SuppressWarnings("unchecked")
                var corsAllowedMethods = (List<String>) ReflectionTestUtils.getField(corsUtils, "defaultAllowedCorsHttpMethods");
                assertEquals(7, corsAllowedMethods.size());
                var allowedCredentials = (boolean) ReflectionTestUtils.getField(corsUtils, "defaultAllowCredentials");
                assertTrue(allowedCredentials);

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
            void validateCorsAllowedMethods() {
                var corsUtils = connectionsConfig.corsUtils();

                @SuppressWarnings("unchecked")
                var corsAllowedMethods = (List<String>) ReflectionTestUtils.getField(corsUtils, "defaultAllowedCorsHttpMethods");

                assertEquals(3, corsAllowedMethods.size());
                assertEquals("GET", corsAllowedMethods.get(0));
                assertEquals("POST", corsAllowedMethods.get(1));
                assertEquals("PATCH", corsAllowedMethods.get(2));
            }

        }

    }

    @Nested
    class AdditionalRegistrationBasicAuthFallback {

        private String withBasicAuthFallback(boolean verify, String url) {
            HttpConfig httpConfig = mock(HttpConfig.class);
            doReturn(verify).when(httpConfig).isVerifySslCertificatesOfServices();
            var connectionsConfig = new ConnectionsConfig(null, httpConfig, Collections.emptyList());
            ReflectionTestUtils.setField(connectionsConfig, "discoveryUserid", "eureka");
            ReflectionTestUtils.setField(connectionsConfig, "discoveryPassword", "password".toCharArray());
            return ReflectionTestUtils.invokeMethod(connectionsConfig, "withBasicAuthFallback", url);
        }

        @Test
        void givenVerificationDisabled_thenCredentialsAreEmbedded() {
            assertEquals("https://eureka:password@localhost:10011/eureka/",
                withBasicAuthFallback(false, "https://localhost:10011/eureka/"));
        }

        @Test
        void givenVerificationDisabledAndMultipleUrls_thenAllAreRewritten() {
            assertEquals("https://eureka:password@host1:10011/eureka/,https://eureka:password@host2:10011/eureka/",
                withBasicAuthFallback(false, "https://host1:10011/eureka/,https://host2:10011/eureka/"));
        }

        @Test
        void givenVerificationEnabled_thenUrlIsUnchanged() {
            assertEquals("https://localhost:10011/eureka/",
                withBasicAuthFallback(true, "https://localhost:10011/eureka/"));
        }
    }

}

