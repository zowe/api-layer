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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import io.restassured.http.ContentType;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.NestedTestConfiguration;
import org.springframework.test.context.NestedTestConfiguration.EnclosingConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.MockService.Scope;
import org.zowe.apiml.gateway.filters.X509FilterFactory;
import org.zowe.apiml.passticket.PassTicketException;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;
import org.zowe.apiml.zaas.security.mapping.OIDCExternalMapper;
import org.zowe.apiml.zaas.security.mapping.X509NativeMapper;
import org.zowe.apiml.zaas.security.service.TokenCreationService;
import org.zowe.apiml.zaas.security.service.token.ApimlAccessTokenProvider;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProvider;

import javax.naming.InvalidNameException;
import java.net.URI;
import java.security.cert.CertificateEncodingException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.constants.ApimlConstants.PAT_HEADER_NAME;
import static org.zowe.apiml.security.common.util.JWTTestUtils.createExpiredZoweJwtToken;
import static org.zowe.apiml.security.common.util.JWTTestUtils.createZoweJwtToken;
import static org.zowe.apiml.security.common.util.JWTTestUtils.createZowePatJwtToken;

class OpenTelemetryResourceAttributesZosTest {

    private static final String LOGIN_ENDPOINT = "/gateway/api/v1/auth/login";
    private static final String AUTH_COOKIE = "apimlAuthenticationToken";

    @SuppressWarnings("null")
    private boolean assertAttributesBase(Attributes attributes, int port) {
        assertEquals("ZWE1AG", attributes.get(stringKey("process.zos.jobname")));
        assertEquals("apiml:apiml1:" + port, attributes.get(stringKey("service.name")));
        assertNull(attributes.get(stringKey("service.namespace")));
        assertNotNull(attributes.get(stringKey("service.version")));
        assertNotNull(attributes.get(stringKey("os.version")));
        assertEquals("LR10", attributes.get(stringKey("zos.smf.id")));
        assertEquals("localhost:gateway:" + port, attributes.get(stringKey("service.instance.id")));
        assertEquals("DEV", attributes.get(stringKey("deployment.environment.name")));
        return true;
    }

    @Nested
    @AcceptanceTest
    @ActiveProfiles({"test", "OpenTelemetryTest", "zos"})
    @TestPropertySource(
        properties = {
            "otel.sdk.disabled=false",
            "otel.metrics.exporter=none",
            "otel.traces.exporter=none",
            "otel.logs.exporter=none"
        }
    )
    class WhenBasicConfig {

        @Autowired
        private InMemoryMetricReader metricReader;

        @LocalServerPort
        private int port;

        @Test
        void thenLogCustomAttributes() {
            var metrics = metricReader.collectAllMetrics();
            assertFalse(metrics.isEmpty(), "No data received");

            metrics.forEach(
                metric -> {
                    var attributes = metric.getResource().getAttributes();
                    assertTrue(assertAttributesBase(attributes, port));
                }
            );
        }

    }

    @Nested
    @AcceptanceTest
    @TestPropertySource(
        properties = {
            "otel.sdk.disabled=false",
            "otel.metrics.exporter=none",
            "otel.traces.exporter=none",
            "otel.logs.exporter=none",
            "apiml.security.auth.provider=saf",
            "apiml.security.x509.enabled=true",
            "apiml.security.oidc.validationType=endpoint",
            "apiml.security.oidc.enabled=true",
            "apiml.security.oidc.userInfo.uri=https://oidc.provider.com/user/info",
            "apiml.security.filterChainConfiguration=new",
            "apiml.security.personalAccessToken.enabled=true"
        }
    )
    @ActiveProfiles({"test", "OpenTelemetryTest", "zos"})
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    @NestedTestConfiguration(EnclosingConfiguration.OVERRIDE)
    class WhenOnboardedService extends AcceptanceTestWithMockServices {

        private static final String VALID_OIDC_TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";

        @Autowired
        private LogRecordExporter logExporter;

        @Autowired
        private HttpConfig httpConfig;

        @MockitoBean
        private OIDCExternalMapper oidcExternalMapper;

        @MockitoBean
        private OIDCTokenProvider oidcTokenProvider;

        @MockitoBean
        private X509NativeMapper x509TokenProvider;

        @MockitoBean
        private TokenCreationService tokenCreationService;

        @MockitoBean
        private ApimlAccessTokenProvider apimlAccessTokenProvider;

        @MockitoSpyBean
        private X509FilterFactory x509FilterFactory;

        @BeforeAll
        void startMockServices() throws Exception {
            if (!SslContext.isInitialized()) {
                SslContextConfigurer configurer = new SslContextConfigurer("password".toCharArray(), "../keystore/client/client-certs.p12", "../keystore/service/service.keystore.p12");
                SslContext.prepareSslAuthentication(configurer);
            }
        }

        @AfterAll
        void stop() {
            SslContext.reset();
        }

        @BeforeEach
        void setUp() {
            assertTrue(logExporter instanceof InMemoryLogRecordExporter);
            ((InMemoryLogRecordExporter) logExporter).reset();
        }

        private List<LogRecordData> assertLogsExported() {
            List<LogRecordData> logs = new ArrayList<>();
            await("Log export")
                .atMost(Duration.ofSeconds(10))
                .until(() -> {
                    var exporter = (InMemoryLogRecordExporter) logExporter;
                    var l = exporter.getFinishedLogRecordItems();
                    if (l.size() > 0) {
                        logs.addAll(l);
                    }
                    exporter.reset();
                    return l.size() > 0;
                });
            return logs;
        }

        private LogRecordData assertOneLogRecordExported(String expectedUrl) {
            var logs = assertLogsExported();

            var logRecord = logs.stream()
                .filter(log -> log.getBodyValue().asString().contains(expectedUrl))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected log record with URL " + expectedUrl + " not found in logs: " + logs.stream().map(LogRecordData::getBodyValue).map(String::valueOf).collect(Collectors.joining(", "))));

            assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());

            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));

            return logRecord;
        }

        // Requests that target API ML (/login, /query, /logout, /services, etc.)
        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenRequestToAPIML {

            @Test
            void givenLoginEndpoint_failure_thenLog() {
                given()
                    .auth().preemptive()
                    .basic("wronguser", "wrongpass")
                    .post(basePath + "/gateway/api/v1/auth/login")
                .then()
                    .statusCode(401);

                var logRecord = assertOneLogRecordExported("/gateway/api/v1/auth/login");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertEquals("gateway", getAttribute(logBody, "service.id"));
                assertEquals("POST", getAttribute(logBody, "http.request.method"));
                assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                assertEquals("EACCES: Permission is denied; the specified password is incorrect", getAttribute(logBody, "auth.error.message"));
                assertEquals("org.zowe.apiml.security.common.error.ZosAuthenticationException", getAttribute(logBody, "auth.error.type"));
                assertEquals("localhost:gateway:" + port, getAttribute(logBody, "service.instance.id"));
                assertEquals("401", getAttribute(logBody, "service.response_code"));
                assertEquals("/gateway/api/v1/auth/login", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertEquals("BASIC", getAttribute(logBody, "auth.method"));
            }

            @Test
            void givenCatalogEndpoint_thenLog() {
                given()
                    .get(basePath + "/apicatalog/ui/v1/index.html")
                .then()
                    .statusCode(200);

                var logRecord = assertOneLogRecordExported("/apicatalog/ui/v1/index.html");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertNull(getAttribute(logBody, "auth.status"));
                assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                assertEquals("200", getAttribute(logBody, "service.response_code"));
                assertEquals("/apicatalog/ui/v1/index.html", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertNull(getAttribute(logBody, "auth.method"));
            }

            @Test
            void givenProtectedCatalogEndpoint_withBasicAuth_success_thenLog() {
                given()
                    .auth().preemptive()
                    .basic("USER", "validPassword")
                    .get(basePath + "/apicatalog/api/v1/containers")
                .then()
                    .statusCode(200);

                var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertNull(getAttribute(logBody, "auth.status"));
                assertNull(getAttribute(logBody, "auth.error.message"));
                assertNull(getAttribute(logBody, "auth.error.type"));
                assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                assertEquals("200", getAttribute(logBody, "service.response_code"));
                assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertEquals("BASIC", getAttribute(logBody, "auth.method"));
            }

            @Test
            void givenProtectedCatalogEndpoint_withBasicAuth_failure_thenLog() {
                given()
                    .auth().preemptive()
                    .basic("USER", "wrongPassword")
                    .get(basePath + "/apicatalog/api/v1/containers")
                .then()
                    .statusCode(401);

                var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                assertEquals("EACCES: Permission is denied; the specified password is incorrect", getAttribute(logBody, "auth.error.message"));
                assertEquals("org.zowe.apiml.security.common.error.ZosAuthenticationException", getAttribute(logBody, "auth.error.type"));
                assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                assertEquals("401", getAttribute(logBody, "service.response_code"));
                assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertEquals("BASIC", getAttribute(logBody, "auth.method"));
            }

            @Test
            void givenProtectedCatalogEndpoint_withJwt_success_thenLog() {
                given()
                    .cookie(AUTH_COOKIE, login())
                    .get(basePath + "/apicatalog/api/v1/containers")
                .then()
                    .statusCode(200);

                var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertNull(getAttribute(logBody, "auth.status"));
                assertNull(getAttribute(logBody, "auth.error.message"));
                assertNull(getAttribute(logBody, "auth.error.type"));
                assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                assertEquals("200", getAttribute(logBody, "service.response_code"));
                assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertEquals("JWT", getAttribute(logBody, "auth.method"));
            }

            @Test
            void givenProtectedCatalogEndpoint_withInvalidJwt_thenLog() {
                given()
                    .cookie(AUTH_COOKIE, "invalid.jwt.token")
                    .get(basePath + "/apicatalog/api/v1/containers")
                .then()
                    .statusCode(401);

                var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                assertEquals("ZWEAO402E The request has not been applied because it lacks valid authentication credentials.", getAttribute(logBody, "auth.error.message"));
                assertEquals("org.zowe.apiml.security.common.token.TokenNotValidException", getAttribute(logBody, "auth.error.type"));
                assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                assertEquals("401", getAttribute(logBody, "service.response_code"));
                assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertEquals("JWT", getAttribute(logBody, "auth.method"));
            }

            @Test
            void givenProtectedCatalogEndpoint_withExpiredJwt_thenLog() {
                given()
                    .cookie(AUTH_COOKIE, createExpiredZoweJwtToken("USER", "z/OS", "Ltpa", httpConfig.getHttpsConfig()))
                    .get(basePath + "/apicatalog/api/v1/containers")
                .then()
                    .statusCode(401);

                var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                assertEquals("ZWEAO402E The request has not been applied because it lacks valid authentication credentials.", getAttribute(logBody, "auth.error.message"));
                assertEquals("org.zowe.apiml.security.common.token.TokenNotValidException", getAttribute(logBody, "auth.error.type"));
                assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                assertEquals("401", getAttribute(logBody, "service.response_code"));
                assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertEquals("JWT", getAttribute(logBody, "auth.method"));
            }

            @Nested
            class WhenMultipleAuthorization {
                @Test
                void givenProtectedCatalogEndpoint_withCorrectJwtAndCorrectBasic_success_thenLog() {
                    given()
                        .cookie(AUTH_COOKIE, login())
                        .auth().preemptive()
                        .basic("USER", "validPassword")
                        .get(basePath + "/apicatalog/api/v1/containers")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertNull(getAttribute(logBody, "auth.status"));
                    assertNull(getAttribute(logBody, "auth.error.message"));
                    assertNull(getAttribute(logBody, "auth.error.type"));
                    assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("BASIC", getAttribute(logBody, "auth.method")); // From JWT was reset to BASIC
                }

                @Test
                void givenProtectedCatalogEndpoint_withCorrectJwtAndInvalidBasic_failure_thenLog() {
                    given()
                        .cookie(AUTH_COOKIE, login())
                        .auth().preemptive()
                        .basic("USER", "invalidPassword")
                        .get(basePath + "/apicatalog/api/v1/containers")
                    .then()
                        .statusCode(401);

                    var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                    assertEquals("EACCES: Permission is denied; the specified password is incorrect", getAttribute(logBody, "auth.error.message"));
                    assertEquals("org.zowe.apiml.security.common.error.ZosAuthenticationException", getAttribute(logBody, "auth.error.type"));
                    assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                    assertEquals("401", getAttribute(logBody, "service.response_code"));
                    assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("BASIC", getAttribute(logBody, "auth.method")); // From JWT was reset to BASIC
                }

                @Test
                void givenProtectedCatalogEndpoint_withInvalidJwtAndValidBasic_success_thenLog() {
                    given()
                        .cookie(AUTH_COOKIE, "invalid.jwt.token")
                        .auth().preemptive()
                        .basic("USER", "validPassword")
                        .get(basePath + "/apicatalog/api/v1/containers")
                    .then()
                        .statusCode(401);

                    var logRecord = assertOneLogRecordExported("/apicatalog/api/v1/containers");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertEquals("apicatalog", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status")); // From ERROR was reset to null
                    assertEquals("ZWEAO402E The request has not been applied because it lacks valid authentication credentials.", getAttribute(logBody, "auth.error.message")); // From "ZWEAO402E The request has not been applied because it lacks valid authentication credentials." was reset to null
                    assertEquals("org.zowe.apiml.security.common.token.TokenNotValidException", getAttribute(logBody, "auth.error.type")); // From "org.zowe.apiml.security.common.token.TokenNotValidException" was reset to null
                    assertEquals("localhost:apicatalog:" + port, getAttribute(logBody, "service.instance.id"));
                    assertEquals("401", getAttribute(logBody, "service.response_code"));
                    assertEquals("/apicatalog/api/v1/containers", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("JWT", getAttribute(logBody, "auth.method")); // From JWT was reset to BASIC
                }
            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenServiceDoesNotExist {

            @Test
            void givenNoRoute_thenLog() {
                given()
                    .get(basePath + "/nonexistant/api/v1/200")
                .then()
                    .statusCode(404);

                var logRecord = assertOneLogRecordExported("/nonexistant/api/v1/200");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("nonexistant", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertNull(getAttribute(logBody, "auth.status"));
                assertNull(getAttribute(logBody, "service.instance.id"));
                assertEquals("404", getAttribute(logBody, "service.response_code"));
                assertEquals("/nonexistant/api/v1/200", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertNull(getAttribute(logBody, "auth.method"));
            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenServiceBypass {

            private MockService mockServiceBypass;

            @BeforeAll
            void init() {
                mockServiceBypass = mockService("testservicebp")
                    .scope(Scope.CLASS)
                    .authenticationScheme(AuthenticationScheme.BYPASS)
                    .addEndpoint("/testservicebp/200")
                    .responseCode(200)
                .and().start();
            }

            @Test
            void thenLog() {
                given()
                    .cookie(AUTH_COOKIE, login())
                    .get(basePath + "/testservicebp/api/v1/200")
                .then()
                    .statusCode(200);

                var logRecord = assertOneLogRecordExported("/testservicebp/api/v1/200");
                assertAttributesBase(logRecord.getResource().getAttributes(), port);
                @SuppressWarnings("null")
                var logBody = logRecord.getBodyValue().asString();
                assertNull(getAttribute(logBody, "user.id"));
                assertEquals("testservicebp", getAttribute(logBody, "service.id"));
                assertEquals("GET", getAttribute(logBody, "http.request.method"));
                assertNull(getAttribute(logBody, "auth.status"));
                assertEquals("localhost:testservicebp:" + mockServiceBypass.getPort(), getAttribute(logBody, "service.instance.id"));
                assertEquals("200", getAttribute(logBody, "service.response_code"));
                assertEquals("/testservicebp/api/v1/200", getAttribute(logBody, "url.path"));
                assertEquals("https", getAttribute(logBody, "url.scheme"));
                assertNull(getAttribute(logBody, "auth.method"));
                assertEquals("bypass", getAttribute(logBody, "auth.service.auth.method"));
            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenServiceRequiresJwt {

            private MockService mockServiceZoweJwt;

            @BeforeAll
            void init() {
                mockServiceZoweJwt = mockService("testservice")
                    .scope(Scope.CLASS)
                    .authenticationScheme(AuthenticationScheme.ZOWE_JWT)
                    .addEndpoint("/testservice/200")
                    .responseCode(200)
                .and()
                    .addEndpoint("/testservice/401")
                    .responseCode(401)
                .and().start();
            }

            @Nested
            class WhenAuthPresent {

                @Nested
                class WhenAuthSuccess {

                    @Test
                    void givenRouted_withAuthJwt_success_thenLog() {
                        given()
                            .cookie("apimlAuthenticationToken", login())
                            .get(basePath + "/testservice/api/v1/200")
                        .then()
                            .statusCode(200);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/200");
                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertEquals("USER", getAttribute(logBody, "user.id"));
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("OK", getAttribute(logBody, "auth.status"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("200", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertEquals("JWT", getAttribute(logBody, "auth.method"));
                    }

                    @Test
                    void givenRouted_withX509_success_thenLog() {
                        given()
                            .config(SslContext.clientCertUser)
                            .get(basePath + "/testservice/api/v1/200")
                        .then()
                            .statusCode(200);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/200");
                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertEquals("USER", getAttribute(logBody, "user.id"));
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("OK", getAttribute(logBody, "auth.status"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("200", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertEquals("CLIENT_CERT", getAttribute(logBody, "auth.method"));
                    }

                    @Test
                    void givenRouted_withOidc_success_thenLog() {
                        when(oidcTokenProvider.isValid(VALID_OIDC_TOKEN)).thenReturn(true);
                        when(oidcExternalMapper.mapToMainframeUserId(any())).thenReturn("USER");

                        given()
                            .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_OIDC_TOKEN)
                            .get(basePath + "/testservice/api/v1/200")
                        .then()
                            .statusCode(200);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/200");
                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertEquals("USER", getAttribute(logBody, "user.id"));
                        assertEquals(List.of("oidc.username"), getAttribute(logBody, "user.distributed.id"));
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("OK", getAttribute(logBody, "auth.status"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("200", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertEquals("OIDC", getAttribute(logBody, "auth.method"));
                    }

                    @Test
                    void givenRouted_withPAT_success_thenLog() {
                        var pat = createZowePatJwtToken("USER", "z/OS", List.of("testservice"), httpConfig.getHttpsConfig());
                        when(apimlAccessTokenProvider.isValidForScopes(pat, "testservice")).thenReturn(true);
                        when(apimlAccessTokenProvider.isInvalidated(pat)).thenReturn(false);
                        given()
                            .header(PAT_HEADER_NAME, pat)
                            .get(basePath + "/testservice/api/v1/200")
                        .then()
                            .statusCode(200);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/200");
                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertEquals("USER", getAttribute(logBody, "user.id"));
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("OK", getAttribute(logBody, "auth.status"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("200", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertEquals("PAT", getAttribute(logBody, "auth.method"));
                    }

                }

                @Nested
                class WhenAuthFailure {

                    @Test
                    void whenOidcTokenInvalid_thenLog() {
                        when(oidcTokenProvider.isValid(VALID_OIDC_TOKEN)).thenReturn(false);

                        given()
                            .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_OIDC_TOKEN)
                            .get(basePath + "/testservice/api/v1/200")
                        .then()
                            .statusCode(200);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/200");

                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertNull(getAttribute(logBody, "user.id"));
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                        assertEquals("ZWEAG160E No authentication provided in the request", getAttribute(logBody, "auth.error.message"));
                        assertEquals("org.springframework.security.authentication.InsufficientAuthenticationException", getAttribute(logBody, "auth.error.type"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("200", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertNull(getAttribute(logBody, "auth.method"));
                    }

                    @Test
                    void whenInvalidToken_thenLog() {
                        given()
                            .cookie("apimlAuthenticationToken", "invalid.jwt.token")
                            .get(basePath + "/testservice/api/v1/401")
                        .then()
                            .statusCode(401);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/401");
                        var logBody = logRecord.getBodyValue().asString();
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                        assertEquals("ZWEAO402E The request has not been applied because it lacks valid authentication credentials.", getAttribute(logBody, "auth.error.message"));
                        assertEquals("org.zowe.apiml.security.common.token.TokenNotValidException", getAttribute(logBody, "auth.error.type"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("401", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/401", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertNull(getAttribute(logBody, "auth.method"));
                    }

                    @Test
                    void whenExpiredToken_thenLog() {
                        given()
                            .cookie("apimlAuthenticationToken", createExpiredZoweJwtToken("USER", "z/OS", "Ltpa", httpConfig.getHttpsConfig()))
                            .get(basePath + "/testservice/api/v1/401")
                        .then()
                            .statusCode(401);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/401");
                        var logBody = logRecord.getBodyValue().asString();
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                        assertEquals("ZWEAO402E The request has not been applied because it lacks valid authentication credentials.", getAttribute(logBody, "auth.error.message"));
                        assertEquals("org.zowe.apiml.security.common.token.TokenExpireException", getAttribute(logBody, "auth.error.type"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("401", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/401", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertNull(getAttribute(logBody, "auth.method"));
                    }

                    @Test
                    void givenUntrustedX509_thenLog() {
                        given()
                            .config(SslContext.selfSignedUntrusted)
                            .get(basePath + "/testservice/api/v1/401")
                            .then()
                            .statusCode(401);

                        var logRecord = assertOneLogRecordExported("/testservice/api/v1/401");
                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertNull(getAttribute(logBody, "user.id"));
                        assertEquals("testservice", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                        assertEquals("ZWEAG160E No authentication provided in the request", getAttribute(logBody, "auth.error.message"));
                        assertEquals("org.springframework.security.authentication.InsufficientAuthenticationException", getAttribute(logBody, "auth.error.type"));
                        assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("401", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservice/api/v1/401", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                        assertNull(getAttribute(logBody, "auth.method"));
                    }

                }

            }

            @Nested
            class WhenAuthAbsent {

                @Test
                void whenNoJwtProvided_thenLog() {
                    given()
                        .get(basePath + "/testservice/api/v1/401")
                    .then()
                        .statusCode(401);

                    var logRecord = assertOneLogRecordExported("/testservice/api/v1/401");
                    var logBody = logRecord.getBodyValue().asString();
                    assertEquals("testservice", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                    assertEquals("ZWEAG160E No authentication provided in the request", getAttribute(logBody, "auth.error.message"));
                    assertEquals("org.springframework.security.authentication.InsufficientAuthenticationException", getAttribute(logBody, "auth.error.type"));
                    assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("401", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservice/api/v1/401", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
                    assertNull(getAttribute(logBody, "auth.method"));
                }

            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenServiceRequiresPassTicket {

            private MockService mockServicePassTicket;
            private MockService mockServicePassTicketMisconfigured;

            @BeforeAll
            void init() {
                mockServicePassTicket = mockService("testservicept")
                    .scope(Scope.CLASS)
                    .authenticationScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET)
                    .applid("TSTSVRPT")
                    .addEndpoint("/testservicept/200")
                    .responseCode(200)
                .and().start();

                mockServicePassTicketMisconfigured = mockService("testservicepterror")
                    .scope(Scope.CLASS)
                    .authenticationScheme(AuthenticationScheme.HTTP_BASIC_PASSTICKET)
                    .addEndpoint("/testservicepterror/200")
                    .responseCode(200)
                .and().start();
            }

            @Nested
            class WhenMisconfigured {

                @Test
                void thenLog() {
                    given()
                        .cookie(AUTH_COOKIE, login())
                        .get(basePath + "/testservicepterror/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicepterror/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertEquals("testservicepterror", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                    assertEquals(mockServicePassTicketMisconfigured.getInstanceId(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicepterror/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("httpBasicPassTicket", getAttribute(logBody, "auth.service.auth.method"));
                    assertNull(getAttribute(logBody, "auth.method"));
                }

            }

            @Nested
            class WhenAuthPresent {

                @Test
                void whenSucess_thenLog() {
                    given()
                        .cookie(AUTH_COOKIE, login())
                        .get(basePath + "/testservicept/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicept/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertEquals("USER", getAttribute(logBody, "user.id"));
                    assertEquals("testservicept", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("OK", getAttribute(logBody, "auth.status"));
                    assertEquals("localhost:testservicept:" + mockServicePassTicket.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicept/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("httpBasicPassTicket", getAttribute(logBody, "auth.service.auth.method"));
                    assertEquals("JWT", getAttribute(logBody, "auth.method"));
                }

            }

            @Nested
            class WhenAuthAbsent {

                @Test
                void whenNoTokenProvided_thenLog() {
                    given()
                        .get(basePath + "/testservicept/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicept/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertEquals("testservicept", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                    assertEquals("localhost:testservicept:" + mockServicePassTicket.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicept/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("httpBasicPassTicket", getAttribute(logBody, "auth.service.auth.method"));
                    assertNull(getAttribute(logBody, "auth.method"));
                    assertEquals("ZWEAG160E No authentication provided in the request", getAttribute(logBody, "auth.error.message"));
                    assertEquals("org.springframework.security.authentication.InsufficientAuthenticationException", getAttribute(logBody, "auth.error.type"));
                }

            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenServiceRequiresSafIdt {

            private MockService mockServiceSafIdt;

            @BeforeAll
            void init() {
                mockServiceSafIdt = mockService("testservicesafidt")
                    .scope(Scope.CLASS)
                    .authenticationScheme(AuthenticationScheme.SAF_IDT)
                    .applid("TSTSVRID")
                    .addEndpoint("/testservicesafidt/200")
                    .responseCode(200)
                .and().start();
            }

            @BeforeEach
            void setUp() {
                Mockito.reset(tokenCreationService);
            }

            @Nested
            class WhenAuthPresent {

                @Test
                void whenSuccess_thenLog() {
                    when(tokenCreationService.createSafIdTokenWithoutCredentials("USER", "TSTSVRID")).thenReturn("validsafidt");
                    given()
                        .cookie(AUTH_COOKIE, login())
                        .get(basePath + "/testservicesafidt/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicesafidt/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertEquals("USER", getAttribute(logBody, "user.id"));
                    assertEquals("testservicesafidt", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("OK", getAttribute(logBody, "auth.status"));
                    assertEquals("localhost:testservicesafidt:" + mockServiceSafIdt.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicesafidt/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("safIdt", getAttribute(logBody, "auth.service.auth.method"));
                    assertEquals("JWT", getAttribute(logBody, "auth.method"));
                    assertNull(getAttribute(logBody, "auth.error.type"));
                    assertNull(getAttribute(logBody, "auth.error.message"));
                }

                @Test
                void whenFailure_thenLog() {
                    when(tokenCreationService.createSafIdTokenWithoutCredentials("USER", "TSTSVRID")).thenThrow(new PassTicketException("Test exception"));
                    given()
                        .cookie(AUTH_COOKIE, login())
                        .get(basePath + "/testservicesafidt/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicesafidt/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertEquals("testservicesafidt", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                    assertEquals("Test exception", getAttribute(logBody, "auth.error.message"));
                    assertEquals("org.zowe.apiml.passticket.PassTicketException", getAttribute(logBody, "auth.error.type"));
                    assertEquals("localhost:testservicesafidt:" + mockServiceSafIdt.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicesafidt/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("safIdt", getAttribute(logBody, "auth.service.auth.method"));
                    assertNull(getAttribute(logBody, "auth.method"));
                }

            }

            @Nested
            class WhenAuthAbsent {

                @Test
                void thenLog() {
                    given()
                        .get(basePath + "/testservicesafidt/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicesafidt/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertEquals("testservicesafidt", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                    assertEquals("ZWEAG160E No authentication provided in the request", getAttribute(logBody, "auth.error.message"));
                    assertEquals("org.springframework.security.authentication.InsufficientAuthenticationException", getAttribute(logBody, "auth.error.type"));
                    assertEquals("localhost:testservicesafidt:" + mockServiceSafIdt.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicesafidt/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("safIdt", getAttribute(logBody, "auth.service.auth.method"));
                    assertNull(getAttribute(logBody, "auth.method"));
                }

            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenServiceRequiresX509 {

            private MockService mockServiceX509;

            @BeforeAll
            void init() {
                mockServiceX509 = mockService("testservicex509")
                    .scope(Scope.CLASS)
                    .authenticationScheme(AuthenticationScheme.X509)
                    .additionalMetadata(Map.of(
                        "apiml.authentication.headers", "X-Certificate-Public,X-Certificate-DistinguishedName,X-Certificate-CommonName"))
                    .addEndpoint("/testservicex509/200")
                        .assertion(exchange -> assertTrue(exchange.getRequestHeaders().containsKey("X-Certificate-Public")))
                        .assertion(exchange -> assertTrue(exchange.getRequestHeaders().containsKey("X-Certificate-DistinguishedName")))
                        .assertion(exchange -> assertTrue(exchange.getRequestHeaders().containsKey("X-Certificate-CommonName")))
                        .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-zowe-auth-failure")))
                        .responseCode(200)
                    .and().addEndpoint("/testservicex509/401")
                        .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-Certificate-Public")))
                        .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-Certificate-DistinguishedName")))
                        .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-Certificate-CommonName")))
                        .assertion(exchange -> assertEquals("Invalid client certificate in request. Error message: Test Exception", exchange.getRequestHeaders().get("X-zowe-auth-failure").get(0)))
                        .responseCode(401)
                    .and().addEndpoint("/testservicex509/400")
                        .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-Certificate-Public")))
                        .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-Certificate-DistinguishedName")))
                        .assertion(exchange -> assertFalse(exchange.getRequestHeaders().containsKey("X-Certificate-CommonName")))
                        .assertion(exchange -> assertEquals("ZWEAG167E No client certificate provided in the request", exchange.getRequestHeaders().get("X-zowe-auth-failure").get(0)))
                        .responseCode(401)
                    .and().start();
            }

            @BeforeEach
            void setUp() {
                Mockito.reset(x509FilterFactory);
            }

            @Nested
            class WhenAuthPresent {

                @Test
                void whenSuccess_thenLog() {
                    given()
                        .config(SslContext.clientCertUser)
                        .get(basePath + "/testservicex509/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicex509/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertNull(getAttribute(logBody, "auth.status"));
                    assertNull(getAttribute(logBody, "auth.error.message"));
                    assertNull(getAttribute(logBody, "auth.error.type"));
                    assertEquals("testservicex509", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("localhost:testservicex509:" + mockServiceX509.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicex509/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("x509", getAttribute(logBody, "auth.service.auth.method"));
                    assertEquals("CLIENT_CERT", getAttribute(logBody, "auth.method"));
                }

                @Test
                void whenFailure_thenLog() throws InvalidNameException, CertificateEncodingException {
                    doThrow(new InvalidNameException("Test Exception")).when(x509FilterFactory).setHeader(any(), any(), any());

                    given()
                        .config(SslContext.clientCertUser)
                        .get(basePath + "/testservicex509/api/v1/401")
                    .then()
                        .statusCode(401);

                    var logRecord = assertOneLogRecordExported("/testservicex509/api/v1/401");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertNull(getAttribute(logBody, "auth.status"));
                    assertNull(getAttribute(logBody, "auth.error.message"));
                    assertNull(getAttribute(logBody, "auth.error.type"));
                    assertEquals("testservicex509", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("localhost:testservicex509:" + mockServiceX509.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("401", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicex509/api/v1/401", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("x509", getAttribute(logBody, "auth.service.auth.method"));
                    assertEquals("CLIENT_CERT", getAttribute(logBody, "auth.method"));
                }

            }

            @Nested
            class WhenAuthAbsent {

                @Test
                void thenLog() {
                    given()
                        .get(basePath + "/testservicex509/api/v1/400")
                    .then()
                        .statusCode(401);

                    var logRecord = assertOneLogRecordExported("/testservicex509/api/v1/400");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertNull(getAttribute(logBody, "auth.status"));
                    assertNull(getAttribute(logBody, "auth.error.message"));
                    assertNull(getAttribute(logBody, "auth.error.type"));
                    assertEquals("testservicex509", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("localhost:testservicex509:" + mockServiceX509.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("401", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicex509/api/v1/400", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("x509", getAttribute(logBody, "auth.service.auth.method"));
                    assertNull(getAttribute(logBody, "auth.method"));
                }

            }

        }

        @Nested
        @TestInstance(TestInstance.Lifecycle.PER_CLASS)
        class WhenServiceRequiresZosmf {

            private MockService mockServicezosmf;

            @BeforeAll
            void init() {
                mockServicezosmf = mockService("testservicezosmf")
                    .scope(Scope.CLASS)
                    .authenticationScheme(AuthenticationScheme.ZOSMF)
                    .addEndpoint("/testservicezosmf/200")
                    .responseCode(200)
                .and()
                    .addEndpoint("/testservicezosmf/401")
                    .responseCode(401)
                .and().start();
            }

            @Nested
            class WhenAuthPresent {

                @Test
                void whenSuccess_thenLog() {
                    given()
                        .cookie(AUTH_COOKIE, createZoweJwtToken("USER", "z/OS", "Ltpa", httpConfig.getHttpsConfig()))
                        .get(basePath + "/testservicezosmf/api/v1/200")
                    .then()
                        .statusCode(200);

                    var logRecord = assertOneLogRecordExported("/testservicezosmf/api/v1/200");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertEquals("OK", getAttribute(logBody, "auth.status"));
                    assertNull(getAttribute(logBody, "auth.error.message"));
                    assertNull(getAttribute(logBody, "auth.error.type"));
                    assertEquals("testservicezosmf", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("localhost:testservicezosmf:" + mockServicezosmf.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicezosmf/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("zosmf", getAttribute(logBody, "auth.service.auth.method"));
                    assertEquals("JWT", getAttribute(logBody, "auth.method"));
                }

                @Nested
                class WhenAuthFailure {

                    @Test
                    void whenInvalidToken_thenLog() {
                        given()
                            .cookie(AUTH_COOKIE, "invalid.jwt.token")
                            .get(basePath + "/testservicezosmf/api/v1/401")
                        .then()
                            .statusCode(401);

                        var logRecord = assertOneLogRecordExported("/testservicezosmf/api/v1/401");
                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertNull(getAttribute(logBody, "user.id"));
                        assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                        assertEquals("Token is not valid.", getAttribute(logBody, "auth.error.message"));
                        assertEquals("org.zowe.apiml.security.common.token.TokenNotValidException", getAttribute(logBody, "auth.error.type"));
                        assertEquals("testservicezosmf", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("localhost:testservicezosmf:" + mockServicezosmf.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("401", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservicezosmf/api/v1/401", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zosmf", getAttribute(logBody, "auth.service.auth.method"));
                        assertNull(getAttribute(logBody, "auth.method"));
                    }

                    @Test
                    void whenExpiredToken_thenLog() {
                        given()
                            .cookie(AUTH_COOKIE, createExpiredZoweJwtToken("USER", "z/OS", "Ltpa", httpConfig.getHttpsConfig()))
                            .get(basePath + "/testservicezosmf/api/v1/401")
                        .then()
                            .statusCode(401);

                        var logRecord = assertOneLogRecordExported("/testservicezosmf/api/v1/401");
                        assertAttributesBase(logRecord.getResource().getAttributes(), port);
                        @SuppressWarnings("null")
                        var logBody = logRecord.getBodyValue().asString();
                        assertNull(getAttribute(logBody, "user.id"));
                        assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                        assertEquals("Token is expired.", getAttribute(logBody, "auth.error.message"));
                        assertEquals("org.zowe.apiml.security.common.token.TokenExpireException", getAttribute(logBody, "auth.error.type"));
                        assertEquals("testservicezosmf", getAttribute(logBody, "service.id"));
                        assertEquals("GET", getAttribute(logBody, "http.request.method"));
                        assertEquals("localhost:testservicezosmf:" + mockServicezosmf.getPort(), getAttribute(logBody, "service.instance.id"));
                        assertEquals("401", getAttribute(logBody, "service.response_code"));
                        assertEquals("/testservicezosmf/api/v1/401", getAttribute(logBody, "url.path"));
                        assertEquals("https", getAttribute(logBody, "url.scheme"));
                        assertEquals("zosmf", getAttribute(logBody, "auth.service.auth.method"));
                        assertNull(getAttribute(logBody, "auth.method"));
                    }

                }

            }

            @Nested
            class WhenAuthAbsent {

                @Test
                void thenLog() {
                    given()
                        .get(basePath + "/testservicezosmf/api/v1/401")
                    .then()
                        .statusCode(401);

                    var logRecord = assertOneLogRecordExported("/testservicezosmf/api/v1/401");
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertNull(getAttribute(logBody, "user.id"));
                    assertEquals("ERROR", getAttribute(logBody, "auth.status"));
                    assertEquals("ZWEAG160E No authentication provided in the request", getAttribute(logBody, "auth.error.message"));
                    assertEquals("org.springframework.security.authentication.InsufficientAuthenticationException", getAttribute(logBody, "auth.error.type"));
                    assertEquals("testservicezosmf", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("localhost:testservicezosmf:" + mockServicezosmf.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("401", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservicezosmf/api/v1/401", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("zosmf", getAttribute(logBody, "auth.service.auth.method"));
                    assertNull(getAttribute(logBody, "auth.method"));
                }

            }

        }

        private Object getAttribute(String logBody, String attributeName) {
            var objectMapper = new ObjectMapper();
            try {
                return objectMapper.readValue(logBody, Map.class).get(attributeName);
            } catch (JsonProcessingException e) {
                fail("Invalid JSON", e);
                return null;
            }
        }

        private String login() {
            var token = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                            "username": "USER",
                            "password": "validPassword"
                        }
                    """)
                .log().all()
            .when()
                .post(URI.create(basePath + LOGIN_ENDPOINT))
            .then()
                .statusCode(204)
                .cookie(AUTH_COOKIE)
            .extract()
                .cookie(AUTH_COOKIE);

            setUp(); // clean up log emitted from the login
            return token;
        }

    }

}
