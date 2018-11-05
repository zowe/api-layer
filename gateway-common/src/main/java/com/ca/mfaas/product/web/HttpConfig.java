/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.product.web;

import lombok.extern.slf4j.Slf4j;
import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Objects;

@Slf4j
@Configuration
public class HttpConfig {

    private static final String SAFKEYRING = "safkeyring";

    @Value("${server.ssl.protocol:TLSv1.2}")
    private String protocol;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private String trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private String keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private String keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory =
            new HttpComponentsClientHttpRequestFactory(secureHttpClient());
        return new RestTemplate(factory);
    }

    @Bean
    public CloseableHttpClient secureHttpClient() {
        Registry<ConnectionSocketFactory> socketFactoryRegistry;
        RegistryBuilder<ConnectionSocketFactory> socketFactoryRegistryBuilder = RegistryBuilder.<ConnectionSocketFactory>create()
            .register("http", PlainConnectionSocketFactory.getSocketFactory());

        socketFactoryRegistryBuilder.register("https", createSslSocketFactory());
        socketFactoryRegistry = socketFactoryRegistryBuilder.build();

        PoolingHttpClientConnectionManager connectionManager
            = new PoolingHttpClientConnectionManager(Objects.requireNonNull(socketFactoryRegistry));
        return HttpClientBuilder.create()
            .setConnectionManager(connectionManager)
            .disableCookieManagement()
            .disableAuthCaching()
            .build();
    }

    private ConnectionSocketFactory createSslSocketFactory() {
        if (verifySslCertificatesOfServices) {
            return createSecureSslSocketFactory();
        } else {
            log.warn("The gateway is not verifying the TLS/SSL certificates of the services");
            return createIgnoringSslSocketFactory();
        }
    }

    private ConnectionSocketFactory createIgnoringSslSocketFactory() {
        return new SSLConnectionSocketFactory(ignoringSslContext(), new NoopHostnameVerifier());
    }

    private SSLContext ignoringSslContext() {
        try {
            return new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new BeanInitializationException("Error initializing HTTP client " + e.getMessage(), e);
        }
    }

    private void loadTrustMaterial(SSLContextBuilder sslContextBuilder)
            throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException {
        if (trustStore != null) {
            sslContextBuilder.setKeyStoreType(trustStoreType).setProtocol(protocol);

            if (!trustStore.startsWith(SAFKEYRING)) {
                if (trustStorePassword == null) {
                    throw new IllegalArgumentException("server.ssl.trustStorePassoword configuration parameter is not defined");
                }

                log.info("Loading trust store file: " + trustStore);
                FileSystemResource trustStoreResource = new FileSystemResource(trustStore);

                sslContextBuilder.loadTrustMaterial(trustStoreResource.getFile(), trustStorePassword.toCharArray());
            } else {
                log.info("Loading trust store key ring: " + trustStore);
                if (!trustStore.startsWith(SAFKEYRING + ":////")) {
                    throw new MalformedURLException("Incorrect key ring format: " + trustStore +
                        ". Make sure you use format safkeyring:////userId/keyRing");
                }
                URL keyRing = new URL(trustStore.replaceFirst("////", "//"));

                sslContextBuilder.loadTrustMaterial(keyRing, trustStorePassword.toCharArray());
            }
        } else {
            log.info("No trust store is defined");
        }
    }

    private void loadKeyMaterial(SSLContextBuilder sslContextBuilder) throws NoSuchAlgorithmException, KeyStoreException, CertificateException, IOException, UnrecoverableKeyException {
        if (keyStore != null) {
            sslContextBuilder.setKeyStoreType(keyStoreType).setProtocol(protocol);

            if (!keyStore.startsWith(SAFKEYRING)) {
                if (keyStore == null) {
                    throw new IllegalArgumentException("server.ssl.keyStore configuration parameter is not defined");
                }
                if (keyStorePassword == null) {
                    throw new IllegalArgumentException("server.ssl.keyStorePassword configuration parameter is not defined");
                }
                log.info("Loading key store file: " + keyStore);
                FileSystemResource keyStoreResource = new FileSystemResource(keyStore);
                sslContextBuilder.loadKeyMaterial(keyStoreResource.getFile(), keyStorePassword.toCharArray(), keyPassword.toCharArray());
            } else {
                log.info("Loading key store key ring: " + keyStore);
                if (!keyStore.startsWith(SAFKEYRING + ":////")) {
                    throw new MalformedURLException("Incorrect key ring format: " + keyStore +
                        ". Make sure you use format safkeyring:////userId/keyRing");
                }
                URL keyRing = new URL(keyStore.replaceFirst("////", "//"));

                sslContextBuilder.loadKeyMaterial(keyRing, keyStorePassword.toCharArray(), keyPassword.toCharArray(), null);
            }
        } else {
            log.info("No key store is defined");
        }
    }

    private SSLContext secureSslContext() {
        log.debug("Protocol: {}", protocol);
        SSLContextBuilder sslContextBuilder = SSLContexts.custom();
        try {
            loadTrustMaterial(sslContextBuilder);
            loadKeyMaterial(sslContextBuilder);
            return sslContextBuilder.build();
         } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
                | UnrecoverableKeyException | KeyManagementException e) {
            log.error("Error initializing HTTP client: {}", e.getMessage(), e);
            throw new RuntimeException("Error initializing HTTP client: " + e.getMessage(), e);
        }
    }

    @Bean
    public SSLContext apimlSslContext() {
        if (verifySslCertificatesOfServices) {
            return secureSslContext();
        }
        else {
            return ignoringSslContext();
        }
    }

    private ConnectionSocketFactory createSecureSslSocketFactory() {
        return new SSLConnectionSocketFactory(secureSslContext(), SSLConnectionSocketFactory.getDefaultHostnameVerifier());
    }
}
