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
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
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
    @DirtiesContext
    @TestInstance(TestInstance.Lifecycle.PER_CLASS)
    class WhenAnonymousBypassRequest extends AcceptanceTestWithMockServices {

        @Autowired
        private LogRecordExporter logExporter;

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
            assertTrue(logExporter instanceof InMemoryLogRecordExporter,
                "Expected InMemoryLogRecordExporter, got " + logExporter.getClass().getName());
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
                .orElseThrow(() -> new AssertionError(
                    "Expected log record with URL " + expectedUrl + " not found in logs: "
                    + logs.stream().map(LogRecordData::getBodyValue).map(String::valueOf).collect(Collectors.joining(", "))));

            assertEquals("INFO", logRecord.getSeverityText(),
                "Expected INFO log level, was " + logRecord.getSeverityText());

            var logBody = logRecord.getBodyValue().asString();
            assertTrue(StringUtils.isNotBlank(logBody));

            return logRecord;
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
        void thenLogWithAnonymousUserIdAndAuthOk() {
            given()
                .get(basePath + "/testservicebp/api/v1/200")
            .then()
                .statusCode(200);

            var logRecord = assertOneLogRecordExported("/testservicebp/api/v1/200");
            @SuppressWarnings("null")
            var logBody = logRecord.getBodyValue().asString();

            // Full attribute set verification
            assertEquals("GET", getAttribute(logBody, "http.request.method"),
                "http.request.method should be GET");
            assertEquals("https", getAttribute(logBody, "url.scheme"),
                "url.scheme should be https");
            assertEquals("/testservicebp/api/v1/200", getAttribute(logBody, "url.path"),
                "url.path should match request path");
            assertEquals("testservicebp", getAttribute(logBody, "service.id"),
                "service.id should be testservicebp");
            assertEquals("localhost:testservicebp:" + mockServiceBypass.getPort(),
                getAttribute(logBody, "service.instance.id"),
                "service.instance.id should match mock service");
            assertEquals("bypass", getAttribute(logBody, "auth.service.auth.method"),
                "auth.service.auth.method should be bypass");
            assertEquals("200", getAttribute(logBody, "service.response_code"),
                "service.response_code should be 200");

            // NEW behavior: anonymous user ID and OK auth status
            assertEquals("anonymous", getAttribute(logBody, "user.id"),
                "user.id should be 'anonymous' for bypass routes");
            assertEquals("OK", getAttribute(logBody, "auth.status"),
                "auth.status should be 'OK' for bypass routes");

            // Error attributes should NOT be present for successful bypass
            assertNull(getAttribute(logBody, "auth.error.type"),
                "auth.error.type should be null for successful bypass");
            assertNull(getAttribute(logBody, "auth.error.message"),
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
            var logBody = logRecord.getBodyValue().asString();

            // Standard attributes still present
            assertEquals("GET", getAttribute(logBody, "http.request.method"));
            assertEquals("https", getAttribute(logBody, "url.scheme"));
            assertEquals("/testservicebp/api/v1/500", getAttribute(logBody, "url.path"));
            assertEquals("testservicebp", getAttribute(logBody, "service.id"));
            assertEquals("localhost:testservicebp:" + mockServiceBypass.getPort(),
                getAttribute(logBody, "service.instance.id"));
            assertEquals("bypass", getAttribute(logBody, "auth.service.auth.method"));
            assertEquals("500", getAttribute(logBody, "service.response_code"),
                "service.response_code should be 500 for error endpoint");

            // Anonymous user ID and OK auth status still apply (bypass auth succeeded)
            assertEquals("anonymous", getAttribute(logBody, "user.id"),
                "user.id should be 'anonymous' for bypass routes even on error");
            assertEquals("OK", getAttribute(logBody, "auth.status"),
                "auth.status should be 'OK' for bypass routes even on error");

            // Error attributes: service error but auth succeeded, so no auth.error attributes
            assertNull(getAttribute(logBody, "auth.error.type"),
                "auth.error.type should be null — bypass auth always succeeds");
            assertNull(getAttribute(logBody, "auth.error.message"),
                "auth.error.message should be null — bypass auth always succeeds");
        }
    }
}
