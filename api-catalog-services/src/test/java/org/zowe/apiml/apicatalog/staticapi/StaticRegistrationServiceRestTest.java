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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.util.ReflectionTestUtils;
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
import static org.mockito.ArgumentCaptor.forClass;

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

    @BeforeEach
    void init() {
        var webCLient = WebClient.builder().exchangeFunction(exchangeFunction).build();
        staticServiceRest = new StaticRegistrationServiceRest(webCLient);

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
                ReflectionTestUtils.setField(staticServiceRest, "locations", new String[]{DISCOVERY_LOCATION});
                StepVerifier.create(staticServiceRest.refresh())
                    .assertNext(actualResponse -> {
                        StaticAPIResponse expectedResponse = new StaticAPIResponse(200, BODY);
                        assertEquals(expectedResponse, actualResponse);
                    })
                    .verifyComplete();
            }

            @Test
            void givenRefreshAPIWithUnSecureDiscoveryService_thenReturnApiResponseCodeWithBody() {
                ReflectionTestUtils.setField(staticServiceRest, "locations", new String[]{DISCOVERY_LOCATION_HTTP});

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
                    ReflectionTestUtils.setField(staticServiceRest, "locations", discoveryLocations);

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
                    ReflectionTestUtils.setField(staticServiceRest, "locations", discoveryLocations);

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
                    ReflectionTestUtils.setField(staticServiceRest, "locations", discoveryLocations);
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
        ReflectionTestUtils.setField(staticServiceRest, "locations", new String[]{});

        StepVerifier.create(staticServiceRest.refresh())
            .assertNext(actualResponse -> {
                StaticAPIResponse expectedResponse = new StaticAPIResponse(500, "Error making static API refresh request to the Discovery Service");
                assertEquals(expectedResponse, actualResponse);
            })
            .verifyComplete();
    }

    @Nested
    class EurekaAuthorization {

        @Test
        void givenCredentials_whenSetCredentials_thenSetAuthorizationHeader() {
            var service = new StaticRegistrationServiceRest(null);
            ReflectionTestUtils.setField(service, "discoveryUserid", "user");
            ReflectionTestUtils.setField(service, "discoveryPassword", "password");

            var headers = mock(HttpHeaders.class);
            service.setAuthorization(headers);

            verify(headers).add(HttpHeaders.AUTHORIZATION, "Basic dXNlcjpwYXNzd29yZA==");
        }

        @ParameterizedTest
        @CsvSource({
            ",password,,",
            "user,,",
            ",,"
        })
        void givenIncompleteCredentials_whenSetCredentials_thenDoNotSetAuthorization(String userId, String password) {
            var service = new StaticRegistrationServiceRest(null);
            ReflectionTestUtils.setField(service, "discoveryUserid", userId);
            ReflectionTestUtils.setField(service, "discoveryPassword", password);

            var headers = mock(HttpHeaders.class);
            service.setAuthorization(headers);

            verify(headers, never()).add(any(), any());
        }

    }

    @Nested
    class GivenSslVerificationDisabled {

        @BeforeEach
        void setup() {
            ReflectionTestUtils.setField(staticServiceRest, "discoveryUserid", "user");
            ReflectionTestUtils.setField(staticServiceRest, "discoveryPassword", "password");
            ReflectionTestUtils.setField(staticServiceRest, "verifySslCertificatesOfServices", false);
            doReturn(Mono.just(clientResponse)).when(exchangeFunction).exchange(any());
            doReturn(Mono.empty()).when(clientResponse).releaseBody();
            doReturn(HttpStatusCode.valueOf(SC_OK)).when(clientResponse).statusCode();
            doReturn(Mono.just(BODY)).when(clientResponse).bodyToMono(String.class);
        }

        @Test
        void whenHttpsDiscoveryService_thenAuthorizationHeaderIsSet() {
            ReflectionTestUtils.setField(staticServiceRest, "locations", new String[]{DISCOVERY_LOCATION});

            var requestCaptor = forClass(ClientRequest.class);
            StepVerifier.create(staticServiceRest.refresh()).expectNextCount(1).verifyComplete();

            verify(exchangeFunction).exchange(requestCaptor.capture());
            assertEquals("Basic dXNlcjpwYXNzd29yZA==",
                requestCaptor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
        }
    }

    @Nested
    class GivenSslVerificationEnabled {

        @BeforeEach
        void setup() {
            ReflectionTestUtils.setField(staticServiceRest, "discoveryUserid", "user");
            ReflectionTestUtils.setField(staticServiceRest, "discoveryPassword", "password");
            ReflectionTestUtils.setField(staticServiceRest, "verifySslCertificatesOfServices", true);
            doReturn(Mono.just(clientResponse)).when(exchangeFunction).exchange(any());
            doReturn(Mono.empty()).when(clientResponse).releaseBody();
            doReturn(HttpStatusCode.valueOf(SC_OK)).when(clientResponse).statusCode();
            doReturn(Mono.just(BODY)).when(clientResponse).bodyToMono(String.class);
        }

        @Test
        void whenHttpsDiscoveryService_thenNoAuthorizationHeader() {
            ReflectionTestUtils.setField(staticServiceRest, "locations", new String[]{DISCOVERY_LOCATION});

            var requestCaptor = org.mockito.ArgumentCaptor.forClass(ClientRequest.class);
            StepVerifier.create(staticServiceRest.refresh()).expectNextCount(1).verifyComplete();

            verify(exchangeFunction).exchange(requestCaptor.capture());
            assertEquals(null, requestCaptor.getValue().headers().getFirst(HttpHeaders.AUTHORIZATION));
        }
    }

}
