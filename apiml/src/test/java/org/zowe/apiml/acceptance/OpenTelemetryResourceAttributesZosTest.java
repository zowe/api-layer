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
import io.opentelemetry.sdk.logs.export.LogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryLogRecordExporter;
import io.opentelemetry.sdk.testing.exporter.InMemoryMetricReader;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.zowe.apiml.auth.AuthenticationScheme;
import org.zowe.apiml.gateway.MockService;
import org.zowe.apiml.gateway.MockService.Scope;

import java.util.HashMap;
import java.util.Map;

import static io.opentelemetry.api.common.AttributeKey.stringKey;
import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
            "otel.logs.exporter=none"
        }
    )
    @ActiveProfiles({ "OpenTelemetryTest", "zos" })
    class WhenOnboardedService extends AcceptanceTestWithMockServices {

        @Autowired
        private LogRecordExporter logExporter;

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

        @Test
        void givenRouted_thenLog() {
            given()
                .get(basePath + "/testservice/api/v1/200")
            .then()
            .statusCode(200);

            logExporter.flush();
            var logs = ((InMemoryLogRecordExporter) logExporter).getFinishedLogRecordItems();
            assertFalse(logs.isEmpty(), "No logs received");
            assertEquals(1, logs.size());

            assertTrue(
                logs.stream()
                .allMatch(logRecord -> {
                    assertAttributesBase(logRecord.getResource().getAttributes(), port);
                    @SuppressWarnings("null")
                    var logBody = logRecord.getBodyValue().asString();
                    assertTrue(StringUtils.isNotBlank(logBody));
                    assertEquals("INFO", logRecord.getSeverityText(), "Expected INFO log level, was " + logRecord.getSeverityText());
                    assertEquals("TESTSERVICE", getAttribute(logBody, "service.id"));
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
            assertNotNull(value);
            return value;
        }

        @Test
        void givenLoginEndpoint_thenLog() {

            // given()
            //     .post(basePath + "/gateway/api/v1/auth/login");


        }

        @Test
        void givenCatalogEndpoint_thenLog() {

        }

        @Test
        void givenRouted_withAuth_thenLog() {

        }

    }

}
