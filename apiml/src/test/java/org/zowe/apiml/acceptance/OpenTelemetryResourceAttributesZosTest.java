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
import org.zowe.apiml.zaas.security.mapping.OIDCExternalMapper;
import org.zowe.apiml.zaas.security.service.token.OIDCTokenProvider;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.restassured.RestAssured.given;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.*;

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
    @ActiveProfiles({ "OpenTelemetryTest", "zos" })
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
            "apiml.security.oidc.validationType=endpoint",
            "apiml.security.oidc.enabled=true",
            "apiml.security.oidc.userInfo.uri=https://oidc.provider.com/user/info",
            "apiml.security.filterChainConfiguration=new"
        }
    )
    @ActiveProfiles({ "OpenTelemetryTest", "zos" })
    class WhenOnboardedService extends AcceptanceTestWithMockServices {

        private static final String VALID_OIDC_TOKEN = "ewogICJ0eXAiOiAiSldUIiwKICAibm9uY2UiOiAiYVZhbHVlVG9CZVZlcmlmaWVkIiwKICAiYWxnIjogIlJTMjU2IiwKICAia2lkIjogIlNlQ1JldEtleSIKfQ.ewogICJhdWQiOiAiMDAwMDAwMDMtMDAwMC0wMDAwLWMwMDAtMDAwMDAwMDAwMDAwIiwKICAiaXNzIjogImh0dHBzOi8vb2lkYy5wcm92aWRlci5vcmcvYXBwIiwKICAiaWF0IjogMTcyMjUxNDEyOSwKICAibmJmIjogMTcyMjUxNDEyOSwKICAiZXhwIjogODcyMjUxODEyNSwKICAic3ViIjogIm9pZGMudXNlcm5hbWUiCn0.c29tZVNpZ25lZEhhc2hDb2Rl";

        @Autowired
        private LogRecordExporter logExporter;

        @MockitoBean
        private OIDCExternalMapper mapper;

        @MockitoBean
        private OIDCTokenProvider oidcTokenProvider;

        private MockService mockServiceZoweJwt;
        private MockService mockServicePassTicket;

        @BeforeAll
        void startMockServices() {
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

        @Test
        void givenRouted_whenAuthFail_thenLog() {
            given()
                .get(basePath + "/testservice/api/v1/200")
            .then()
            .statusCode(200);

            var logs = assertLogsExported();

            assertTrue(
                logs.stream()
                .allMatch(logRecord -> {
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertTrue(StringUtils.isNotBlank(logBody));
                    assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());
                    assertEquals("testservice", getAttribute(logBody, "service.id"));
                    assertEquals("GET", getAttribute(logBody, "http.request.method"));
                    assertEquals("FAILED", getAttribute(logBody, "auth.status"));
                    assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
                    assertEquals("200", getAttribute(logBody, "service.response_code"));
                    assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
                    assertEquals("https", getAttribute(logBody, "url.scheme"));
                    assertEquals("zoweJwt", getAttribute(logBody, "auth.method"));

                    return true;
                })
            );

        }

        private Object getAttribute(String logBody, String attributeName) {
            var mapper = new ObjectMapper();
            Map<?, ?> map;
            try {
                map = mapper.readValue(logBody, Map.class);
            } catch (JsonProcessingException e) {
                map = new HashMap<>();
            }
            var value = map.get(attributeName);
            return value;
        }

        @Test
        @Disabled("This test is for invalid authentication (server error). To be reviewed in follow up story")
        void givenLoginEndpoint_thenLog() {
            given()
                .auth().preemptive()
                .basic("wronguser", "wrongpass")
                .post(basePath + "/gateway/api/v1/auth/login")
            .then()
                .statusCode(500);

            var logs = assertLogsExported();

            var logRecord = logs.get(0);
            assertAttributesBase(logRecord.getResource().getAttributes(), port);
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));
            assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());
            assertEquals("apicatalog", getAttribute(logBody, "service.id"));
            assertEquals("GET", getAttribute(logBody, "http.request.method"));
            assertEquals("FAILED", getAttribute(logBody, "auth.status"));
            assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
            assertEquals("200", getAttribute(logBody, "service.response_code"));
            assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
            assertEquals("https", getAttribute(logBody, "url.scheme"));
            assertEquals("zoweJwt", getAttribute(logBody, "auth.method"));
        }

        @Test
        void givenCatalogEndpoint_thenLog() {
            given()
                .get(basePath + "/apicatalog/ui/v1/index.html")
            .then()
                .statusCode(200);

            var logs = assertLogsExported();
            assertEquals(1, logs.size());

            var logRecord = logs.get(0);
            assertAttributesBase(logRecord.getResource().getAttributes(), port);
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));
            assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());
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
        void givenRouted_withAuthSuccess_thenLog() {
            given()
                .cookie("apimlAuthenticationToken", login())
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);

            var logs = assertLogsExported();
            assertEquals(1, logs.size());

            var logRecord = logs.get(0);
            assertAttributesBase(logRecord.getResource().getAttributes(), port);
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));
            assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());
            assertEquals("testservice", getAttribute(logBody, "service.id"));
            assertEquals("GET", getAttribute(logBody, "http.request.method"));
            assertEquals("OK", getAttribute(logBody, "auth.status"));
            assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
            assertEquals("200", getAttribute(logBody, "service.response_code"));
            assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
            assertEquals("https", getAttribute(logBody, "url.scheme"));
            assertEquals("zoweJwt", getAttribute(logBody, "auth.method"));
        }

        @Test
        void givenRoutedWitArgs_withAuthSuccess_thenLog() {

        }

        @Test
        void givenRouted_withAuthPassTicketSucess_thenLog() {
            given()
                .cookie(AUTH_COOKIE, login())
                .get(basePath + "/testservicept/api/v1/200")
            .then()
                .statusCode(200);

            var logs = assertLogsExported(); // This now includes the login request
            assertEquals(1, logs.size());

            var logRecord = logs.get(0);
            assertAttributesBase(logRecord.getResource().getAttributes(), port);
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));
            assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());
            assertEquals("testservicept", getAttribute(logBody, "service.id"));
            assertEquals("GET", getAttribute(logBody, "http.request.method"));
            assertEquals("OK", getAttribute(logBody, "auth.status"));
            assertEquals("localhost:testservicept:" + mockServicePassTicket.getPort(), getAttribute(logBody, "service.instance.id"));
            assertEquals("200", getAttribute(logBody, "service.response_code"));
            assertEquals("/testservicept/api/v1/200", getAttribute(logBody, "url.path"));
            assertEquals("https", getAttribute(logBody, "url.scheme"));
            assertEquals("httpBasicPassTicket", getAttribute(logBody, "auth.method"));
        }

        @Test
        void givenRouted_withOidc_thenLog() {
            given()
                .header(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + VALID_OIDC_TOKEN)
                .get(basePath + "/testservice/api/v1/200")
            .then()
                .statusCode(200);

            var logs = assertLogsExported();
            assertEquals(1, logs.size());

            var logRecord = logs.get(0);
            assertAttributesBase(logRecord.getResource().getAttributes(), port);
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));
            assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());
            assertEquals("testservice", getAttribute(logBody, "service.id"));
            assertEquals("GET", getAttribute(logBody, "http.request.method"));
            assertNull(getAttribute(logBody, "auth.status"));
            assertEquals("localhost:testservice:" + mockServiceZoweJwt.getPort(), getAttribute(logBody, "service.instance.id"));
            assertEquals("200", getAttribute(logBody, "service.response_code"));
            assertEquals("/testservice/api/v1/200", getAttribute(logBody, "url.path"));
            assertEquals("https", getAttribute(logBody, "url.scheme"));
            assertEquals("zoweJwt", getAttribute(logBody, "auth.method"));
        }

        @Test
        void givenRouted_withX509_thenLog() {

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
