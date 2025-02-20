/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.functional.gateway;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.zowe.apiml.util.categories.RateLimitTest;
import org.zowe.apiml.util.config.ConfigReader;
import org.zowe.apiml.util.http.HttpRequestUtils;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;
import java.time.Duration;
import java.util.stream.IntStream;

@RateLimitTest
public class InMemoryRateLimiterFilterFactoryIntegrationTest {

    private static WebTestClient client;

    final int bucketCapacity = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration().getBucketCapacity();

    @BeforeAll
    static void setUpTester() {
        String baseUrl = HttpRequestUtils.getUriFromGateway("/discoverableclient/api/v1/greeting").toString();
        SslContext sslContext;
        try {
            sslContext = SslContextBuilder
                .forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build();
        } catch (SSLException e) {
            throw new RuntimeException(e);
        }
        SslContext finalSslContext = sslContext;
        HttpClient httpClient = HttpClient.create().secure(t -> t.sslContext(finalSslContext));

        client =
            WebTestClient.bindToServer().clientConnector(new ReactorClientHttpConnector(httpClient))
                .responseTimeout(Duration.ofSeconds(30L))
                .baseUrl(baseUrl)
                .build();
    }

    @Test
    void testRateLimitingWhenAllowedWithCookie() {
        client.get()
            .cookie("apimlAuthenticationToken", "someCookie")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testRateLimitingWhenExceeded() {
        IntStream.range(0, bucketCapacity).parallel().forEach(i -> client.get()
            .cookie("apimlAuthenticationToken", "validTokenValue")
            .exchange().expectStatus().isOk());


        client.get()
            .cookie("apimlAuthenticationToken", "validTokenValue")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectBody()
            .jsonPath("$.messages[0].messageReason").isEqualTo("Connections limit exceeded.");
    }

    @Test
    void testRateLimiterAllowsAccessToAnotherUser() {
        // the first user requires access
        IntStream.range(0, bucketCapacity).parallel().forEach(i -> client.get()
            .cookie("apimlAuthenticationToken", "theFirstUser")
            .exchange().expectStatus().isOk());
        //access should be denied
        client.get()
            .cookie("apimlAuthenticationToken", "theFirstUser")
            .exchange()
            .expectStatus().isEqualTo(HttpStatus.TOO_MANY_REQUESTS)
            .expectBody()
            .jsonPath("$.messages[0].messageReason").isEqualTo("Connections limit exceeded.");

        // the second user requires access
        client.get()
            .cookie("apimlAuthenticationToken", "theSecondUser")
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testRateLimitingWithEmptyCookie() {
        client.get()
            .exchange()
            .expectStatus().isOk();
    }

    @Test
    void testRateLimitingWithEmptyKey() {
        WebTestClient customClient = client.mutate()
            .baseUrl(HttpRequestUtils.getUriFromGateway("/apicatalog/api/v1/containers").toString())
            .build();

        customClient.get()
            .exchange()
            .expectStatus().isUnauthorized();
    }

}
