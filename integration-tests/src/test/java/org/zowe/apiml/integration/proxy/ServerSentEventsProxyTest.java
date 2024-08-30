/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.integration.proxy;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.FluxExchangeResult;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.util.categories.TestsNotMeantForZowe;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.config.GatewayServiceConfiguration;
import reactor.netty.http.client.HttpClient;
import reactor.test.StepVerifier;

import javax.net.ssl.SSLException;
import java.time.Duration;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.zowe.apiml.util.requests.Endpoints.DISCOVERABLE_SSE_EVENTS;

@TestsNotMeantForZowe
public class ServerSentEventsProxyTest {
    private static final GatewayServiceConfiguration gatewayConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();

    private static WebTestClient webTestClient;

    @BeforeAll
    static void setup() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient()
            .trustManager(InsecureTrustManagerFactory.INSTANCE).build();
        HttpClient httpClient = HttpClient.create().secure(ssl -> {
            ssl.sslContext(sslContext);
        });
        ClientHttpConnector httpConnector = new ReactorClientHttpConnector(
            httpClient);

        String baseUrl = String.format("%s://%s:%d", gatewayConfiguration.getScheme(), gatewayConfiguration.getHost(), gatewayConfiguration.getPort());
        webTestClient = WebTestClient.bindToServer(httpConnector).baseUrl(baseUrl).build();
    }

    @Nested
    class WhenRoutingSession {
        @Test
        void givenValidSsePaths_thenReturnEvents() {
            FluxExchangeResult<String> fluxResult = webTestClient
                .get()
                .uri(DISCOVERABLE_SSE_EVENTS)
                .exchange()
                .expectStatus()
                .isOk()
                .returnResult(String.class);

            StepVerifier.create(fluxResult.getResponseBody())
                .expectNext("event")
                .expectNext("event")
                .thenCancel()
                .verify(Duration.ofSeconds(3));
        }

        @Nested
        class GivenIncorrectPath_thenReturnError {
            @Test
            void givenInvalidServiceId() {
                String path = "/bad/sse/v1/events";
                FluxExchangeResult<String> fluxResult = webTestClient
                    .get()
                    .uri(path)
                    .exchange()
                    .expectStatus()
                    .isNotFound()
                    .returnResult(String.class);

                String response = fluxResult.getResponseBody().next().block();
                assertThat(response, is("ZWEAG700E No instance of the service 'bad' found. Routing will not be available."));
            }

            @Test
            void givenInvalidVersion() {
                String path = "/discoverableclient/sse/bad/events";
                FluxExchangeResult<String> fluxResult = webTestClient
                    .get()
                    .uri(path)
                    .exchange()
                    .expectStatus()
                    .isNotFound()
                    .returnResult(String.class);

                String response = fluxResult.getResponseBody().next().block();
                assertThat(response, is("ZWEAM104E The endpoint you are looking for 'sse/bad' could not be located"));
            }

            @Test
            void givenNoServiceId() {
                FluxExchangeResult<String> fluxResult = webTestClient
                    .get()
                    .uri("/sse/v1")
                    .exchange()
                    .expectStatus()
                    .isBadRequest()
                    .returnResult(String.class);

                String response = fluxResult.getResponseBody().next().block();
                assertThat(response, is("ZWEAG712E The URI '/sse/v1' is an invalid format"));
            }
        }
    }
}
