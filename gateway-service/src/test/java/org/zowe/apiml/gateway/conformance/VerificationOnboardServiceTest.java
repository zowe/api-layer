/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.conformance;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationOnboardServiceTest {

    @InjectMocks
    private VerificationOnboardService verificationOnboardService;

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private RestTemplate restTemplate;


    @Test
    void whenCheckingOnboardedService() {
        when(discoveryClient.getServices()).thenReturn(new ArrayList<>(Collections.singleton("OnboardedService")));
        assertFalse(verificationOnboardService.checkOnboarding("Test"));
        assertTrue(verificationOnboardService.checkOnboarding("OnboardedService"));
    }

    @Test
    void whenRetrievingSwaggerUrl() {
        final String swaggerUrl = "https://hostname/sampleclient/api-doc";
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", swaggerUrl);
        assertEquals(swaggerUrl, verificationOnboardService.findSwaggerUrl(metadata));
    }

    @Test
    void whenRetrievingnNullSwaggerUrl() {
        final String swaggerUrl = null;
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", swaggerUrl);
        assertEquals("", verificationOnboardService.findSwaggerUrl(metadata));
    }


    @Test
    void whenRetrievingEmptySwaggerUrl() {
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", null);
        assertEquals("", verificationOnboardService.findSwaggerUrl(metadata));
    }


    @Nested
    class givenEndpoint {
        @Test
        void whenEndpointReturnsDocumented400_thenReturnEmptyList() {
            String url = "https://localhost:8000/test";
            ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            Set<HttpMethod> methods = new HashSet<>();
            methods.add(HttpMethod.GET);

            HashMap<String, Set<String>> responses = new HashMap<>();
            responses.put("GET", new HashSet<>(Collections.singleton("400")));


            when(restTemplate.getForEntity(url, String.class)).thenReturn(response);
            Endpoint endpoint = new Endpoint(url, "testservice", methods, responses);
            HashSet<Endpoint> endpoints = new HashSet<>();
            endpoints.add(endpoint);
            List<String> result = verificationOnboardService.testGetEndpoints(endpoints);

            assertTrue(result.isEmpty());

        }

        @Test
        void whenEndpointReturnsUndocumented500_thenReturnCorrectError() {
            String url = "https://localhost:8000/test";
            ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            Set<HttpMethod> methods = new HashSet<>();
            methods.add(HttpMethod.GET);

            HashMap<String, Set<String>> responses = new HashMap<>();
            responses.put("GET", new HashSet<>(Collections.singleton("0")));

            when(restTemplate.getForEntity(url, String.class)).thenReturn(response);
            Endpoint endpoint = new Endpoint(url, "testservice", methods, responses);
            HashSet<Endpoint> endpoints = new HashSet<>();
            endpoints.add(endpoint);
            List<String> result = verificationOnboardService.testGetEndpoints(endpoints);
            assertTrue(result.get(0).contains("gives undocumented"));
        }
    }

    @Test
    void whenEndpointReturnsOnly404_thenReturnCorrectError() {
        String url = "https://localhost:8000/test";
        ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.NOT_FOUND);
        Set<HttpMethod> methods = new HashSet<>();
        methods.add(HttpMethod.GET);

        HashMap<String, Set<String>> responses = new HashMap<>();
        responses.put("GET", new HashSet<>(Collections.singleton("404")));

        when(restTemplate.getForEntity(url, String.class)).thenReturn(response);
        Endpoint endpoint = new Endpoint(url, "testservice", methods, responses);
        HashSet<Endpoint> endpoints = new HashSet<>();
        endpoints.add(endpoint);
        List<String> result = verificationOnboardService.testGetEndpoints(endpoints);
        assertTrue(result.get(0).contains("Could not verify if API can be called through gateway"));
    }


}

