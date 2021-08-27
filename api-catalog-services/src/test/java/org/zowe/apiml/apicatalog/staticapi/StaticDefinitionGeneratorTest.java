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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.*;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.client.RestTemplate;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;
import org.zowe.apiml.security.common.token.TokenAuthentication;

import java.io.IOException;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class StaticDefinitionGeneratorTest {
    private static final String STATIC_DEF_GENERATE_ENDPOINT = "static-api/generate";
    private static final String STATIC_DEF_OVERRIDE_ENDPOINT = "static-api/override";
    private static final String ACTUATOR_ENV = "application/env";

    private static final String DISCOVERY_LOCATION = "https://localhost:60004/eureka/";
    private static final String DISCOVERY_LOCATION_2 = "https://localhost:60005/eureka/";
    private static final String DISCOVERY_URL = "https://localhost:60004/";
    private static final String DISCOVERY_URL_2 = "https://localhost:60005/";

    private static final String DISCOVERY_LOCATION_HTTP = "http://localhost:60004/eureka/";
    private static final String DISCOVERY_URL_HTTP = "http://localhost:60004/";

    @InjectMocks
    private StaticDefinitionGenerator staticDefinitionGenerator;

    @Mock
    private RestTemplate restTemplate;
    @Mock
    private DiscoveryConfigProperties discoveryConfigProperties;

    @Nested
    class WhenStaticDefinitionGenerationResponse {

        @Test
        void givenInvalidRequest_thenThrowException() {
            when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION});
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            mockRestTemplateExchange(DISCOVERY_URL, new ResponseEntity<>(HttpStatus.OK), ACTUATOR_ENV);
            assertThrows(IOException.class, () ->
                staticDefinitionGenerator.generateFile("services: \\n  "));
        }
    }

    @Nested
    class WhenStaticDefinitionOverrideResponse {

        @Test
        void givenInvalidRequest_thenThrowException() {
            when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION});
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            mockRestTemplateExchange(DISCOVERY_URL, new ResponseEntity<>(HttpStatus.OK), ACTUATOR_ENV);
            assertThrows(IOException.class, () ->
                staticDefinitionGenerator.overrideFile("services: \\n  "));
        }
    }

    private void mockRestTemplateExchange(String discoveryUrl, ResponseEntity expectedResponse, String path) {
        when(restTemplate.exchange(
            discoveryUrl + path,
            HttpMethod.GET, getHttpEntity(discoveryUrl), String.class
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
