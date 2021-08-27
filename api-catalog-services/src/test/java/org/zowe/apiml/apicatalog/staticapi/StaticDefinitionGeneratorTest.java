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
    private static final String ACTUATOR_ENV = "application/env";

    private static final String DISCOVERY_LOCATION = "https://localhost:60004/eureka/";
    private static final String DISCOVERY_URL = "https://localhost:60004/";

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
            assertThrows(IOException.class, () ->
                staticDefinitionGenerator.generateFile("services: \\n  "));
        }

        @Test
        void givenValidRequest_thenThrowExceptionWithCorrectPath() {
            when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION});
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            mockRestTemplateExchange(DISCOVERY_URL, new ResponseEntity<>("{ \"apiml.discovery.staticApiDefinitionsDirectories\": {\n" +
                "                    \"value\": \"config/local/api-defs\",\n" +
                "                    \"origin\": \"URL [file:config/local/discovery-service.yml]:9:42\"\n }" +
                "                },", HttpStatus.OK), ACTUATOR_ENV);
            Exception exception = assertThrows(IOException.class, () ->
                staticDefinitionGenerator.generateFile("services: \\n serviceId: service\\n "));
            assertEquals("./config/local/api-defs/service.yml (No such file or directory)", exception.getMessage());
        }

        @Test
        void givenHttpValidRequest_thenThrowExceptionWithCorrectPath() {
            when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION_HTTP});
            TokenAuthentication authentication = new TokenAuthentication("token");
            authentication.setAuthenticated(true);
            SecurityContextHolder.setContext(new SecurityContextImpl(authentication));
            mockRestTemplateExchange(DISCOVERY_URL_HTTP, new ResponseEntity<>("{ \"apiml.discovery.staticApiDefinitionsDirectories\": {\n" +
                "                    \"value\": \"config/local/api-defs\",\n" +
                "                    \"origin\": \"URL [file:config/local/discovery-service.yml]:9:42\"\n }" +
                "                },", HttpStatus.OK), ACTUATOR_ENV);
            Exception exception = assertThrows(IOException.class, () ->
                staticDefinitionGenerator.generateFile("services: \\n serviceId: service\\n "));
            assertEquals("./config/local/api-defs/service.yml (No such file or directory)", exception.getMessage());
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
        String token;
        if (isHttp) {
            token = "Basic " + Base64.getEncoder().encodeToString("null:null".getBytes());
            httpHeaders.add("Authorization", token);
        }
        else {
            token = "apimlAuthenticationToken=token";
            httpHeaders.add("Cookie", token);
        }

        return new HttpEntity<>(null, httpHeaders);
    }
}
