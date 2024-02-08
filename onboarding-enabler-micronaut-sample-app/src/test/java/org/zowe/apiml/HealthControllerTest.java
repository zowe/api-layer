/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;


import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.eurekaservice.model.Health;

import jakarta.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
class HealthControllerTest {

    @Inject
    @Client("/micronautdiscoverableclient/application")
    HttpClient client;

    @Test
    void testHealth() throws JsonProcessingException {
        HttpRequest<String> request = HttpRequest.GET("/health");
        String body = client.toBlocking().retrieve(request);

        assertNotNull(body);
        Health health = new Health("UP");
        ObjectMapper om = new ObjectMapper();
        String expectedResponse = om.writeValueAsString(health);
        assertEquals(expectedResponse, body);
    }

    @Test
    void testInfo() throws JsonProcessingException {
        HttpRequest<String> request = HttpRequest.GET("/info");
        String body = client.toBlocking().retrieve(request);

        assertNotNull(body);
        Map<String, String> info = new HashMap<>();
        ObjectMapper om = new ObjectMapper();
        String expectedResponse = om.writeValueAsString(info);
        assertEquals(expectedResponse, body);
    }
}
