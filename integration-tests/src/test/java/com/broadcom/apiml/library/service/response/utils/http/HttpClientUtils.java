/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.broadcom.apiml.library.service.response.utils.http;

import com.broadcom.apiml.library.service.response.utils.config.ConfigReader;
import com.broadcom.apiml.library.service.response.utils.config.GatewayServiceConfiguration;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.slf4j.Logger;

import javax.net.ssl.SSLContext;

import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

public class HttpClientUtils {
    private static final Logger log = org.slf4j.LoggerFactory.getLogger(HttpClientUtils.class);

    private HttpClientUtils() {
    }

    public static HttpClient client() {
        return client(false);
    }

    public static HttpClient client(boolean disableRedirectHandling) {
        GatewayServiceConfiguration serviceConfiguration = ConfigReader.environmentConfiguration().getGatewayServiceConfiguration();
        String scheme = serviceConfiguration.getScheme();

        HttpClientBuilder builder;
        if (scheme.equals("https")) {
            builder = httpsClient();
        } else {
            builder = httpClient();
        }
        if (disableRedirectHandling) {
            return builder.disableRedirectHandling().build();
        }

        return builder.build();
    }

    public static SSLContext ignoreSslContext() {
        try {
            return new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            log.warn("SSL context creation failed: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }

    private static HttpClientBuilder httpClient() {
        return HttpClientBuilder.create();
    }

    private static HttpClientBuilder httpsClient() {
        return HttpClients.custom().setSSLContext(ignoreSslContext())
            .setSSLHostnameVerifier(new NoopHostnameVerifier());
    }
}
