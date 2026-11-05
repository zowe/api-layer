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

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
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
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.HttpsFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import java.net.URI;
import java.util.Collection;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ApimlSenderProviderTest {

    private static final String INITIALIZED_HTTP_CONFIG = "initializedHttpConfig";
    private static final String COLLECTOR_ENDPOINT = "https://localhost:4018";
    private static final String GRPC_METHOD = "opentelemetry.proto.collector.metrics.v1.MetricsService/Export";
    private static final List<String> ENABLED_PROTOCOLS = List.of("TLSv1.3");

    // Positions of the OkHttpHttpSender constructor arguments (the sender is created by calling the constructor directly)
    private static final int HTTP_SENDER_ARGS_COUNT = 13;
    private static final int HTTP_SENDER_SSL_CONTEXT = 8;
    private static final int HTTP_SENDER_TRUST_MANAGER = 9;
    private static final int HTTP_SENDER_ENABLED_PROTOCOLS = 12;

    // Positions of the OkHttpGrpcSender constructor arguments
    private static final int GRPC_SENDER_ARGS_COUNT = 11;
    private static final int GRPC_SENDER_URL = 0;
    private static final int GRPC_SENDER_SSL_CONTEXT = 6;
    private static final int GRPC_SENDER_TRUST_MANAGER = 7;
    private static final int GRPC_SENDER_ENABLED_PROTOCOLS = 10;

    private ApimlSenderProvider apimlSenderProvider;

    @Mock
    private HttpConfig httpConfig;
    @Mock
    private HttpsFactory httpsFactory;
    @Mock
    private HttpSenderConfig senderHttpConfig;
    @Mock
    private GrpcSenderConfig senderGrpcConfig;

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

    /**
     * Creates an HTTP sender with the tested provider and returns the arguments passed to the OkHttpHttpSender constructor.
     */
    private List<Object> createHttpSenderAndGetConstructorArgs() {
        var constructorArgs = new ArrayList<Object>();
        try (var ignored = mockConstruction(OkHttpHttpSender.class, (mock, context) -> constructorArgs.addAll(context.arguments()))) {
            apimlSenderProvider.createSender(senderHttpConfig);
        }
        return constructorArgs;
    }

    /**
     * Creates a gRPC sender with the tested provider and returns the arguments passed to the OkHttpGrpcSender constructor.
     */
    private List<Object> createGrpcSenderAndGetConstructorArgs() {
        when(senderGrpcConfig.getEndpoint()).thenReturn(URI.create(COLLECTOR_ENDPOINT));
        when(senderGrpcConfig.getFullMethodName()).thenReturn(GRPC_METHOD);

        var constructorArgs = new ArrayList<Object>();
        try (var ignored = mockConstruction(OkHttpGrpcSender.class, (mock, context) -> constructorArgs.addAll(context.arguments()))) {
            apimlSenderProvider.createSender(senderGrpcConfig);
        }
        return constructorArgs;
    }

    @Nested
    class WhenSslConfigured {

        @Mock
        private SSLContext sslContext;
        @Mock
        private X509TrustManager x509TrustManager;

        @BeforeEach
        void setUp() {
            when(httpConfig.httpsFactory()).thenReturn(httpsFactory);
            when(httpsFactory.getSslContext()).thenReturn(sslContext);
            when(httpsFactory.getTrustManagers()).thenReturn((Collection<TrustManager>) (Collection<?>) List.of(x509TrustManager));
        }

        @Test
        void httpSenderUsesSslContextTrustManagerAndProtocolsFromConfiguration() {
            when(senderHttpConfig.getEnabledProtocols()).thenReturn(ENABLED_PROTOCOLS);

            var args = createHttpSenderAndGetConstructorArgs();

            assertEquals(HTTP_SENDER_ARGS_COUNT, args.size());
            assertSame(sslContext, args.get(HTTP_SENDER_SSL_CONTEXT));
            assertSame(x509TrustManager, args.get(HTTP_SENDER_TRUST_MANAGER));
            assertSame(ENABLED_PROTOCOLS, args.get(HTTP_SENDER_ENABLED_PROTOCOLS));
        }

        @Test
        void grpcSenderUsesSslContextTrustManagerAndProtocolsFromConfiguration() {
            when(senderGrpcConfig.getEnabledProtocols()).thenReturn(ENABLED_PROTOCOLS);

            var args = createGrpcSenderAndGetConstructorArgs();

            assertEquals(GRPC_SENDER_ARGS_COUNT, args.size());
            assertSame(sslContext, args.get(GRPC_SENDER_SSL_CONTEXT));
            assertSame(x509TrustManager, args.get(GRPC_SENDER_TRUST_MANAGER));
            assertSame(ENABLED_PROTOCOLS, args.get(GRPC_SENDER_ENABLED_PROTOCOLS));
        }

        @Test
        void grpcSenderIsCreatedWithFullUrlOfCollectorAndMethod() {
            var args = createGrpcSenderAndGetConstructorArgs();

            assertEquals(COLLECTOR_ENDPOINT + "/" + GRPC_METHOD, args.get(GRPC_SENDER_URL));
        }

    }

    @Nested
    class WhenSenderCreationFails {

        private final Logger logger = (Logger) LoggerFactory.getLogger(ApimlSenderProvider.class);
        private final ListAppender<ILoggingEvent> logAppender = new ListAppender<>();

        @BeforeEach
        void setUp() {
            when(httpConfig.httpsFactory()).thenReturn(httpsFactory);
            logAppender.start();
            logger.addAppender(logAppender);
        }

        @AfterEach
        void tearDown() {
            logger.detachAppender(logAppender);
            logAppender.stop();
        }

        @Test
        void httpSenderLogsErrorAndRethrowsOriginalException() {
            var failure = new IllegalArgumentException("Invalid HTTP exporter endpoint");
            when(senderHttpConfig.getEndpoint()).thenThrow(failure);

            var thrown = assertThrows(RuntimeException.class, () -> apimlSenderProvider.createSender(senderHttpConfig));

            assertSame(failure, thrown);
            assertErrorLogged("Unable to create OpenTelemetry http exporter: " + failure.getMessage());
        }

        @Test
        void grpcSenderLogsErrorAndRethrowsOriginalException() {
            var failure = new IllegalArgumentException("Invalid gRPC exporter endpoint");
            when(senderGrpcConfig.getEndpoint()).thenThrow(failure);

            var thrown = assertThrows(RuntimeException.class, () -> apimlSenderProvider.createSender(senderGrpcConfig));

            assertSame(failure, thrown);
            assertErrorLogged("Unable to create OpenTelemetry grpc exporter: " + failure.getMessage());
        }

        private void assertErrorLogged(String expectedMessage) {
            assertEquals(1, logAppender.list.size());
            var event = logAppender.list.get(0);
            assertEquals(Level.ERROR, event.getLevel());
            assertEquals(expectedMessage, event.getFormattedMessage());
        }
    }

    @Nested
    class WhenSslNotConfigured {

        @BeforeEach
        void setUp() {
            when(httpConfig.httpsFactory()).thenReturn(httpsFactory);
        }

        @Test
        void httpSenderIsCreatedWithoutSslContextTrustManagerAndProtocols() {
            when(senderHttpConfig.getEnabledProtocols()).thenReturn(null);

            var args = createHttpSenderAndGetConstructorArgs();

            assertEquals(HTTP_SENDER_ARGS_COUNT, args.size());
            assertNull(args.get(HTTP_SENDER_SSL_CONTEXT));
            assertNull(args.get(HTTP_SENDER_TRUST_MANAGER));
            assertNull(args.get(HTTP_SENDER_ENABLED_PROTOCOLS));
        }

        @Test
        void grpcSenderIsCreatedWithoutSslContextTrustManagerAndProtocols() {
            when(senderGrpcConfig.getEnabledProtocols()).thenReturn(null);

            var args = createGrpcSenderAndGetConstructorArgs();

            assertEquals(GRPC_SENDER_ARGS_COUNT, args.size());
            assertNull(args.get(GRPC_SENDER_SSL_CONTEXT));
            assertNull(args.get(GRPC_SENDER_TRUST_MANAGER));
            assertNull(args.get(GRPC_SENDER_ENABLED_PROTOCOLS));
        }

        @Test
        void grpcSenderIsCreatedWithFullUrlOfCollectorAndMethod() {
            var args = createGrpcSenderAndGetConstructorArgs();

            assertEquals(COLLECTOR_ENDPOINT + "/" + GRPC_METHOD, args.get(GRPC_SENDER_URL));
        }

    }

}
