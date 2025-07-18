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

import com.netflix.appinfo.ApplicationInfoManager;
import com.netflix.appinfo.EurekaInstanceConfig;
import com.netflix.appinfo.HealthCheckHandler;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.discovery.EurekaClientConfig;
import io.netty.handler.ssl.util.KeyManagerFactoryWrapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.reactive.SslInfo;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.WebFilter;
import org.zowe.apiml.gateway.GatewayServiceApplication;
import org.zowe.apiml.product.web.HttpConfig;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.X509KeyManager;
import java.net.MalformedURLException;
import java.net.Socket;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
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
    class WhenCreateEurekaJerseyClientBuilder {

        @Autowired
        private ConnectionsConfig connectionsConfig;

        @Test
        void thenIsNotNull() {
            assertThat(connectionsConfig).isNotNull();
        }

    }

    @Nested
    @SpringBootTest
    @ComponentScan(basePackages = "org.zowe.apiml.gateway")
    class WhenInitializeEurekaClient {

        @Autowired
        private ConnectionsConfig connectionsConfig;

        @Mock
        private ApplicationInfoManager manager;

        @Mock
        private EurekaClientConfig config;

        @Mock
        private HealthCheckHandler healthCheckHandler;

        @Test
        void thenCreateIt() {
            assertThat(connectionsConfig.primaryEurekaClient(manager, config, healthCheckHandler)).isNotNull();
        }

    }

    @Nested
    @SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {"management.port=-1"},
        classes = {GatewayServiceApplication.class, ConnectionsConfigTest.SslDetectorConfig.class}
    )
    class ChooseAlias {

        @LocalServerPort
        protected int port;

        @Nested
        class UsingX509KeyManagerSelectedAlias {

            @Value("${server.ssl.keyAlias}")
            private String keyAlias;

            @MockitoSpyBean
            private ConnectionsConfig connectionsConfig;

            @Test
            void whenAliasIsSet_thenReturnItByX509KeyManagerSelectedAlias() {
                AtomicReference<X509KeyManager> returnValue = new AtomicReference<>();
                doAnswer(answer -> {
                    if (returnValue.get() == null) {
                        returnValue.set(spy((X509KeyManager) answer.callRealMethod()));
                    }
                    return returnValue.get();
                }).when(connectionsConfig).x509KeyManagerSelectedAlias(any());

                var sslContext = connectionsConfig.getSslContext(true);
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

        @Nested
        class Negative {

            @Autowired
            private ConnectionsConfig connectionsConfig;
            @MockitoSpyBean
            private HttpConfig httpConfig;

            @Test
            void whenAliasIsInvalid_thenNoCertificateProvided() {
                when(httpConfig.getKeyAlias()).thenReturn("invalid");

                var sslContext = connectionsConfig.getSslContext(true);
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
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getClientAliases(KEY_TYPE, ISSUERS)
                );
                verify(origKeyManager).getClientAliases(KEY_TYPE, ISSUERS);
            }

            @Test
            void givenNoAlias_whenChooseClientAlias_thenRecall() {
                doReturn(ALIAS).when(origKeyManager).chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET);
                assertSame(ALIAS,
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, null)
                        .chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET)
                );
                verify(origKeyManager).chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET);
            }

            @Test
            void givenAlias_whenChooseClientAlias_thenReturnAlias() {
                assertSame(CONFIG_ALIAS,
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET)
                );
                verify(origKeyManager, never()).chooseClientAlias(KEY_TYPES, ISSUERS, SOCKET);
            }

            @Test
            void whenGetServerAliases_thenRecall() {
                doReturn(ALIASES).when(origKeyManager).getServerAliases(KEY_TYPE, ISSUERS);
                assertSame(ALIASES,
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getServerAliases(KEY_TYPE, ISSUERS)
                );
                verify(origKeyManager).getServerAliases(KEY_TYPE, ISSUERS);
            }

            @Test
            void givenNoAlias_whenChooseServerAlias_thenRecall() {
                doReturn(ALIAS).when(origKeyManager).chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET);
                assertSame(ALIAS,
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, null)
                        .chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET)
                );
                verify(origKeyManager).chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET);
            }

            @Test
            void givenAlias_whenChooseServerAlias_thenReturnAlias() {
                assertSame(CONFIG_ALIAS,
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET)
                );
                verify(origKeyManager, never()).chooseServerAlias(KEY_TYPE, ISSUERS, SOCKET);
            }

            @Test
            void whenGetCertificateChain_thenRecall() {
                doReturn(CERTIFICATES).when(origKeyManager).getCertificateChain(ALIAS);
                assertSame(CERTIFICATES,
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getCertificateChain(ALIAS)
                );
                verify(origKeyManager).getCertificateChain(ALIAS);
            }

            @Test
            void whenGetPrivateKey_thenRecall() {
                doReturn(PRIVATE_KEY).when(origKeyManager).getPrivateKey(ALIAS);
                assertSame(PRIVATE_KEY,
                    new ConnectionsConfig.X509KeyManagerSelectedAlias(origKeyManagerFactory, CONFIG_ALIAS)
                        .getPrivateKey(ALIAS)
                );
                verify(origKeyManager).getPrivateKey(ALIAS);
            }

        }

    }

    @Nested
    class AdditionalRegistration {

        private EurekaInstanceConfig createConfig() {
            var config = mock(EurekaInstanceConfig.class);
            doReturn(30).when(config).getLeaseRenewalIntervalInSeconds();
            doReturn(90).when(config).getLeaseExpirationDurationInSeconds();
            doReturn("namespace").when(config).getNamespace();
            doReturn("serviceId").when(config).getAppname();
            doReturn("instanceId").when(config).getInstanceId();
            doReturn("/").when(config).getHomePageUrlPath();
            doReturn("/application/health").when(config).getHealthCheckUrlPath();
            doReturn("/application/status").when(config).getStatusPageUrlPath();
            doReturn("https://localhost:10010/").when(config).getHomePageUrl();
            doReturn(10010).when(config).getSecurePort();
            doReturn(true).when(config).getSecurePortEnabled();

            return config;
        }

        @Test
        void givenInvalidUrl_whenCreate_thenThrowAnException() {
            var connectionsConfig = new ConnectionsConfig(null, null);
            ReflectionTestUtils.setField(connectionsConfig, "externalUrl", "invalidUrl");
            var e = assertThrows(RuntimeException.class, () -> connectionsConfig.create(createConfig()));
            assertInstanceOf(MalformedURLException.class, e.getCause());
        }

        @Test
        void givenValidInputs_whenCreate_thenCreateIt() {
            var config = createConfig();
            var connectionsConfig = new ConnectionsConfig(null, null);
            ReflectionTestUtils.setField(connectionsConfig, "externalUrl", "https://domain:1234/");

            InstanceInfo instanceInfo = connectionsConfig.create(config);

            assertNotNull(instanceInfo);
            assertEquals("https://domain:1234/", instanceInfo.getHomePageUrl());
            assertEquals("https://domain:1234/application/health", instanceInfo.getSecureHealthCheckUrl());
            assertEquals("https://domain:1234/application/status", instanceInfo.getStatusPageUrl());
        }

        @Test
        void givenMetadataWithUrl_whenCreate_thenUpdateThem() {
            Map<String, String> metadata = new HashMap<>();
            metadata.put("swaggerUrl", "https://localhost:10010/swagger");
            metadata.put("otherKey", "otherValue");

            var config = createConfig();
            doReturn(metadata).when(config).getMetadataMap();

            var connectionsConfig = new ConnectionsConfig(null, null);
            ReflectionTestUtils.setField(connectionsConfig, "externalUrl", "https://domain:1234/");

            InstanceInfo instanceInfo = connectionsConfig.create(config);

            assertNotNull(instanceInfo);
            assertEquals("https://domain:1234/swagger", instanceInfo.getMetadata().get("swaggerUrl"));
            assertEquals("otherValue", instanceInfo.getMetadata().get("otherKey"));
        }

    }

    @Nested
    class ConfigDelegator {

        private final EurekaInstanceConfig eurekaInstanceConfig = mock(EurekaInstanceConfig.class);
        private final InstanceInfo instanceInfo = mock(InstanceInfo.class);
        private final ConnectionsConfig.AdditionalEurekaConfiguration delegator = new ConnectionsConfig.AdditionalEurekaConfiguration(eurekaInstanceConfig, instanceInfo);

        @ParameterizedTest
        @ValueSource(booleans = {true, false})
        void givenDelegator_whenGetHostName_thenCallConfigButReturnInstanceInfo(boolean refresh) {
            doReturn("hostname").when(instanceInfo).getHostName();
            assertEquals("hostname", delegator.getHostName(refresh));
            verify(eurekaInstanceConfig, times(1)).getHostName(refresh);
            verify(instanceInfo, times(1)).getHostName();
        }

        @Test
        void givenUnsecuredConfiguration_whenGetHealthCheckUrl_thenCallGetHealthCheckUrl() {
            doReturn(true).when(instanceInfo).isPortEnabled(InstanceInfo.PortType.UNSECURE);
            doReturn("unsecuredUrl").when(instanceInfo).getHealthCheckUrl();
            assertEquals("unsecuredUrl", delegator.getHealthCheckUrl());
        }

        @Test
        void givenSecuredConfiguration_whenGetHealthCheckUrl_thenCallGetSecureHealthCheckUrl() {
            doReturn(false).when(instanceInfo).isPortEnabled(InstanceInfo.PortType.UNSECURE);
            doReturn("securedUrl").when(instanceInfo).getSecureHealthCheckUrl();
            assertEquals("securedUrl", delegator.getHealthCheckUrl());
        }

        @Test
        void givenDelegator_whenGetSecureHealthCheckUrl_thenCallInstanceInfo() {
            doReturn("securedUrl").when(instanceInfo).getSecureHealthCheckUrl();
            assertEquals("securedUrl", delegator.getSecureHealthCheckUrl());
        }

        @Test
        void givenDelegator_whenGetHomePageUrl_thenCallInstanceInfo() {
            doReturn("homepageUrl").when(instanceInfo).getHomePageUrl();
            assertEquals("homepageUrl", delegator.getHomePageUrl());
        }

        @Test
        void givenDelegator_whenGetStatusPageUrl_thenCallInstanceInfo() {
            doReturn("statuspageUrl").when(instanceInfo).getStatusPageUrl();
            assertEquals("statuspageUrl", delegator.getStatusPageUrl());
        }

    }

    @Configuration
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

}

