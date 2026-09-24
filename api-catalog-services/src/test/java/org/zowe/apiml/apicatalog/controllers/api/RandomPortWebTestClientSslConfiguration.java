/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.apicatalog.controllers.api;

import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webtestclient.autoconfigure.WebTestClientBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import reactor.netty.http.client.HttpClient;

import javax.net.ssl.SSLException;

/**
 * Makes the injected {@code WebTestClient} trust the TLS certificate of the test server.
 * <p>
 * {@code @SpringBootTest(webEnvironment = RANDOM_PORT)} starts a real HTTPS listener using the
 * self-signed certificate configured by the {@code test} profile
 * ({@code server.ssl.trustStore}/{@code keyStore} under {@code ../keystore/service}). The
 * {@code WebTestClient} that {@code @AutoConfigureWebTestClient} injects is built by
 * {@code WebTestClientAutoConfiguration} with no trust material of its own, so it previously
 * inherited a usable SSL context from the Spring Boot 3 auto-configuration path. In Spring Boot 4
 * that path no longer applies one, and the client fails the handshake with
 * {@code PKIX path building failed: unable to find valid certification path to requested target}.
 * <p>
 * This supplies the missing trust material for tests only, using the same
 * {@link InsecureTrustManagerFactory} approach already used by the repository's integration tests
 * (for example {@code BookControllerTest} and {@code ServerSentEventsProxyTest}). It is a test
 * configuration and does not affect production code or production TLS behaviour in any way.
 */
@TestConfiguration(proxyBeanMethods = false)
public class RandomPortWebTestClientSslConfiguration {

    /**
     * Installs a client connector that trusts the test server's certificate.
     * <p>
     * The trust scope is deliberately narrow: this bean exists only in a test context, and the only
     * endpoint it is ever pointed at is the locally started test server on {@code localhost}.
     *
     * @return the customizer applied to the injected {@code WebTestClient} builder
     */
    @Bean
    public WebTestClientBuilderCustomizer trustTestServerCertificateCustomizer() {
        return builder -> builder.clientConnector(new ReactorClientHttpConnector(
            HttpClient.create().secure(spec -> {
                try {
                    spec.sslContext(SslContextBuilder.forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build());
                } catch (SSLException e) {
                    throw new IllegalStateException("Unable to build the test client SSL context", e);
                }
            })
        ));
    }

}
