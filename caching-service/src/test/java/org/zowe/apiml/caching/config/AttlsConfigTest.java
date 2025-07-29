/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.config;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
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
import org.springframework.http.HttpStatus;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.caching.CachingServiceApplication;
import org.zowe.apiml.filter.AttlsHttpHandler;
import org.zowe.apiml.util.config.SslContext;

import javax.net.ssl.SSLException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;

@SpringBootTest(
    classes = CachingServiceApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
@ActiveProfiles("AttlsConfigTestCachingService")
@TestPropertySource(
    properties = {
        "server.attls.enabled=true",
        "server.ssl.enabled=false",
        "caching.storage.mode=inMemory"
    }
)
@DirtiesContext
@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    @Value("${apiml.service.hostname:localhost}")
    String hostname;
    @LocalServerPort
    int port;

    @Nested
    class GivenAttlsModeEnabled {

        @Mock
        private Appender<ILoggingEvent> mockedAppender;

        @Captor
        private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

        private String getUri(String scheme) {
            return String.format("%s://%s:%d/%s", scheme, hostname, port, "api/v1/cache");
        }

        @Nested
        class WhenContextLoads {

            @Test
            void requestFailsWithHttps() {
                try {
                    given()
                        .config(SslContext.clientCertUnknownUser)
                        .header("Content-type", "application/json")
                        .get(getUri("https"))
                        .then()
                        .statusCode(HttpStatus.FORBIDDEN.value());
                    fail("");
                } catch (Exception e) {
                    assertInstanceOf(SSLException.class, e);
                }
            }

            @Test
            void requestFailsWithAttlsReasonWithHttp() {
                var logger = (Logger) LoggerFactory.getLogger(AttlsHttpHandler.class);
                logger.addAppender(mockedAppender);
                logger.setLevel(Level.ERROR);
                given()
                    .config(SslContext.clientCertUnknownUser)
                    .header("Content-type", "application/json")
                    .get(getUri("http"))
                    .then()
                    .statusCode(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .body(containsString("org.zowe.apiml.common.internalServerError"));

                verify(mockedAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
                assertThat(loggingEventCaptor.getAllValues())
                    .filteredOn(element -> element.getMessage().contains("Cannot verify AT-TLS status"))
                    .isNotEmpty();
            }

        }

    }

}
