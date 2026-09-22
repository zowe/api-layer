/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.swagger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.netflix.eureka.EurekaServiceInstance;
import org.springframework.http.HttpStatusCode;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.apicatalog.exceptions.ApiDocNotFoundException;
import org.zowe.apiml.apicatalog.functional.ApiCatalogFunctionalTest;
import org.zowe.apiml.config.ApiInfo;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

class ApiDocRetrievalServiceRestTest {

    @Nested
    @TestPropertySource(properties = {
        "apiml.webClientConfig.enabled=true"
    })
    class Certificate extends ApiCatalogFunctionalTest {

        @Autowired
        private ApiDocRetrievalServiceRest apiDocRetrievalServiceRest;

        @Autowired
        private WebClient webClient;

        @Autowired
        @Qualifier("webClientClientCert")
        private WebClient webClientClientCert;

        @Test
        void givenApiDocRetrievalServiceRest_whenOutboundCall_thenUsingClientCertificate() {
            var usedWebClient = (WebClient) ReflectionTestUtils.getField(apiDocRetrievalServiceRest, "webClientClientCert");
            assertSame(usedWebClient, webClientClientCert);
            assertNotSame(usedWebClient, webClient);
        }

    }

    @Nested
    @ExtendWith(MockitoExtension.class)
    class UnitTests {

        @Mock
        private ExchangeFunction exchangeFunction;

        @Mock
        private ApiInfo apiInfo;
        @Mock
        private EurekaServiceInstance instance;
        @Mock
        private ClientResponse clientResponse;

        private ApiDocRetrievalServiceRest apiDocRetrievalServiceRest;

        @BeforeEach
        void setUp() {
            var webClient = spy(WebClient.builder().exchangeFunction(exchangeFunction).build());
            apiDocRetrievalServiceRest = new ApiDocRetrievalServiceRest(webClient);

            when(instance.getServiceId()).thenReturn("service");
            when(apiInfo.getVersion()).thenReturn("1.0.0");
        }

        @ParameterizedTest
        @CsvSource({
            "localhost,8080,https://localhost:8080"
        })
        void givenValidSwaggerUrl_thenFetch(String serviceHost, int servicePort, String swaggerUrl) {
            when(apiInfo.getSwaggerUrl()).thenReturn(swaggerUrl);
            when(instance.getHost()).thenReturn(serviceHost);
            when(instance.getPort()).thenReturn(servicePort);

            when(exchangeFunction.exchange(any(ClientRequest.class))).thenReturn(Mono.just(clientResponse));
            when(clientResponse.statusCode()).thenReturn(HttpStatusCode.valueOf(200));
            when(clientResponse.bodyToMono(String.class)).thenReturn(Mono.just("api doc"));

            StepVerifier.create(apiDocRetrievalServiceRest.retrieveApiDoc(instance, apiInfo))
                .expectNextMatches(apiDoc ->
                    "api doc".equals(apiDoc.getApiDocContent())
                )
                .verifyComplete();


        }

        @ParameterizedTest
        @CsvSource({
            ",-1,https://:/",
            "locahost,8080,https://localhost:8081",
            "localhost2,8080,https://localhost:8080"
        })
        void givenInvalidUrl_thenDoNotFetch(String serviceHost, int servicePort, String swaggerUrl) {
            when(apiInfo.getSwaggerUrl()).thenReturn(swaggerUrl);
            when(instance.getHost()).thenReturn(serviceHost == null ? "" : serviceHost);
            when(instance.getPort()).thenReturn(servicePort);

            StepVerifier.create(apiDocRetrievalServiceRest.retrieveApiDoc(instance, apiInfo))
                .verifyErrorMatches(e -> {
                    return e instanceof ApiDocNotFoundException && e.getMessage().equals("Swagger URL validation failed");
                });

        }

    }

}
