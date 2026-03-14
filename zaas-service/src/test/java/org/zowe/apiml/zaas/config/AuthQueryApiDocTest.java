/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Regression test for issue #4159.
 *
 * <p>Guards the {@code /zaas/api/v1/auth/query} endpoint summary and operationId against
 * accidental revert. The operationId {@code validateUsingGET} must NOT be renamed because
 * SDK code generators reference it by name; renaming it is a breaking API change.</p>
 */
class AuthQueryApiDocTest {

    private static final String API_DOC_PATH = "/zaas-api-doc.json";
    private static final String QUERY_PATH = "/zaas/api/v1/auth/query";

    @Test
    void givenZaasApiDoc_whenReadingAuthQueryEndpoint_thenSummaryContainsUserInformation() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream(API_DOC_PATH)) {
            assertNotNull(in, "zaas-api-doc.json not found on test classpath");
            JsonNode root = mapper.readTree(in);
            JsonNode summary = root.path("paths").path(QUERY_PATH).path("get").path("summary");
            assertNotNull(summary, "summary field not found for " + QUERY_PATH);
            String summaryText = summary.asText();
            assertTrue(summaryText.toLowerCase().contains("user information"),
                "Summary for " + QUERY_PATH + " should mention user information but was: " + summaryText);
        }
    }

    @Test
    void givenZaasApiDoc_whenReadingAuthQueryEndpoint_thenOperationIdIsValidateUsingGET() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        try (InputStream in = getClass().getResourceAsStream(API_DOC_PATH)) {
            assertNotNull(in, "zaas-api-doc.json not found on test classpath");
            JsonNode root = mapper.readTree(in);
            JsonNode operationId = root.path("paths").path(QUERY_PATH).path("get").path("operationId");
            assertEquals("validateUsingGET", operationId.asText(),
                "operationId must remain 'validateUsingGET' to avoid breaking SDK code generators");
        }
    }

    // Workaround for missing static import in this simple test
    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
