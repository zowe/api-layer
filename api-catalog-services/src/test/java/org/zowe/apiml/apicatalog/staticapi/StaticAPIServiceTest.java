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

    @InjectMocks
    private StaticAPIService staticAPIService;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private DiscoveryConfigProperties discoveryConfigProperties;

    @Test
    void givenRefreshAPIWithSecureDiscoveryService_whenRefreshEndpointPresentResponse_thenReturnApiResponseCodeWithBody() {
        String discoveryUrl = "https://localhost:60004/";
        when(discoveryConfigProperties.getLocations()).thenReturn("https://localhost:60004/eureka/");

        when(restTemplate.exchange(
            discoveryUrl + REFRESH_ENDPOINT,
            HttpMethod.POST, getHttpEntity(discoveryUrl), String.class))
            .thenReturn(new ResponseEntity<>("This is body", HttpStatus.OK));


        StaticAPIResponse actualResponse = staticAPIService.refresh();
        StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "This is body");
        assertEquals(expectedResponse, actualResponse);
    }

    @Test
    void givenRefreshAPIWithUnSecureDiscoveryService_whenRefreshEndpointPresentResponse_thenReturnApiResponseCodeWithBody() {
        String discoveryUrl = "http://localhost:60004/";
        when(discoveryConfigProperties.getLocations()).thenReturn("http://localhost:60004/eureka/");

        when(restTemplate.exchange(
            discoveryUrl + REFRESH_ENDPOINT,
            HttpMethod.POST, getHttpEntity(discoveryUrl), String.class))
            .thenReturn(new ResponseEntity<>("This is body", HttpStatus.OK));

        StaticAPIResponse actualResponse = staticAPIService.refresh();
        StaticAPIResponse expectedResponse = new StaticAPIResponse(200, "This is body");
        assertEquals(expectedResponse, actualResponse);
    }

    private HttpEntity<?> getHttpEntity(String discoveryServiceUrl) {
        boolean isHttp = discoveryServiceUrl.startsWith("http://");
        HttpHeaders httpHeaders = new HttpHeaders();
        if (isHttp) {
            String basicToken = "Basic " + Base64.getEncoder().encodeToString( "null:null".getBytes());
            httpHeaders.add("Authorization", basicToken);
        }

        return new HttpEntity<>(null, httpHeaders);
    }
}
