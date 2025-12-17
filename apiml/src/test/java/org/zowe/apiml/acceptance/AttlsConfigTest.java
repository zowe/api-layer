/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.acceptance;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;
import com.netflix.discovery.shared.Applications;
import org.apache.http.HttpStatus;
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
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.result.view.freemarker.FreeMarkerConfigurer;
import org.zowe.apiml.ApimlApplication;
import org.zowe.apiml.discovery.ApimlInstanceRegistry;
import org.zowe.apiml.filter.AttlsHttpHandler;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.product.web.ApimlTomcatCustomizer;
import org.zowe.commons.attls.AttlsContext;
import org.zowe.commons.attls.InboundAttls;
import org.zowe.commons.attls.IoctlCallException;
import org.zowe.commons.attls.StatConn;
import org.zowe.commons.attls.UnknownEnumValueException;

import javax.net.ssl.SSLException;

import java.util.Base64;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.apache.hc.core5.http.HttpStatus.SC_PERMANENT_REDIRECT;
import static org.apache.http.HttpStatus.SC_FORBIDDEN;
import static org.apache.http.HttpStatus.SC_OK;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@TestInstance(Lifecycle.PER_CLASS)
class AttlsConfigTest {

    private String getGatewayUrlWithPath(String hostname, int port, String scheme, String path) {
        return String.format("%s://%s:%d/%s", scheme, hostname, port, path);
    }

    @Nested
    @ActiveProfiles({"attlsClient", "attlsServer"})
    @DirtiesContext
    @SpringBootTest(
        classes = ApimlApplication.class,
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
            assertThrows(SSLException.class, () -> {
                given()
                    .log().all()
                    .when()
                    .get(getGatewayUrlWithPath(hostname, port, "https", "application/version"))
                    .then()
                    .log().all();
            });
        }

        @Test
        void requestFailsWithAttlsReasonWithHttp() {
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
            "server.ssl.keyStore="
        }
    )
    @ActiveProfiles({"attlsServer", "attlsClient", "ApimlModulithAcceptanceTest"})
    @DirtiesContext
    @SpringBootTest(
        classes = {
            ApimlApplication.class,
            FreeMarkerConfigurer.class,
            TestConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    class GivenSslDisabled {

        @MockitoBean
        private AttlsHttpHandler attlsHttpHandler;

        @MockitoBean
        private ApimlInstanceRegistry apimlInstanceRegistry;

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;
        @MockitoBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;

        @BeforeEach
        void setUp() {
            when(apimlInstanceRegistry.getApplications()).thenReturn(new Applications());
            doNothing().when(apimlTomcatCustomizer).customize(any());
        }

        @Test
        void whenNoKeystore_thenStartupSuccess() {
            //@formatter:off
            given()
                .log().all()
            .when()
                .get(getGatewayUrlWithPath(hostname, port, "http", "application/version"))
            .then()
                .statusCode(SC_OK);
            //@formatter:on
            verify(apimlTomcatCustomizer, times(1)).customize(any());
            verify(attlsHttpHandler, times(1)).postProcessAfterInitialization(any(HttpHandler.class), any());
        }

    }

    @Nested
    @ActiveProfiles({"attlsClient", "attlsServer", "WhenCorsEnabledService"})
    @DirtiesContext
    @SpringBootTest(classes = {
            ApimlApplication.class,
            FreeMarkerConfigurer.class,
            TestConfig.class
        },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
    )
    @AcceptanceTest
    class WhenCorsEnabledService extends AcceptanceTestWithMockServices {

        // @MockitoBean
        // private AttlsHttpHandler attlsHttpHandler;

        @MockitoBean
        private ApimlInstanceRegistry apimlInstanceRegistry;

        @LocalServerPort
        private int port;

        @Value("${apiml.service.hostname:localhost}")
        private String hostname;
        @MockitoBean
        private ApimlTomcatCustomizer apimlTomcatCustomizer;

        @Mock
        private AttlsContext attlsContext;
        @Mock
        private ThreadLocal<AttlsContext> threadLocal;

        @BeforeEach
        void setUp() {
            // when(apimlInstanceRegistry.getApplications()).thenReturn(new Applications());
            doNothing().when(apimlTomcatCustomizer).customize(any());
            //((ThreadLocal<AttlsContext>) ReflectionTestUtils.getField(InboundAttls.class, "contexts")).set(attlsContext);
            ReflectionTestUtils.setField(InboundAttls.class, "contexts", threadLocal);
            when(threadLocal.get()).thenReturn(attlsContext);
        }

        @Test
        void givenServiceRuns_ApimlAccepts() {
            var testPath = "/testpath";
            var serviceId = "testuiservice";

            mockService(serviceId)
                .scope(MockService.Scope.TEST)
                .additionalMetadata(Map.of(
                    "apiml.corsEnabled", "true",
                    "apiml.corsAllowedOrigins", String.format("https://localhost:%d", port)
                ))
                .addEndpoint("/" + serviceId + "/api/v1" + testPath)
                    .responseCode(SC_OK)
                .and()
                .start();

            //@formatter:off
            given()
                .log().all()
                .header(HttpHeaders.ORIGIN, String.format("https://localhost:%d", port))
            .when()
                .post(getGatewayUrlWithPath(hostname, port, "http", "testuiservice/api/v1" + testPath))
            .then()
                .statusCode(is(not(SC_FORBIDDEN)));
            //@formatter:on
        }

        @Test
        void testLoginEndpoint() throws IoctlCallException, UnknownEnumValueException {
                // mockedContextHolder.when(ReactiveSecurityContextHolder::getContext).thenReturn(Mono.just(securityContext));
            //@formatter:off
            given()
                .log().all()
                .header(HttpHeaders.ORIGIN, String.format("https://localhost:%d", port))
                .auth().preemptive().basic("invaliduser", "invalidpassword")
            .when()
                .post(getGatewayUrlWithPath(hostname, port, "http", "apicatalog/api/v1/auth/login"))
            .then()
                .statusCode(is(SC_PERMANENT_REDIRECT));
            //@formatter:on
        }

        @Test
        void testLoginEndpoint_mockedAttls() throws IoctlCallException, UnknownEnumValueException {
            String cert = "MIID3jCCAsagAwIBAgIULApMeb1+40+ifLXNVf1mqwsNlt4wDQYJKoZIhvcNAQEL" +
                "BQAwYDELMAkGA1UEBhMCQ1oxEDAOBgNVBAgMB0N6ZWNoaWExDzANBgNVBAcMBlBy" +
                "YWd1ZTEMMAoGA1UECgwDT01GMQ0wCwYDVQQLDARab3dlMREwDwYDVQQDDAhBUElN" +
                "TCBDQTAeFw0yMzA2MDIxMjQ4NDBaFw0yOTA1MzExMjQ4NDBaMF8xCzAJBgNVBAYT" +
                "AkNaMRAwDgYDVQQIDAdDemVjaGlhMQ8wDQYDVQQHDAZQcmFndWUxDDAKBgNVBAoM" +
                "A09NRjENMAsGA1UECwwEWm93ZTEQMA4GA1UEAwwHQVBJTVRTVDCCASIwDQYJKoZI" +
                "hvcNAQEBBQADggEPADCCAQoCggEBAJ6L+6l6mfxByy/VrHQ881xkW/GWQQndocPH" +
                "i5Em15P+/ZQToYBTfLPUqGXcPnILg+PrjMtTHBCHO03pIuJxFXqrWfsaxR/O7zhp" +
                "BSTt+iT6/kMBhPdF4sJF2VQo1sGBa79hIn3StvD3hKba/5Rzx8i+WXpKNeCzYRoZ" +
                "BLYH/MLAokgabf0iWjzrwy9STBvZ0uPON4iBhz6bYh0wTra90j0dDjsetTBMOrm9" +
                "gO/sj7RD2KBQUM+mMiny5w4AWjvDChfzGEc37f/Ur2FyCqwY7k4oNS2tMtPQKemg" +
                "4CtmFsWLL3Vb7e6fwoCNFLsmJumsd13u2HCmnV5YT13ZL8xphqkCAwEAAaOBkDCB" +
                "jTALBgNVHQ8EBAMCBeAwHQYDVR0lBBYwFAYIKwYBBQUHAwIGCCsGAQUFBwMBMB8G" +
                "A1UdEQQYMBaCCWxvY2FsaG9zdIIJMTI3LjAuMC4xMB0GA1UdDgQWBBQ3GrkUuyvH" +
                "QmPRECqdzcR3qmQSHzAfBgNVHSMEGDAWgBT78hIus4SCXxMW8T9T0AEIe7HZNjAN" +
                "BgkqhkiG9w0BAQsFAAOCAQEAHAzeBownnYY9kSF6fif+dXw2miRTNkhRRc6ZIlij" +
                "Jy+d5ZysrR0yUTeW11raltGiX2gcCtg5GZp+ODgiqSMJN3mV1bIpKiuBhODKHlMz" +
                "pg8v4ebjIHd1buO8KbOlR8zKv4kMFiGqdfWW6W3BZy3w3RCOnWhts2Y4O+XZ4Gri" +
                "Yjiwkwf1IY7xv7HBJ4BsbUwxjxMcxa1HNqE8oAqEtiFxRmPkAi+g1lijvF26AKZd" +
                "WxKFTLJV1HxUsa5l8b7cHN9yya6IVixVcB9Cla06Rg7dkaI4Deb5JCxFXjoznDKY" +
                "kv8ZumkzQI9Ov90d1FYyVr7VWPEun/XV2XmH9nGHWyJSkA==";
            when(attlsContext.getCertificate()).thenReturn(Base64.getDecoder().decode(cert));
            when(attlsContext.getStatConn()).thenReturn(StatConn.SECURE);

            given()
                .log().all()
                .header(HttpHeaders.ORIGIN, String.format("https://localhost:%d", port))
                .auth().preemptive().basic("invaliduser", "invalidpassword")
            .when()
                .post(getGatewayUrlWithPath(hostname, port, "http", "apicatalog/api/v1/auth/login"))
            .then()
                .statusCode(is(SC_PERMANENT_REDIRECT));
        }
    }

}
