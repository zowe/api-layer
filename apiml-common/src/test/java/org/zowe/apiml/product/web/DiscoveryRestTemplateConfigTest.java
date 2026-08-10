/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.product.web;

import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.io.CloseMode;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javax.net.ssl.SSLContext;

import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(MockitoExtension.class)
class DiscoveryRestTemplateConfigTest {

    private DiscoveryRestTemplateConfig discoveryRestTemplateConfig;

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException {
        this.discoveryRestTemplateConfig = new DiscoveryRestTemplateConfig();
    }

    @Test
    void giveSecureEurekaUrl_thenCreateSSLConfig() throws NoSuchAlgorithmException {
        var connectionManager = discoveryRestTemplateConfig.httpClientConnectionManager(SSLContext.getDefault(), new NoopHostnameVerifier());
        var args = discoveryRestTemplateConfig.defaultArgs("https://localhost:10011", SSLContext.getDefault(), new NoopHostnameVerifier(), connectionManager);
        assertTrue(args.getSSLContext().isPresent());
    }

    @Test
    void giveUnsecureEurekaUrl_thenDontCreateSSLConfig() throws NoSuchAlgorithmException {
        var connectionManager = discoveryRestTemplateConfig.httpClientConnectionManager(SSLContext.getDefault(), new NoopHostnameVerifier());
        var args = discoveryRestTemplateConfig.defaultArgs("http://localhost:10011", SSLContext.getDefault(), new NoopHostnameVerifier(), connectionManager);
        assertFalse(args.getSSLContext().isPresent());
    }

    @Nested
    class GivenRestTemplateConfig {

        @Test
        void whenSupplierInvokedRepeatedly_thenEvictorThreadsUnderControl() throws Exception {
            var connectionManager = discoveryRestTemplateConfig.httpClientConnectionManager(SSLContext.getDefault(), new NoopHostnameVerifier());
            var supplier = DiscoveryRestTemplateConfig.getDefaultEurekaClientHttpRequestFactorySupplier(connectionManager);

            var before = countThreadsMatching("idle-connection-evictor-.*"); // default name in Apache HTTP Client

            var factories = new ArrayList<>();
            for (int i = 0; i < 50; i++) {
                factories.add(supplier.get(SSLContext.getDefault(), new NoopHostnameVerifier()));
            }

            var after = countThreadsMatching("idle-connection-evictor-.*");

            assertTrue(after - before <= 1, "before threads: " + before + ". after threads: " + after);

            factories
                .stream()
                .map(HttpComponentsClientHttpRequestFactory.class::cast)
                .forEach(f -> ((CloseableHttpClient) f.getHttpClient()).close(CloseMode.IMMEDIATE));
        }

        private long countThreadsMatching(String regex) {
            return Thread.getAllStackTraces().keySet().stream()
                .filter(t -> t.getName().matches(regex))
                .count();
        }

    }

}
