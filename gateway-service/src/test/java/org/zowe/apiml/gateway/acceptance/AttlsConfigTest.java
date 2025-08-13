/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.acceptance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.http.HttpStatus;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.filter.AttlsHttpHandler;
import org.zowe.apiml.gateway.GatewayServiceApplication;
import org.zowe.apiml.product.web.ApimlTomcatCustomizer;

import javax.net.ssl.SSLException;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_INTERNAL_SERVER_ERROR;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.verify;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    private String getGatewayUrlWithPath(String hostname, int port, String scheme, String path) {
        return String.format("%s://%s:%d/%s", scheme, hostname, port, path);
    }

    @Nested
    @TestPropertySource(
        properties = {
            "server.attls.enabled=true",
            "server.ssl.enabled=false"
        }
    )
    @ActiveProfiles("attls")
    @DirtiesContext
    // @Import(TestConfig.class)
    @SpringBootTest(
        classes = GatewayServiceApplication.class,
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

        @MockitoBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;

        @Test
        void whenContextloads_requestFailsWithHttps() {
            try {
                given()
                    .log().all()
                .when()
                    .get(getGatewayUrlWithPath(hostname, port, "https", "application/version"))
                .then()
                    .log().all()
                    .statusCode(SC_INTERNAL_SERVER_ERROR);
                fail("Expected an SSL failure");
            } catch (Exception e) {
                assertInstanceOf(SSLException.class, e);
            }
        }

        @Test
        void requestWorksWithAttls() {
            var logger = (Logger) LoggerFactory.getLogger(AttlsHttpHandler.class);
            logger.addAppender(mockedAppender);
            logger.setLevel(Level.ERROR);

            // Prevent use of native code but verify it calls the customizer
            doNothing().when(apimlTomcatCustomizer).customize(any());

            given()
                    .log().all()
                .when()
                    .get(getGatewayUrlWithPath(hostname, port, "http", "application/version"))
                .then()
                    .log().all()
                    .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                    .body(containsString("org.zowe.apiml.common.internalServerError"));

                verify(mockedAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
                assertThat(loggingEventCaptor.getAllValues())
                    .filteredOn(element -> element.getMessage().contains("Cannot verify AT-TLS status"))
                    .isNotEmpty();
        }

        @Test
        void requestFailsWithAttls() {
            // gateway does not use AttlsFilter?
        }

    }

    /**
     * This test intends to verify ICSF workaround (no keyring load)
     */
    @Nested
    @TestPropertySource(
        properties = {
            "server.ssl.enabled=false",
            "server.ssl.keyStoreType=",
            "server.ssl.keyStorePassword=",
            "server.ssl.keyPassword=",
            "server.ssl.keyAlias=",
            "server.ssl.keyStore="
        }
    )
    // @Import(TestConfig.class)
    @ActiveProfiles("attls")
    @DirtiesContext
    @SpringBootTest(
        classes = GatewayServiceApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenSslDisabled {

        @MockitoBean
        private AttlsHttpHandler attlsHttpHandler;

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;

        // @Autowired
        // private DiscoveryClient discoveryClient;

        @Test
        void whenNoKeystore_thenStartupSuccess() {
            given()
                .log().all()
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "http", "application/version"))
            .then()
                .statusCode(SC_OK);
        }

    }

}
