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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.MockService.Scope;
import org.zowe.apiml.util.config.SslContext;
import org.zowe.apiml.util.config.SslContextConfigurer;
import org.zowe.apiml.zaas.security.mapping.OIDCExternalMapper;
import org.zowe.apiml.zaas.security.mapping.X509NativeMapper;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProvider;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

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
    @ActiveProfiles({"OpenTelemetryTest", "zos"})
    @TestPropertySource(
        properties = {
            "otel.sdk.disabled=false",
            "otel.metrics.exporter=none",
            "otel.traces.exporter=none",
            "otel.logs.exporter=none"
        }
    )
    @DirtiesContext
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
            "apiml.security.filterChainConfiguration=new"
        }
    )
    @ActiveProfiles({"OpenTelemetryTest", "zos"})
    class WhenOnboardedService extends AcceptanceTestWithMockServices {

        private static final String VALID_OIDC_TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";

        @Autowired
        private LogRecordExporter logExporter;

        @MockitoBean
        private OIDCExternalMapper oidcExternalMapper;

        @MockitoBean
        private OIDCTokenProvider oidcTokenProvider;

        @MockitoBean
        private X509NativeMapper x509TokenProvider;

        private MockService mockServiceZoweJwt;
        private MockService mockServicePassTicket;
        private MockService mockServicePassTicketMisconfigured;
        private MockService mockServiceBypass;
        private MockService mockServiceWs;

        @BeforeAll
        void startMockServices() throws Exception {
            SslContextConfigurer configurer = new SslContextConfigurer("password".toCharArray(), "../keystore/client_cert/client-certs.p12", "../keystore/localhost/localhost.keystore.p12");
            SslContext.prepareSslAuthentication(configurer);

            mockServiceZoweJwt = mockService("testservice")
                .scope(Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.ZOWE_JWT)
                .addEndpoint("/testservice/200")
                .responseCode(200)
            .and().start();

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

            mockServiceBypass = mockService("testservicebp")
                .scope(Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.BYPASS)
                .addEndpoint("/testservicebp/200")
                .responseCode(200)
            .and().start();

            mockServiceWs = mockServiceWs("testservicews")
                .scope(Scope.CLASS)
                .start();
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

        private LogRecordData assertOneLogRecordExported() {
            var logs = assertLogsExported();
            assertEquals(1, logs.size());

            var logRecord = logs.get(0);
            assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());

            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));

            return logRecord;
        }

        @Test
        void givenRouted_whenAuthFail_thenLog() {
            given()
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();

            assertAttributesBase(logRecord.getResource().getAttributes(), port);
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();
            assertEquals("testservice", getAttribute(logBody, "service.id"));
            assertEquals("GET", getAttribute(logBody, "http.request.method"));
            assertEquals("ERROR", getAttribute(logBody, "auth.status"));
            assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
            assertEquals("200", getAttribute(logBody, "service.response_code"));
            assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
            assertEquals("https", getAttribute(logBody, "url.scheme"));
            assertNull(getAttribute(logBody, "auth.method"));
            assertEquals("zoweJwt", getAttribute(logBody, "auth.service.auth.method"));
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

        @Test
        void givenLoginEndpoint_thenLog() {
            given()
                .auth().preemptive()
                .basic("wronguser", "wrongpass")
                .post(basePath + "/gateway/api/v1/auth/login")
            .then()
                .statusCode(401);

            var logRecord = assertOneLogRecordExported();
            assertAttributesBase(logRecord.getResource().getAttributes(), port);
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();
            assertEquals("gateway", getAttribute(logBody, "service.id"));
            assertEquals("POST", getAttribute(logBody, "http.request.method"));
            assertEquals("ERROR", getAttribute(logBody, "auth.status"));
            assertEquals("EACCES: Permission is denied; the specified password is incorrect", getAttribute(logBody, "auth.error.message"));
            assertEquals("Unauthorized", getAttribute(logBody, "auth.error.type"));
            assertEquals("localhost:gateway:" + port, getAttribute(logBody, "service.instance.id"));
            assertEquals("401", getAttribute(logBody, "service.response_code"));
            assertEquals("/gateway/api/v1/auth/login", getAttribute(logBody, "url.path"));
            assertEquals("https", getAttribute(logBody, "url.scheme"));
            assertEquals("basicAuth", getAttribute(logBody, "auth.service.auth.method"));
        }

        @Test
        void givenCatalogEndpoint_thenLog() {
            given()
                .get(basePath + "/apicatalog/ui/v1/index.html")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();
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
        void givenRouted_withAuthJwt_success_thenLog() {
            given()
                .cookie("apimlAuthenticationToken", login())
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();
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
        void givenRouted_withAuthJwt_failure_thenLog() {
        }

        @Test
        void givenNoRoute_thenLog() {
            given()
                .get(basePath + "/nonexistant/api/v1/200")
            .then()
                .statusCode(404);

            var logRecord = assertOneLogRecordExported();
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

        @Test
        void givenRouted_withAuthPassTicketSucess_thenLog() {
            given()
                .cookie(AUTH_COOKIE, login())
                .get(basePath + "/testservicept/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();
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

        @Test
        void givenRouted_withBypass_thenLog() {
            given()
                .cookie(AUTH_COOKIE, login())
                .get(basePath + "/testservicebp/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();
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

        @Test
        void givenRouted_withMisconfiguredAuthPassTicket_thenLog() {
            given()
                .cookie(AUTH_COOKIE, login())
                .get(basePath + "/testservicepterror/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();
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

        @Test
        void givenRouted_withOidc_success_thenLog() {
            when(oidcTokenProvider.isValid(VALID_OIDC_TOKEN)).thenReturn(true);
            when(oidcExternalMapper.mapToMainframeUserId(any())).thenReturn("USER");

            given()
                .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_OIDC_TOKEN)
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();
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
        void givenRouted_withOidc_failure_thenLog() {
            when(oidcTokenProvider.isValid(VALID_OIDC_TOKEN)).thenReturn(false);

            given()
                .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_OIDC_TOKEN)
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);
        }

        @Test
        void givenRouted_withX509_success_thenLog() {
            given()
                .config(SslContext.clientCertUser)
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported();
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
        void givenRouter_withX509_failure_thenLog() {
            given()
                .config(SslContext.tlsWithoutCert)
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);

            when(x509TokenProvider.isValid(any())).thenReturn(false);
        }

        @Test
        void givenRouter_withWs_success_thenLog() {
            given()
                .get(basePath + "/testservicews/api/v1/200")
            .then()
                .statusCode(200);
        }

        @Test
        void givenRouter_withPAT_success_thenLog() {

        }

        @Test
        void givenRouter_withPAT_failure_thenLog() {

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
