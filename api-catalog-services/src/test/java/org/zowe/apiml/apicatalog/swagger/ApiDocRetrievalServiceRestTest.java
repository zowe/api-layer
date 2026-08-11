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

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.ssl.SslBundles;
import org.springframework.cloud.gateway.config.HttpClientProperties;
import org.springframework.cloud.gateway.config.HttpClientSslConfigurer;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.common.config.WebClientConfig;

import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

class ApiDocRetrievalServiceRestTest {

    @Nested
    @ExtendWith(SpringExtension.class)
    @Import({
        ApiDocRetrievalServiceRest.class,
        WebClientConfig.class,
        HttpConfig.class,
        HttpClientProperties.class,
        ServerProperties.class,
        HttpClientSslConfigurer.class,
        HttpClientProperties.class
    })
    @TestPropertySource(properties = {
        "apiml.webClientConfig.enabled=true",
        "server.ssl.keyAlias=localhost",
        "server.ssl.keyStore=../keystore/localhost/localhost.keystore.p12",
        "server.ssl.keyStorePassword=password",
        "server.ssl.keyPassword=password",
        "server.ssl.trustStore=../keystore/localhost/localhost.truststore.p12",
        "server.ssl.trustStorePassword=password"
    })
    @MockitoBean(types = {
        HttpClientProperties.Ssl.class,
        SslBundles.class
    })
    class Certificate {

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

}
