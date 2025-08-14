/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.functional;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.apicatalog.ApiCatalogApplication;
import org.zowe.apiml.filter.AttlsHttpHandler;
import org.zowe.apiml.product.web.ApimlTomcatCustomizer;

import javax.net.ssl.SSLException;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.core.StringContains.containsString;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    @Nested
    @TestPropertySource(
        properties = {
            "server.attls.enabled=true",
            "server.ssl.enabled=false"
        }
    )
    @DirtiesContext
    @ActiveProfiles({"AttlsConfigTestCatalog", "attls"})
    @Disabled
    class GivenAttlsModeEnabled extends ApiCatalogFunctionalTest {

        @Mock
        private Appender<ILoggingEvent> mockedAppender;

        @Captor
        private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

        @Test
        void requestFailsWithHttps() {
            try {
                given()
                    .log().all()
                .when()
                    .get(getCatalogUriWithPath("apicatalog/containers"))
                .then()
                    .log().all()
                    .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR);
                fail("Expected an SSL failure");
            } catch (Exception e) {
                assertInstanceOf(SSLException.class, e);
            }
        }

        @Test
        void requestFailsWithAttlsContextReasonWithHttp() {
            var logger = (Logger) LoggerFactory.getLogger(AttlsHttpHandler.class);
            logger.addAppender(mockedAppender);
            logger.setLevel(Level.ERROR);

            given()
                .log().all()
            .when()
                .get(getCatalogUriWithPath("http", "apicatalog/containers"))
            .then()
                .log().all()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body(containsString("org.zowe.apiml.common.internalServerError"));

            verify(mockedAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
            assertThat(loggingEventCaptor.getAllValues())
                .filteredOn(element -> element.getMessage().contains("Cannot verify AT-TLS status"))
                .isNotEmpty();
        }

    }

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
    @ActiveProfiles({ "attls", "debug" })
    @DirtiesContext
    @SpringBootTest(
        classes = ApiCatalogApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenSslDisabled extends ApiCatalogFunctionalTest {

        @Mock
        private Appender<ILoggingEvent> mockedAppender;

        @Captor
        private ArgumentCaptor<LoggingEvent> loggingEventCaptor;

        @MockitoBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;

        @BeforeEach
        void setUp() {
            doNothing().when(apimlTomcatCustomizer).customize(any());
        }

        @Test
        void whenNoKeystore_thenStartupSuccess() {
            var logger = (Logger) LoggerFactory.getLogger(AttlsHttpHandler.class);
            logger.addAppender(mockedAppender);
            logger.setLevel(Level.ERROR);

            given()
                .log().all()
            .when()
                .get(getCatalogUriWithPath("http", "apicatalog/containers"))
            .then()
                .log().all()
                .statusCode(HttpStatus.SC_INTERNAL_SERVER_ERROR)
                .body(containsString("org.zowe.apiml.common.internalServerError"));

            verify(apimlTomcatCustomizer, times(1)).customize(any());
            verify(mockedAppender, atLeast(1)).doAppend(loggingEventCaptor.capture());
            assertThat(loggingEventCaptor.getAllValues())
                .filteredOn(element -> element.getMessage().contains("Cannot verify AT-TLS status"))
                .isNotEmpty();
        }

    }

}
