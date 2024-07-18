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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VerificationOnboardServiceTest {

    @Mock
    private DiscoveryClient discoveryClient;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ResponseEntity<String> responseEntity;

    @InjectMocks
    private VerificationOnboardService verificationOnboardService;

    @Test
    void whenCheckingOnboardedService_thenCorrectResults() {
        when(discoveryClient.getServices()).thenReturn(new ArrayList<>(Collections.singleton("OnboardedService")));
        assertFalse(verificationOnboardService.checkOnboarding("Test"));
        assertTrue(verificationOnboardService.checkOnboarding("OnboardedService"));
    }

    @Test
    void whenRetrievingSwaggerUrl_thenCorrectlyRetrieves() {
        final String swaggerUrl = "https://hostname/sampleclient/api-doc";
        HashMap<String, String> metadata = new HashMap<>();
        metadata.put("apiml.apiInfo.api-v2.swaggerUrl", swaggerUrl);
        Optional<String> result = verificationOnboardService.findSwaggerUrl(metadata);
        assertTrue(result.isPresent());
        assertEquals(swaggerUrl, result.get());
    }

    @Test
    void whenGetSwagger_thenOk() {
        when(restTemplate.exchange(
            anyString(),
            any(HttpMethod.class),
            any(),
            ArgumentMatchers.<Class<String>>any()))
            .thenReturn(responseEntity);
        when(responseEntity.getBody()).thenReturn("returned");

        assertEquals("returned", verificationOnboardService.getSwagger("mock"));
    }


    @Nested
    class GivenMetadata {
        @Test
        void whenRetrievingNullSwaggerUrl_thenEmptyMetadata() {
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("apiml.apiInfo.api-v2.swaggerUrl", null);
            assertFalse(verificationOnboardService.findSwaggerUrl(metadata).isPresent());
        }

        @Test
        void whenRetrievingEmptySwaggerUrl_thenEmptyMetadata() {
            HashMap<String, String> metadata = new HashMap<>();
            metadata.put("apiml.apiInfo.api-v2.swaggerUrl", null);
            assertFalse(verificationOnboardService.findSwaggerUrl(metadata).isPresent());
        }

        @Test
        void whenSwaggerUrlNotInMetadata_thenReturnsNull() {
            Map<String, String> mockMetadata = new HashMap<>();
            mockMetadata.put("", null);
            assertFalse(verificationOnboardService.findSwaggerUrl(mockMetadata).isPresent());
        }

        @Test
        void whenDoesntSupportSSO_thenFalse() {
            Map<String, String> mockMetadata = new HashMap<>();
            mockMetadata.put("swaggerUrl", "x");
            assertFalse(VerificationOnboardService.supportsSSO(mockMetadata));
        }
    }

    @Nested
    class GivenEndpoint {

        @BeforeEach
        void setup() throws URISyntaxException {
            setUpZaasService();
        }

        private void setUpZaasService() throws URISyntaxException {
            ServiceInstance serviceInstance = mock(ServiceInstance.class);
            when(serviceInstance.getUri()).thenReturn(new URI("https://localhost:1000"));
            when(discoveryClient.getServices()).thenReturn(asList("zaas"));
            when(discoveryClient.getInstances("zaas")).thenReturn(asList(serviceInstance));
        }

        @Test
        void whenEndpointNotFound_thenReturnCorrectError() {
            String url = "https://localhost:8000/test";
            ResponseEntity<String> response = new ResponseEntity<>("ZWEAM104E", HttpStatus.NOT_FOUND);
            Set<HttpMethod> methods = new HashSet<>();
            methods.add(HttpMethod.GET);

            HashMap<String, Set<String>> responses = new HashMap<>();
            responses.put("GET", new HashSet<>(Collections.singleton("404")));

            doReturn(response).when(restTemplate).exchange(eq("https://localhost:1000/zaas/validate/auth"), any(), any(), eq(String.class));
            doReturn(response).when(restTemplate).exchange(eq(url), any(), any(), eq(String.class));
            Endpoint endpoint = new Endpoint(url, "testservice", methods, responses);
            HashSet<Endpoint> endpoints = new HashSet<>();
            endpoints.add(endpoint);
            List<String> result = verificationOnboardService.testEndpointsByCalling(endpoints, "dummy");
            assertTrue(result.get(0).contains("could not be located, attempting to call it through gateway gives the ZWEAM104E"));
        }

        @Test
        void whenEndpointReturnsDocumented400_thenReturnEmptyList() {
            String url = "https://localhost:8000/test";
            ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.BAD_REQUEST);
            Set<HttpMethod> methods = new HashSet<>();
            methods.add(HttpMethod.GET);

            HashMap<String, Set<String>> responses = new HashMap<>();
            responses.put("GET", new HashSet<>(Collections.singleton("400")));

            doReturn(response).when(restTemplate).exchange(eq("https://localhost:1000/zaas/validate/auth"), any(), any(), eq(String.class));
            doReturn(response).when(restTemplate).exchange(eq(url), any(), any(), eq(String.class));
            Endpoint endpoint = new Endpoint(url, "testservice", methods, responses);
            HashSet<Endpoint> endpoints = new HashSet<>();
            endpoints.add(endpoint);
            List<String> result = verificationOnboardService.testEndpointsByCalling(endpoints, "dummy");

            assertTrue(result.isEmpty());
        }

        @Test
        void whenEndpointReturnsDocumented200Response_thenReturnEmptyList() {
            String url = "https://localhost:8000/test";
            ResponseEntity<String> response = new ResponseEntity<>(HttpStatus.OK);
            Set<HttpMethod> methods = new HashSet<>();
            methods.add(HttpMethod.GET);

            HashMap<String, Set<String>> responses = new HashMap<>();
            responses.put("GET", new HashSet<>(Collections.singleton("200")));

            doReturn(response).when(restTemplate).exchange(eq("https://localhost:1000/zaas/validate/auth"), any(), any(), eq(String.class));
            doReturn(response).when(restTemplate).exchange(eq(url), any(), any(), eq(String.class));
            Endpoint endpoint = new Endpoint(url, "testservice", methods, responses);
            HashSet<Endpoint> endpoints = new HashSet<>();
            endpoints.add(endpoint);
            List<String> result = verificationOnboardService.testEndpointsByCalling(endpoints, "dummy");

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

            doReturn(response).when(restTemplate).exchange(eq("https://localhost:1000/zaas/validate/auth"), any(), any(), eq(String.class));
            doReturn(response).when(restTemplate).exchange(eq(url), any(), any(), eq(String.class));

            Endpoint endpoint = new Endpoint(url, "testservice", methods, responses);
            HashSet<Endpoint> endpoints = new HashSet<>();
            endpoints.add(endpoint);
            List<String> result = verificationOnboardService.testEndpointsByCalling(endpoints, "dummy");
            assertTrue(result.get(0).contains("returns undocumented"));
        }

    }

}

