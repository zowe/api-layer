/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package com.ca.mfaas.gateway.config.security;

import com.ca.mfaas.product.config.MFaaSConfigPropertiesContainer;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Objects;

@SuppressWarnings("Duplicates")
@Slf4j
@Configuration
public class SecureHttpConfig extends WebSecurityConfigurerAdapter {

    private static final String SAFKEYRING = "safkeyring";

    private final MFaaSConfigPropertiesContainer propertiesContainer;

    @Autowired
    public SecureHttpConfig(MFaaSConfigPropertiesContainer propertiesContainer) {
        this.propertiesContainer = propertiesContainer;
    }

    @Override
    protected void configure(HttpSecurity http) throws Exception {
        http
            .cors().disable()
            .csrf().disable()
            .exceptionHandling()
            .and()
            .sessionManagement()
            .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            .and()
            .authorizeRequests()
            .antMatchers("/**").permitAll()
            .and()
            .httpBasic().disable()
            .headers().disable();
    }

    @Bean
    RestTemplate restTemplate() {
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
        if (propertiesContainer.getGateway().getVerifySslCertificatesOfServices()) {
            return createSecureSslSocketFactory();
        } else {
            log.warn("The gateway is not verifying the TLS/SSL certificates of the services");
            return createIgnoringSslSocketFactory();
        }
    }

    private ConnectionSocketFactory createIgnoringSslSocketFactory() {
        try {
            SSLContext sslContext = new SSLContextBuilder().loadTrustMaterial(null, (certificate, authType) -> true).build();
            return new SSLConnectionSocketFactory(sslContext, new NoopHostnameVerifier());
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new BeanInitializationException("Error initializing secureHttpClient: " + e.getMessage(), e);
        }
    }

    private ConnectionSocketFactory createSecureSslSocketFactory() {
        String trustStore = propertiesContainer.getSecurity().getTrustStore();
        String trustStorePassword = propertiesContainer.getSecurity().getTrustStorePassword();

        SSLContextBuilder sslContextBuilder = SSLContexts
            .custom()
            .setKeyStoreType(propertiesContainer.getSecurity().getTrustStoreType())
            .setProtocol(propertiesContainer.getSecurity().getProtocol());

        try {
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

            return new SSLConnectionSocketFactory(sslContextBuilder.build(), SSLConnectionSocketFactory.getDefaultHostnameVerifier());
        } catch (IOException | CertificateException | KeyStoreException | KeyManagementException
            | NoSuchAlgorithmException e) {
            throw new BeanInitializationException("Error initializing secureHttpClient: " + e.getMessage(), e);
        }
    }
}
