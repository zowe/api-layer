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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.apache.hc.core5.http.HttpStatus.SC_NOT_FOUND;
import static org.apache.hc.core5.http.HttpStatus.SC_OK;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StaticRegistrationServiceRestTest {

    private static final String BODY = "This is body";

    private static final String DISCOVERY_LOCATION = "https://localhost:60004/eureka/";
    private static final String DISCOVERY_LOCATION_HTTP = "http://localhost:60004/eureka/";
    private static final String DISCOVERY_LOCATION_2 = "https://localhost:60005/eureka/";
    private static final String[] discoveryLocations = { DISCOVERY_LOCATION, DISCOVERY_LOCATION_2 };

    private StaticRegistrationServiceRest staticServiceRest;

    @Mock
    private ExchangeFunction exchangeFunction;

    @Mock
    private ClientResponse clientResponse;

    @Mock
    private DiscoveryConfigProperties discoveryConfigProperties;

    @BeforeEach
    void init() {
        var webCLient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        staticServiceRest = new StaticRegistrationServiceRest(webCLient, discoveryConfigProperties);
    }

    @Nested
    class WhenRefreshEndpointPresentsResponseTest {

        @Nested
        class GivenSingleUrlTest {

            @BeforeEach
            void setup() {
                doReturn(Mono.just(clientResponse)).when(exchangeFunction).exchange(any());
                doReturn(Mono.empty()).when(clientResponse).releaseBody();
                doReturn(HttpStatusCode.valueOf(SC_OK)).when(clientResponse).statusCode();
                doReturn(Mono.just(BODY)).when(clientResponse).bodyToMono(String.class);
            }

            @Test
            void givenRefreshAPIWithSecureDiscoveryService_thenReturnApiResponseCodeWithBody() {
                when(discoveryConfigProperties.getLocations()).thenReturn(new String[] { DISCOVERY_LOCATION });

                StepVerifier.create(staticServiceRest.refresh())
                    .assertNext(actualResponse -> {
                        StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                        assertEquals(expectedResponse, actualResponse);
                    })
                    .verifyComplete();
            }

            @Test
            void givenRefreshAPIWithUnSecureDiscoveryService_thenReturnApiResponseCodeWithBody() {
                when(discoveryConfigProperties.getLocations()).thenReturn(new String[] { DISCOVERY_LOCATION_HTTP });

                StepVerifier.create(staticServiceRest.refresh())
                    .assertNext(actualResponse -> {
                        StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                        assertEquals(expectedResponse, actualResponse);
                    })
                    .verifyComplete();
            }

        }

        @Nested
        class GivenTwoDiscoveryUrlsTest {

            @Nested
            class WhenOneSucceedsTest {

                @Test
                void whenFirstSucceeds_thenReturnResponseFromFirst() {
                    doReturn(Mono.just(clientResponse)).when(exchangeFunction).exchange(any());
                    doReturn(Mono.empty()).when(clientResponse).releaseBody();
                    doReturn(HttpStatusCode.valueOf(SC_OK)).when(clientResponse).statusCode();
                    doReturn(Mono.just(BODY)).when(clientResponse).bodyToMono(String.class);
                    when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);

                    StepVerifier.create(staticServiceRest.refresh())
                        .assertNext(actualResponse -> {
                            StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                            assertEquals(expectedResponse, actualResponse);
                        })
                        .verifyComplete();
                }

                @Test
                void whenFirstFails_thenReturnResponseFromSecond() {
                    doAnswer(answer -> {
                        ClientRequest clientRequest = answer.getArgument(0);
                        ClientResponse clientResponse = mock(ClientResponse.class);
                        doReturn(Mono.empty()).when(clientResponse).releaseBody();
                        switch (clientRequest.url().getPort()) {
                            case 60004:
                                doReturn(HttpStatusCode.valueOf(SC_NOT_FOUND)).when(clientResponse).statusCode();
                                doReturn(Mono.just("")).when(clientResponse).bodyToMono(String.class);
                                break;
                            case 60005:
                                doReturn(HttpStatusCode.valueOf(SC_OK)).when(clientResponse).statusCode();
                                doReturn(Mono.just(BODY)).when(clientResponse).bodyToMono(String.class);
                                break;
                            default:
                                fail("Unexpected discovery service with URL: " + clientRequest.url());
                        }
                        return Mono.just(clientResponse);
                    }).when(exchangeFunction).exchange(any());
                    when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);

                    StepVerifier.create(staticServiceRest.refresh())
                        .assertNext(actualResponse -> {
                            StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                            assertEquals(expectedResponse, actualResponse);
                        })
                        .verifyComplete();
                }

            }

            @Nested
            class WhenBothFailsTest {

                @Test
                void whenBothFail_thenReturnResponseFromSecond() {
                    doReturn(Mono.just(clientResponse)).when(exchangeFunction).exchange(any());
                    doReturn(Mono.empty()).when(clientResponse).releaseBody();
                    when(discoveryConfigProperties.getLocations()).thenReturn(discoveryLocations);
                    doReturn(HttpStatusCode.valueOf(SC_NOT_FOUND)).when(clientResponse).statusCode();
                    doReturn(Mono.just(BODY)).when(clientResponse).bodyToMono(String.class);

                    StepVerifier.create(staticServiceRest.refresh())
                        .assertNext(actualResponse -> {
                            StaticAPIResponse expectedResponse = new StaticAPIResponse(404, BODY);
                            assertEquals(expectedResponse, actualResponse);
                        })
                        .verifyComplete();
                }

            }

        }

    }

    @Test
    void givenNoDiscoveryLocations_whenAttemptRefresh_thenReturn500() {
        when(discoveryConfigProperties.getLocations()).thenReturn(new String[]{});

        StepVerifier.create(staticServiceRest.refresh())
            .assertNext(actualResponse -> {
                StaticAPIResponse expectedResponse = new StaticAPIResponse(500, "Error making static API refresh request to the Discovery Service");
                assertEquals(expectedResponse, actualResponse);
            })
            .verifyComplete();
    }

}
