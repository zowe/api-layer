/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.security.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.filter.SecureConnectionFilter;
import org.zowe.apiml.gateway.GatewayApplication;
import org.zowe.apiml.product.web.ApimlTomcatCustomizer;

import javax.net.ssl.SSLException;

import static io.restassured.RestAssured.given;
import static org.apache.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    private String getGatewayUrlWithPath(String hostname, int port, String scheme) {
        return String.format("%s://%s:%d/%s", scheme, hostname, port, "application");
    }

    @Nested
    @ActiveProfiles({"attlsServer", "attlsClient"})
    @TestPropertySource(
        properties = {
            "server.internal.enabled=false"
        }
    )
    @DirtiesContext
    @SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenAttlsProfile {

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;

        @Mock
        private Appender<ILoggingEvent> mockedAppender;

        @Captor
        private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

        @MockBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;

        @Test
        void whenContextLoads_requestFailsWithHttps() {
            assertThrows(SSLException.class, () ->
                given()
                    .log().all()
                .when()
                    .get(getGatewayUrlWithPath(hostname, port, "https"))
                .then()
                    .log().all());
        }

        @Test
        void requestFailsWithAttlsReasonWithHttp() {
            Logger logger = (Logger) LoggerFactory.getLogger(SecureConnectionFilter.class);
            logger.addAppender(mockedAppender);
            logger.setLevel(Level.ERROR);

            // Prevent use of native code but verify it calls the customizer
            doNothing().when(apimlTomcatCustomizer).customize(any());

            given()
                .log().all()
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "http"))
            .then()
                .log().all()
                .statusCode(SC_INTERNAL_SERVER_ERROR);

            verify(mockedAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
            assertThat(loggingEventCaptor.getAllValues())
                .filteredOn(element -> element.getMessage().contains("Can't read from AT-TLS context"))
                .isNotEmpty();
        }

    }

    /**
     * This test intends to verify ICSF workaround (no keyring load)
     */
    @Nested
    @TestPropertySource(
        properties = {
            "server.ssl.keyStoreType=",
            "server.ssl.keyStorePassword=",
            "server.ssl.keyPassword=",
            "server.ssl.keyAlias=",
            "server.ssl.keyStore=",
            "server.internal.enabled=false",
            "apiml.security.auth.provider=zosmf" // zosmf is the only authentication provider supported in this scenario
        }
    )
    @ActiveProfiles({"attlsServer", "attlsClient"})
    @DirtiesContext
    @SpringBootTest(
        classes = GatewayApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenSslDisabled {

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;

        @MockBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;

        @Mock
        private Appender<ILoggingEvent> mockedAppender;

        @Captor
        private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

        @BeforeEach
        void setUp() {
            doNothing().when(apimlTomcatCustomizer).customize(any());
        }

        @Test
        void whenNoKeystore_thenStartupSuccess() {
            Logger logger = (Logger) LoggerFactory.getLogger(SecureConnectionFilter.class);
            logger.addAppender(mockedAppender);
            logger.setLevel(Level.ERROR);

            given()
                .log().all()
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "http"))
            .then()
                .log().all()
                .statusCode(SC_INTERNAL_SERVER_ERROR);

            verify(apimlTomcatCustomizer, times(1)).customize(any());
            verify(mockedAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
            assertThat(loggingEventCaptor.getAllValues())
                .filteredOn(element -> element.getMessage().contains("Can't read from AT-TLS context"))
                .isNotEmpty();
        }

    }

}
