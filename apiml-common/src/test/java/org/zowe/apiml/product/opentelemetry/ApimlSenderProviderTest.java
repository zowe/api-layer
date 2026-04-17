/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.opentelemetry;

import io.opentelemetry.exporter.sender.okhttp.internal.OkHttpGrpcSender;
import io.opentelemetry.exporter.sender.okhttp.internal.OkHttpHttpSender;
import io.opentelemetry.sdk.common.export.GrpcSenderConfig;
import io.opentelemetry.sdk.common.export.HttpSenderConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URI;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApimlSenderProviderTest {

    private static final String INITIALIZED_HTTP_CONFIG = "initializedHttpConfig";
    private static final String HTTPS_LOCALHOST_4018_ENDPOINT = "https://localhost:4018/endpoint";

    private ApimlSenderProvider apimlSenderProvider;

    @Mock
    private HttpConfig httpConfig;
    @Mock
    private HttpsFactory httpsFactory;
    @Mock
    private HttpSenderConfig senderHttpConfig;
    @Mock
    private GrpcSenderConfig senderGrcpConfig;

    private HttpConfig oldValue;

    @BeforeEach
    void setUp() {
        apimlSenderProvider = new ApimlSenderProvider();
        oldValue = (HttpConfig) ReflectionTestUtils.getField(ApimlOpenTelemetryConfiguration.class, INITIALIZED_HTTP_CONFIG);
        ReflectionTestUtils.setField(ApimlOpenTelemetryConfiguration.class, INITIALIZED_HTTP_CONFIG, httpConfig);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(ApimlOpenTelemetryConfiguration.class, INITIALIZED_HTTP_CONFIG, oldValue);
    }

    @Nested
    class OnSSLConfigured {

        @Mock
        private SSLContext sslContext;
        @Mock
        private X509TrustManager x509TrustManager;
        @Mock
        private SSLSocketFactory socketFactory;

        @BeforeEach
        void setUp() {
            when(httpConfig.httpsFactory()).thenReturn(httpsFactory);
            when(httpsFactory.getSslContext()).thenReturn(sslContext);
            when(httpsFactory.getTrustManagers()).thenReturn((Collection<TrustManager>) (Collection<?>) List.of(x509TrustManager));
        }

        @Test
        void testCreate_https() {
            try (var mockedConstruction = mockConstruction(OkHttpHttpSender.class, (mock, context) -> {
                var args = context.arguments();
                assertEquals(12, args.size());
                assertSame(sslContext, args.get(8));
                assertSame(x509TrustManager, args.get(9));
            })) {
                var client = apimlSenderProvider.createSender(senderHttpConfig);
                assertNotNull(client);
            }
        }

        @Test
        void testCreate_Grpc() {
            when(senderGrcpConfig.getEndpoint()).thenReturn(URI.create(HTTPS_LOCALHOST_4018_ENDPOINT));

            try (var mockedConstruction = mockConstruction(OkHttpGrpcSender.class, (mock, context) -> {
                var args = context.arguments();
                assertEquals(10, args.size());
                assertSame(sslContext, args.get(6));
                assertSame(x509TrustManager, args.get(7));
            })) {
                var client = apimlSenderProvider.createSender(senderGrcpConfig);
                assertNotNull(client);
            }

        }

    }

    @Nested
    class OnSSLNotConfigured {

        @BeforeEach
        void setUp() {
            when(httpConfig.httpsFactory()).thenReturn(httpsFactory);
        }

        @Test
        void testCreate_https() {
            try (var mockedConstruction = mockConstruction(OkHttpHttpSender.class, (mock, context) -> {
                var args = context.arguments();
                assertEquals(12, args.size());
                assertNull(args.get(10));
                assertNull(args.get(10));
            })) {
                var client = apimlSenderProvider.createSender(senderHttpConfig);
                assertNotNull(client);
            }
        }

        @Test
        void testCreate_Grcp() {
            when(senderGrcpConfig.getEndpoint()).thenReturn(URI.create(HTTPS_LOCALHOST_4018_ENDPOINT));

            try (var mockedConstruction = mockConstruction(OkHttpGrpcSender.class, (mock, context) -> {
                var args = context.arguments();
                assertEquals(10, args.size());
                assertNull(args.get(6));
                assertNull(args.get(7));
            })) {
                var client = apimlSenderProvider.createSender(senderGrcpConfig);
                assertNotNull(client);
            }

        }

    }

}
