/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security;

import com.sun.net.httpserver.HttpServer;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.socket.ConnectionSocketFactory;
import org.apache.hc.client5.http.ssl.DefaultHostnameVerifier;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.security.KeyStoreException;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class HttpsFactoryTest {

    private static final String INCORRECT_PARAMETER_VALUE = "WRONG";

    private HttpsConfig.HttpsConfigBuilder httpsConfigBuilder;

    @BeforeEach
    void setUp() {
        httpsConfigBuilder = SecurityTestUtils.correctHttpsSettings();
    }

    @Test
    void shouldCreateSecureSslSocketFactory() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        ConnectionSocketFactory socketFactory = httpsFactory.createSslSocketFactory();
        assertEquals(SSLConnectionSocketFactory.class, socketFactory.getClass());
    }

    @Test
    void shouldCreateIgnoringSslSocketFactory() throws KeyStoreException {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        ConnectionSocketFactory socketFactory = httpsFactory.createSslSocketFactory();
        assertEquals(SSLConnectionSocketFactory.class, socketFactory.getClass());
        assertFalse(httpsFactory.getUsedKeyStore().aliases().hasMoreElements());
    }

    @Test
    void shouldCreateSecureSslContextWithEmptyKeystoreWhenNoKeystoreIsProvided() throws KeyStoreException {
        HttpsConfig httpsConfig = HttpsConfig.builder().protocol("TLSv1.2").verifySslCertificatesOfServices(true).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        httpsFactory.getSslContext();
        assertFalse(httpsFactory.getUsedKeyStore().aliases().hasMoreElements());
    }

    @Test
    void shouldCreateSecureHttpClient() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");

        var httpClient = httpsFactory.buildHttpClient(null);
        assertEquals("org.apache.hc.client5.http.impl.classic.InternalHttpClient", httpClient.getClass().getName());
    }

    @Test
    void shouldCreateSecureSslContext() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        SSLContext sslContext = httpsFactory.getSslContext();
        assertNotNull(sslContext);
        assertEquals(SSLContext.class, sslContext.getClass());
    }

    @Test
    void shouldCreateIgnoringSslContext() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        SSLContext sslContext = httpsFactory.getSslContext();
        assertNotNull(sslContext);
        assertEquals(SSLContext.class, sslContext.getClass());
    }

    @Test
    void wrongKeyPasswordConfigurationShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.keyPassword(INCORRECT_PARAMETER_VALUE.toCharArray()).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        assertThrows(HttpsConfigError.class, () -> httpsFactory.getSslContext());
    }

    @Test
    void specificIncorrectAliasShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.trustStorePassword(INCORRECT_PARAMETER_VALUE.toCharArray()).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        assertThrows(HttpsConfigError.class, () -> httpsFactory.getSslContext());
    }

    @Test
    void incorrectProtocolShouldFail() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).protocol(INCORRECT_PARAMETER_VALUE).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        assertThrows(HttpsConfigError.class, () -> httpsFactory.getSslContext());
    }

    @Test
    void shouldCreateDefaultHostnameVerifier() {
        HttpsConfig httpsConfig = httpsConfigBuilder.build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        HostnameVerifier hostnameVerifier = httpsFactory.getHostnameVerifier();
        assertEquals(DefaultHostnameVerifier.class, hostnameVerifier.getClass());
    }

    @Test
    void shouldCreateNoopHostnameVerifier() {
        HttpsConfig httpsConfig = httpsConfigBuilder.verifySslCertificatesOfServices(false).build();
        HttpsFactory httpsFactory = new HttpsFactory(httpsConfig, "0.0.0.0");
        HostnameVerifier hostnameVerifier = httpsFactory.getHostnameVerifier();
        assertEquals(NoopHostnameVerifier.class, hostnameVerifier.getClass());
    }

    @Nested
    class OverrideLocalAddress {

        private static final String OWNED_ADDRESS = "127.0.0.1";
        private static final String NOT_OWNED_ADDRESS = "192.0.2.1";

        @Mock
        private NetworkInterface interface0;
        @Mock
        private NetworkInterface interface1;
        @Mock
        private InterfaceAddress interfaceAddress0;
        @Mock
        private InterfaceAddress interfaceAddress1;

        private HttpServer localServer;

        @BeforeEach
        void setUp() throws IOException {
            lenient().when(interface0.getInterfaceAddresses()).thenReturn(List.of(interfaceAddress0));
            lenient().when(interface1.getInterfaceAddresses()).thenReturn(List.of(interfaceAddress1));
            lenient().when(interfaceAddress0.getAddress()).thenReturn(InetAddress.getByName(NOT_OWNED_ADDRESS));
            lenient().when(interfaceAddress1.getAddress()).thenReturn(InetAddress.getByName(OWNED_ADDRESS));

            localServer = HttpServer.create(new InetSocketAddress(OWNED_ADDRESS, 0), 0);
            localServer.createContext("/", exchange -> {
                exchange.sendResponseHeaders(200, -1);
                exchange.close();
            });
            localServer.start();
        }

        @AfterEach
        void tearDown() {
            localServer.stop(0);
        }

        @Test
        void givenSpecificAddress_whenHttpClientConnects_thenItBindsToTheMatchingInterfaceAndConnects() {
            try (
                var mockedNetworkInterface = mockStatic(NetworkInterface.class);
            ) {
                mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenReturn(Collections.enumeration(List.of(interface0, interface1)));

                var httpClient = buildHttpClient(OWNED_ADDRESS);
                var status = connect(httpClient);
                assertEquals(200, status);
            }
        }

        private int connect(CloseableHttpClient httpClient) {
            var get = new HttpGet("http://" + OWNED_ADDRESS + ":" + localServer.getAddress().getPort() + "/");
            try {
                return httpClient.execute(get, r -> {
                    return r.getCode();
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Test
        void givenAddressNotOwnedByThisHost_whenHttpClientConnects_thenBindFails() {
            try (
                var mockedNetworkInterface = mockStatic(NetworkInterface.class);
            ) {
                mockedNetworkInterface.when(NetworkInterface::getNetworkInterfaces)
                    .thenReturn(Collections.enumeration(List.of(interface0, interface1)));

                var httpClient = buildHttpClient(NOT_OWNED_ADDRESS);

                var connectionAttempt = assertThrows(Exception.class, () -> connect(httpClient));

                assertInstanceOf(BindException.class, ExceptionUtils.getRootCause(connectionAttempt));
            }
        }

        private CloseableHttpClient buildHttpClient(String selectedAddress) {
            var httpsConfig = httpsConfigBuilder.build();
            var httpsFactory = new HttpsFactory(httpsConfig, selectedAddress);

            var httpClient = httpsFactory.buildHttpClient(null);
            assertEquals("org.apache.hc.client5.http.impl.classic.InternalHttpClient", httpClient.getClass().getName());

            return httpClient;
        }

    }

}
