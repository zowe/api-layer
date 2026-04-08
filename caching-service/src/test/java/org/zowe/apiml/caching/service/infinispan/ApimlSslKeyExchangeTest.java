/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.infinispan;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.sun.net.httpserver.HttpServer;
import org.jgroups.Event;
import org.jgroups.stack.IpAddress;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLSocketFactory;
import java.net.BindException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ApimlSslKeyExchangeTest {

    private static final IpAddress INVALID_ADDRESS = new IpAddress();

    @Mock
    private Appender<ILoggingEvent> mockedAppender;

    @Captor
    private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

    private ApimlSslKeyExchange apimlSslKeyExchange;

    private Logger logger;

    @BeforeEach
    void setUp() throws Exception {
        logger = (Logger) LoggerFactory.getLogger(ApimlSslKeyExchange.class);
        logger.getLoggerContext().resetTurboFilterList();
        logger.addAppender(mockedAppender);
        logger.setLevel(Level.ERROR);

        apimlSslKeyExchange = createApimlSslKeyExchange();
    }

    private ApimlSslKeyExchange createApimlSslKeyExchange() throws Exception {
        ApimlSslKeyExchange apimlSslKeyExchange = new ApimlSslKeyExchange() {
            @Override
            public Object down(Event evt) {
                return evt.getArg();
            }
        };

        apimlSslKeyExchange.setPortRange(0);
        apimlSslKeyExchange.setKeystoreName("../keystore/localhost/localhost.keystore.p12");
        apimlSslKeyExchange.setKeystorePassword("password");
        apimlSslKeyExchange.setKeystoreType("PKCS12");
        apimlSslKeyExchange.setTruststoreName("../keystore/localhost/localhost.truststore.p12");
        apimlSslKeyExchange.setTruststorePassword("password");
        apimlSslKeyExchange.setTruststoreType("PKCS12");

        apimlSslKeyExchange.setDownProtocol(apimlSslKeyExchange);

        apimlSslKeyExchange.init();

        return apimlSslKeyExchange;
    }

    private String getLogMessage() {
        verify(mockedAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
        List<LoggingEvent> logMessages = loggingEventCaptor.getAllValues();
        assertEquals(1, logMessages.size());
        return logMessages.get(0).getFormattedMessage();
    }


    @Test
    void givenOccupiedPort_whenBecomeKeyserver_thenLogTheError() throws Exception {
        HttpServer occupiedPort = HttpServer.create(new InetSocketAddress(apimlSslKeyExchange.getPort()), 0);
        try {
            occupiedPort.start();

            IllegalStateException e = assertThrows(IllegalStateException.class, apimlSslKeyExchange::createServerSocket);
            assertNotNull(e.getCause());
            assertTrue(e.getCause().getMessage().contains("Address already in use"), "Unexpected cause message: " + e.getCause().getMessage());

            String logMessage = getLogMessage();
            assertTrue(logMessage.contains("Cannot create server socket: "), "Unexpected message: " + logMessage);
            assertTrue(logMessage.contains("Address already in use"), "Unexpected message: " + logMessage);
            assertTrue(logMessage.contains("BindException"), "Unexpected message: " + logMessage);
        } finally {
            occupiedPort.stop(0);
        }
    }

    private boolean containsAny(String message, String...expected) {
        for (String s : expected) {
            if (message.contains(s)) {
                return true;
            }
        }
        return false;
    }

    @Test
    void givenInvalidTarget_whenCreateSocketTo_thenLogTheError() {
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> apimlSslKeyExchange.createSocketTo(INVALID_ADDRESS));

        assertNotNull(e.getCause());
        assertTrue(e.getCause() instanceof ConnectException || e.getCause() instanceof BindException, "Unexpected exception: " + e.getCause().getClass());
        assertTrue(containsAny(e.getCause().getMessage(), "Cannot assign requested address: connect"), "Unexpected cause message: " + e.getCause().getMessage());

        String logMessage = getLogMessage();
        assertTrue(logMessage.contains("Cannot create socket to remote address"), "Unexpected message: " + logMessage);
        assertTrue(containsAny(logMessage, "Cannot assign requested address: connect"), "Unexpected message: " + logMessage);
        assertTrue(containsAny(logMessage, "BindException:", "ConnectException:"), "Unexpected message: " + logMessage);
    }

    @Test
    void givenInvalidTargetWithSslFactory_whenCreateSocketTo_thenLogTheError() throws Exception {
        SSLSocketFactory sslSocketFactory = apimlSslKeyExchange.getClientSSLContext().getSocketFactory();
        IllegalStateException e = assertThrows(IllegalStateException.class, () -> apimlSslKeyExchange.createSocketTo(INVALID_ADDRESS, sslSocketFactory));

        assertNotNull(e.getCause());
        assertTrue(e.getCause() instanceof ConnectException || e.getCause() instanceof BindException, "Unexpected exception: " + e.getCause().getClass());
        assertTrue(containsAny(e.getCause().getMessage(), "Cannot assign requested address: connect", "Connection refused"), "Unexpected cause message: " + e.getCause().getMessage());

        String logMessage = getLogMessage();
        assertTrue(logMessage.contains("Cannot create socket to remote address"), "Unexpected message: " + logMessage);
        assertTrue(containsAny(logMessage, "Cannot assign requested address: connect", "Connection refused"), "Unexpected message: " + logMessage);
        assertTrue(containsAny(logMessage, "BindException:", "ConnectException:"), "Unexpected message: " + logMessage);
    }

}
