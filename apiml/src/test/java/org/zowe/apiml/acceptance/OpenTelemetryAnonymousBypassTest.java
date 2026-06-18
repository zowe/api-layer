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
import io.opentelemetry.sdk.logs.data.LogRecordData;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.MockService;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.awaitility.Awaitility.await;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that bypass-authentication routes emit OTel log signals with
 * {@code user.id=anonymous} and {@code auth.status=OK} instead of null
 * attributes (see #4704). Covers both successful (200) and downstream
 * error (500) scenarios.
 */
class OpenTelemetryAnonymousBypassTest {

    @Nested
    @AcceptanceTest
    @ActiveProfiles({"OpenTelemetryTest"})
    @TestPropertySource(
        properties = {
            "otel.sdk.disabled=false",
            "otel.metrics.exporter=none",
            "otel.traces.exporter=none",
            "otel.logs.exporter=none"
        }
    )
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenAnonymousBypassRequest extends AcceptanceTestWithMockServices {

        @Autowired
        private InMemoryLogRecordExporter logExporter;

        @LocalServerPort
        private int port;

        private MockService mockServiceBypass;

        @BeforeAll
        void init() {
            mockServiceBypass = mockService("testservicebp")
                .scope(MockService.Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.BYPASS)
                .addEndpoint("/testservicebp/200")
                    .responseCode(200)
                .and()
                .addEndpoint("/testservicebp/500")
                    .responseCode(500)
                .and().start();
        }

        @BeforeEach
        void setUp() {
            logExporter.reset();
        }

        private List<LogRecordData> assertLogsExported() {
            List<LogRecordData> logs = new ArrayList<>();
            await("Log export")
                .atMost(Duration.ofSeconds(10))
                .until(() -> {
                    var l = logExporter.getFinishedLogRecordItems();
                    if (l.size() > 0) {
                        logs.addAll(l);
                    }
                    logExporter.reset();
                    return l.size() > 0;
                });
            return logs;
        }

        private LogRecordData assertOneLogRecordExported(String expectedUrl) {
            var logs = assertLogsExported();

            var logRecord = logs.stream()
                .filter(log -> log.getBodyValue().asString().contains(expectedUrl))
                .findFirst()
                .orElseThrow(() -> {
                    var availableUrls = logs.stream()
                        .map(log -> log.getBodyValue().asString())
                        .collect(Collectors.joining(", "));
                    return new AssertionError(
                        "Expected log record with URL " + expectedUrl + " not found in logs: " + availableUrls);
                });

            assertEquals("INFO", logRecord.getSeverityText(),
                "Expected INFO log level, was " + logRecord.getSeverityText());

            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));

            return logRecord;
        }

        private Map<String, Object> parseLogBody(String logBody) {
            try {
                return new ObjectMapper().readValue(logBody, Map.class);
            } catch (JsonProcessingException e) {
                fail("Invalid JSON: " + logBody, e);
                return Map.of();
            }
        }

        @Test
        void thenLogWithAnonymousUserIdAndAuthOk() {
            given()
                .get(basePath + "/testservicebp/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported("/testservicebp/api/v1/200");
            @SuppressWarnings("null")
            Map<String, Object> logBody = parseLogBody(logRecord.getBodyValue().asString());

            // Full attribute set verification
            assertEquals("GET", logBody.get("http.request.method"),
                "http.request.method should be GET");
            assertEquals("https", logBody.get("url.scheme"),
                "url.scheme should be https");
            assertEquals("/testservicebp/api/v1/200", logBody.get("url.path"),
                "url.path should match request path");
            assertEquals("testservicebp", logBody.get("service.id"),
                "service.id should be testservicebp");
            assertEquals("localhost:testservicebp:" + mockServiceBypass.getPort(),
                logBody.get("service.instance.id"),
                "service.instance.id should match mock service");
            assertEquals("bypass", logBody.get("auth.service.auth.method"),
                "auth.service.auth.method should be bypass");
            assertEquals("200", logBody.get("service.response_code"),
                "service.response_code should be 200");

            // NEW behavior: anonymous user ID and OK auth status
            assertEquals("anonymous", logBody.get("user.id"),
                "user.id should be 'anonymous' for bypass routes");
            assertEquals("OK", logBody.get("auth.status"),
                "auth.status should be 'OK' for bypass routes");

            // Error attributes should NOT be present for successful bypass
            assertNull(logBody.get("auth.error.type"),
                "auth.error.type should be null for successful bypass");
            assertNull(logBody.get("auth.error.message"),
                "auth.error.message should be null for successful bypass");
        }

        @Test
        void thenLogWithErrorAttributesOnRoutingError() {
            given()
                .get(basePath + "/testservicebp/api/v1/500")
            .then()
                .statusCode(500);

            var logRecord = assertOneLogRecordExported("/testservicebp/api/v1/500");
            @SuppressWarnings("null")
            Map<String, Object> logBody = parseLogBody(logRecord.getBodyValue().asString());

            // Standard attributes still present
            assertEquals("GET", logBody.get("http.request.method"));
            assertEquals("https", logBody.get("url.scheme"));
            assertEquals("/testservicebp/api/v1/500", logBody.get("url.path"));
            assertEquals("testservicebp", logBody.get("service.id"));
            assertEquals("localhost:testservicebp:" + mockServiceBypass.getPort(),
                logBody.get("service.instance.id"));
            assertEquals("bypass", logBody.get("auth.service.auth.method"));
            assertEquals("500", logBody.get("service.response_code"),
                "service.response_code should be 500 for error endpoint");

            // Anonymous user ID and OK auth status still apply (bypass auth succeeded)
            assertEquals("anonymous", logBody.get("user.id"),
                "user.id should be 'anonymous' for bypass routes even on error");
            assertEquals("OK", logBody.get("auth.status"),
                "auth.status should be 'OK' for bypass routes even on error");

            // Error attributes: service error but auth succeeded, so no auth.error attributes
            assertNull(logBody.get("auth.error.type"),
                "auth.error.type should be null — bypass auth always succeeds");
            assertNull(logBody.get("auth.error.message"),
                "auth.error.message should be null — bypass auth always succeeds");
        }
    }
}
