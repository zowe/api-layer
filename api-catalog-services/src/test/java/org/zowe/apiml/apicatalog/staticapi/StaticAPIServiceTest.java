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

import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.zowe.apiml.apicatalog.discovery.DiscoveryConfigProperties;
import org.zowe.apiml.util.HttpClientMockHelper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StaticAPIServiceTest {

    private static final String REFRESH_ENDPOINT = "discovery/api/v1/staticApi";

    private static final String DISCOVERY_LOCATION = "https://localhost:60004/eureka/";
    private static final String DISCOVERY_LOCATION_2 = "https://localhost:60005/eureka/";
    private static final String DISCOVERY_LOCATION_3 = "https://localhost:60006/eureka/";
    private static final String DISCOVERY_URL = "https://localhost:60004/";

    private static final String DISCOVERY_LOCATION_HTTP = "http://localhost:60004/eureka/";
    private static final String DISCOVERY_URL_HTTP = "http://localhost:60004/";

    @InjectMocks
    private StaticAPIService staticAPIService;

    @Mock
    private CloseableHttpClient httpClient;
    @Mock
    private CloseableHttpResponse okResponse;
    @Mock
    private CloseableHttpResponse notFoundResponse;
    @Mock
    private HttpEntity entity;

    @Mock
    private DiscoveryConfigProperties discoveryConfigProperties;

    private final String[] discoveryLocations = {DISCOVERY_LOCATION, DISCOVERY_LOCATION_2};
    private static final String BODY = "This is body";

    @Nested
    class WhenRefreshEndpointPresentsResponseTest {

        @Nested
        class GivenSingleUrlTest {

            @BeforeEach
            void setup() throws IOException {
                when(okResponse.getCode()).thenReturn(HttpStatus.OK.value());

                when(okResponse.getEntity()).thenReturn(entity);
                when(entity.getContent()).thenReturn(new ByteArrayInputStream(BODY.getBytes()));
            }

            @Test
            void givenRefreshAPIWithSecureDiscoveryService_thenReturnApiResponseCodeWithBody() throws IOException {

                when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION});
                mockRestTemplateExchange(DISCOVERY_URL);

                StaticAPIResponse actualResponse = staticAPIService.refresh();
                StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                assertEquals(expectedResponse, actualResponse);
            }

            @Test
            void givenRefreshAPIWithUnSecureDiscoveryService_thenReturnApiResponseCodeWithBody() throws IOException {
                when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{DISCOVERY_LOCATION_HTTP});

                mockRestTemplateExchange(DISCOVERY_URL_HTTP);
                StaticAPIResponse actualResponse = staticAPIService.refresh();
                StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                assertEquals(expectedResponse, actualResponse);
            }
        }

        @Nested
        class GivenTwoDiscoveryUrlsTest {
            @Nested
            class WhenOneSucceedsTest {
                @BeforeEach
                void setup() throws IOException {
                    when(okResponse.getCode()).thenReturn(HttpStatus.OK.value());

                    when(okResponse.getEntity()).thenReturn(entity);
                    when(entity.getContent()).thenReturn(new ByteArrayInputStream(BODY.getBytes()));
                }

                @Test
                void whenFirstSucceeds_thenReturnResponseFromFirst() throws IOException {
                    when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);
                    mockRestTemplateExchange(DISCOVERY_LOCATION);
                    StaticAPIResponse actualResponse = staticAPIService.refresh();
                    StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                    assertEquals(expectedResponse, actualResponse);
                }

                @Test
                void whenFirstFails_thenReturnResponseFromSecond() throws IOException {
                    when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);
                    when(notFoundResponse.getCode()).thenReturn(HttpStatus.NOT_FOUND.value());
                    mockRestTemplateExchange(DISCOVERY_LOCATION_2);
                    StaticAPIResponse actualResponse = staticAPIService.refresh();
                    StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                    assertEquals(expectedResponse, actualResponse);
                }
            }

            @Nested
            class WhenBothFailsTest {
                @Test
                void whenBothFail_thenReturnResponseFromSecond() throws IOException {
                    when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);
                    when(notFoundResponse.getCode()).thenReturn(HttpStatus.NOT_FOUND.value());
                    when(notFoundResponse.getEntity()).thenReturn(entity);
                    when(entity.getContent()).thenAnswer(invocation -> new ByteArrayInputStream(BODY.getBytes()));
                    mockRestTemplateExchange(DISCOVERY_LOCATION_3);

                    StaticAPIResponse actualResponse = staticAPIService.refresh();
                    StaticAPIResponse expectedResponse = new StaticAPIResponse(404, BODY);
                    assertEquals(expectedResponse, actualResponse);
                }
            }
        }
    }


    @Test
    void givenNoDiscoveryLocations_whenAttemptRefresh_thenReturn500() {
        when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{});

        StaticAPIResponse actualResponse = staticAPIService.refresh();
        StaticAPIResponse expectedResponse = new StaticAPIResponse(500, "Error making static API refresh request to the Discovery Service");
        assertEquals(expectedResponse, actualResponse);
    }

    private void mockRestTemplateExchange(String discoveryUrl) throws IOException {
        HttpPost post = new HttpPost(discoveryUrl.replace("/eureka", "") + REFRESH_ENDPOINT);

        when(httpClient.execute(any(ClassicHttpRequest.class), any(HttpClientResponseHandler.class))).thenAnswer((invocation) -> {
            HttpPost httpRequest = (HttpPost) invocation.getArguments()[0];
            URI uri = httpRequest.getUri();
            int i = uri.compareTo(post.getUri());

            return HttpClientMockHelper.invokeResponseHandler(invocation, i == 0 ? okResponse : notFoundResponse);
        });
    }
}
