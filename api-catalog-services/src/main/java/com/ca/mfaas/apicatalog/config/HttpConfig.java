/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.apicatalog.config;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;

@Slf4j
@Configuration
public class HttpConfig {

    private static final String SAFKEYRING = "safkeyring";

    private final MFaaSConfigPropertiesContainer propertiesContainer;

    public HttpConfig(MFaaSConfigPropertiesContainer propertiesContainer) {
        this.propertiesContainer = propertiesContainer;
    }

    @Bean
    RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(createSslSocketFactory());
        return new RestTemplate(factory);
    }

    @Bean
    public CloseableHttpClient createSslSocketFactory() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        try {
            RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory());

            String trustStore = propertiesContainer.getSecurity().getTrustStore();
            String trustStorePassword = propertiesContainer.getSecurity().getTrustStorePassword();

            SSLContextBuilder sslContextBuilder = SSLContexts
                .custom()
                .setKeyStoreType(propertiesContainer.getSecurity().getTrustStoreType())
                .setProtocol(propertiesContainer.getSecurity().getProtocol());

            if (!trustStore.startsWith(SAFKEYRING)) {
                log.info("Loading truststore file: " + trustStore);
                FileSystemResource trustStoreResource = new FileSystemResource(trustStore);

                sslContextBuilder.loadTrustMaterial(trustStoreResource.getFile(), trustStorePassword.toCharArray());
            } else {
                log.info("Loading truststore key ring: " + trustStore);
                if (!trustStore.startsWith(SAFKEYRING + ":////")) {
                    throw new MalformedURLException("Incorrect key ring format: " + trustStore +
                        ". Make sure you use format safkeyring:////userId/keyRing");
                }
                URL keyRing = new URL(trustStore.replaceFirst("////", "//"));

                sslContextBuilder.loadTrustMaterial(keyRing, trustStorePassword.toCharArray());
            }

            SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(sslContextBuilder.build(),
                SSLConnectionSocketFactory.getDefaultHostnameVerifier());
            socketFactoryRegistryBuilder.register("https", sslSocketFactory);
            socketFactoryRegistry = socketFactoryRegistryBuilder.build();
        } catch (IOException | CertificateException | KeyStoreException | KeyManagementException |
            NoSuchAlgorithmException e) {
            throw new BeanInitializationException("Error initializing secureHttpClient", e);
        }

        PoolingHttpClientConnectionManager connectionManager
            = new PoolingHttpClientConnectionManager(Objects.requireNonNull(socketFactoryRegistry));

        return HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .disableCookieManagement()
            .disableAuthCaching()
            .build();
    }
}
