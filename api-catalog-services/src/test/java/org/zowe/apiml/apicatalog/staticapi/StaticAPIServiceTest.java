/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.apicatalog.staticapi;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;

import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticAPIServiceTest {

    private static final String REFRESH_ENDPOINT = "discovery/api/v1/staticApi";

    private static final String DISCOVERY_LOCATION = "https://localhost:60004/eureka/";
    private static final String DISCOVERY_LOCATION_2 = "https://localhost:60005/eureka/";
    private static final String DISCOVERY_URL = "https://localhost:60004/";
    private static final String DISCOVERY_URL_2 = "https://localhost:60005/";

    private static final String DISCOVERY_LOCATION_HTTP = "http://localhost:60004/eureka/";
    private static final String DISCOVERY_URL_HTTP = "http://localhost:60004/";

    @InjectMocks
    private StaticAPIService staticAPIService;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private DiscoveryConfigProperties discoveryConfigProperties;

    @Nested
    class WhenRefreshEndpointPresentsResponse {

        @Test
        void givenRefreshAPIWithSecureDiscoveryService_thenReturnApiResponseCodeWithBody() {
            when(discoveryConfigProperties.getLocations()).thenReturn(DISCOVERY_LOCATION);

            mockRestTemplateExchange(DISCOVERY_URL, new ResponseEntity<>("This is body", HttpStatus.OK));

            StaticAPIResponse actualResponse = staticAPIService.refresh();
            StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "This is body");
            assertEquals(expectedResponse, actualResponse);
        }

        @Test
        void givenRefreshAPIWithUnSecureDiscoveryService_thenReturnApiResponseCodeWithBody() {
            when(discoveryConfigProperties.getLocations()).thenReturn(DISCOVERY_LOCATION_HTTP);

            mockRestTemplateExchange(DISCOVERY_URL_HTTP, new ResponseEntity<>("This is body", HttpStatus.OK));

            StaticAPIResponse actualResponse = staticAPIService.refresh();
            StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "This is body");
            assertEquals(expectedResponse, actualResponse);
        }

        @Nested
        class GivenTwoDiscoveryUrls {
            private final String discoveryLocations = DISCOVERY_LOCATION + "," + DISCOVERY_LOCATION_2;

            @Test
            void whenFirstFails_thenReturnResponseFromSecond() {
                when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);

                mockRestTemplateExchange(DISCOVERY_URL, new ResponseEntity<>(HttpStatus.NOT_FOUND));
                mockRestTemplateExchange(DISCOVERY_URL_2, new ResponseEntity<>("body", HttpStatus.OK));

                StaticAPIResponse actualResponse = staticAPIService.refresh();
                StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "body");
                assertEquals(expectedResponse, actualResponse);
            }

            @Test
            void whenFirstSucceeds_thenReturnResponseFromFirst() {
                when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);

                mockRestTemplateExchange(DISCOVERY_URL, new ResponseEntity<>("body", HttpStatus.OK));

                StaticAPIResponse actualResponse = staticAPIService.refresh();
                StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "body");
                assertEquals(expectedResponse, actualResponse);
            }

            @Test
            void whenBothFail_thenReturnResponseFromSecond() {
                when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);

                mockRestTemplateExchange(DISCOVERY_URL, new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR));
                mockRestTemplateExchange(DISCOVERY_URL_2, new ResponseEntity<>("body", HttpStatus.NOT_FOUND));

                StaticAPIResponse actualResponse = staticAPIService.refresh();
                StaticAPIResponse expectedResponse = new StaticAPIResponse(404, "body");
                assertEquals(expectedResponse, actualResponse);
            }
        }
    }

    @Test
    void givenNoDiscoveryLocations_whenAttemptRefresh_thenReturn500AndNullBody() {
        when(discoveryConfigProperties.getLocations()).thenReturn("");

        StaticAPIResponse actualResponse = staticAPIService.refresh();
        StaticAPIResponse expectedResponse = new StaticAPIResponse(500, null);
        assertEquals(expectedResponse, actualResponse);
    }

    private void mockRestTemplateExchange(String discoveryUrl, ResponseEntity expectedResponse) {
        when(restTemplate.exchange(
            discoveryUrl + REFRESH_ENDPOINT,
            HttpMethod.POST, getHttpEntity(discoveryUrl), String.class
        )).thenReturn(expectedResponse);
    }

    private HttpEntity<?> getHttpEntity(String discoveryServiceUrl) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Accept", "application/json");
        if (isHttp) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString("null:null".getBytes());
            httpHeaders.add("Authorization", basicToken);
        }

        return new HttpEntity<>(null, httpHeaders);
    }
}
