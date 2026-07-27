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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClientConfig;

import javax.net.ssl.SSLContextSpi;
import javax.net.ssl.X509KeyManager;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiDocRetrievalServiceRestTest {

    @Nested
    @SpringBootTest
    class Certificate {

        @Autowired
        private ApiDocRetrievalServiceRest apiDocRetrievalServiceRest;

        @Test
        void givenApiDocRetrievalServiceRest_whenOutboundCall_thenUsingClientCertificate() {
            var webclient = (WebClient) ReflectionTestUtils.getField(apiDocRetrievalServiceRest, "webClientClientCert");
            var builder = ReflectionTestUtils.getField(webclient, "builder");
            var connector = ReflectionTestUtils.getField(builder, "connector");
            var httpClient = ReflectionTestUtils.getField(connector, "httpClient");
            var config = (HttpClientConfig) ReflectionTestUtils.getField(httpClient, "config");
            var sslContext = ReflectionTestUtils.getField(config.sslProvider().getSslContext(), "sslContext");
            var contextSpi = (SSLContextSpi) ReflectionTestUtils.getField(sslContext, "contextSpi");
            var keyManager = (X509KeyManager) ReflectionTestUtils.getField(contextSpi, "keyManager");
            assertNotNull(keyManager.getPrivateKey("localhost"), "WebClient has not defined keystore with client certificate");
        }

    }

}
