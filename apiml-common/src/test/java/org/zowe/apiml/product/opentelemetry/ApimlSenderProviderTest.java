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

import io.opentelemetry.exporter.internal.grpc.GrpcSenderConfig;
import io.opentelemetry.exporter.internal.http.HttpSenderConfig;
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
import javax.net.ssl.X509TrustManager;

import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApimlSenderProviderTest {

    private ApimlSenderProvider apimlSenderProvider;

    @Mock
    private HttpConfig httpConfig;
    @Mock
    private HttpsFactory httpsFactory;
    @Mock
    private HttpSenderConfig senderHttpConfig;
    @Mock
    private GrpcSenderConfig<?> senderGrcpConfig;

    private HttpConfig oldValue;

    @BeforeEach
    void setUp() {
        apimlSenderProvider = new ApimlSenderProvider();
        oldValue = (HttpConfig) ReflectionTestUtils.getField(ApimlOpenTelemetryConfiguration.class, "httpConfig2");
        ReflectionTestUtils.setField(ApimlOpenTelemetryConfiguration.class, "httpConfig2", httpConfig);
    }

    @AfterEach
    void tearDown() {
        ReflectionTestUtils.setField(ApimlOpenTelemetryConfiguration.class, "httpConfig2", oldValue);
    }

    @Nested
    class OnSSLConfigured {

        @Mock
        private SSLContext sslContext;
        @Mock
        private X509TrustManager x509TrustManager;

        @BeforeEach
        void setUp() {
            when(httpConfig.httpsFactory()).thenReturn(httpsFactory);
        }

        @Test
        void testCreate_https() {
            var client = apimlSenderProvider.createSender(senderHttpConfig);

        }

        @Test
        void testCreate_Grpc() {
            var client = apimlSenderProvider.createSender(senderGrcpConfig);
        }

    }

    @Nested
    class OnSSLNotConfigured {

        @BeforeEach
        void setUp() {
            when(httpConfig.httpsFactory()).thenReturn(null);
        }

        @Test
        void testCreate_https() {
            var client = apimlSenderProvider.createSender(senderHttpConfig);


        }

        @Test
        void testCreate_Grcp() {
            var client = apimlSenderProvider.createSender(senderGrcpConfig);
        }

    }

}
