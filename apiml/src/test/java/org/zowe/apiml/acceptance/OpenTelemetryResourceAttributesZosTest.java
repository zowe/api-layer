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
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.MockService.Scope;
import org.zowe.apiml.zaas.security.mapping.OIDCExternalMapper;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

class OpenTelemetryResourceAttributesZosTest {

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
            "apiml.security.saf.provider=saf"
        }
    )
    @ActiveProfiles({ "OpenTelemetryTest", "zos" })
    class WhenOnboardedService extends AcceptanceTestWithMockServices {

        @Autowired
        private LogRecordExporter logExporter;

        @MockitoBean
        private OIDCExternalMapper mapper;

        private MockService mockService;

        @BeforeAll
        void startMockServices() {
            mockService = mockService("testservice")
                .scope(Scope.CLASS)
                .authenticationScheme(AuthenticationScheme.ZOWE_JWT)
                .addEndpoint("/testservice/200")
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
                    assertEquals("localhost:testservice:" + mockService.getPort(), getAttribute(logBody, "service.instance.id"));
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
            assertEquals("localhost:testservice:" + mockService.getPort(), getAttribute(logBody, "service.instance.id"));
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

            // TODO mock auth result
            fail("not implemented yet");
        }

        @Test
        void givenRouted_withOidc_thenLog() {
            // TODO mock OIDC result

            fail("not implemented yet");
        }

    }

}
