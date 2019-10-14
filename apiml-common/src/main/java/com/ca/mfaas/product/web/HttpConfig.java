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

import com.ca.mfaas.message.log.ApimlLogger;
import com.ca.mfaas.product.logging.annotations.InjectApimlLogger;
import com.ca.mfaas.security.HttpsConfig;
import com.ca.mfaas.security.HttpsConfigError;
import com.ca.mfaas.security.HttpsFactory;
import com.ca.mfaas.security.SecurityUtils;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClient;
import com.netflix.discovery.shared.transport.jersey.EurekaJerseyClientImpl.EurekaJerseyClientBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.impl.client.CloseableHttpClient;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;

@Slf4j
@Configuration
public class HttpConfig {
    @Value("${server.ssl.protocol:TLSv1.2}")
    private String protocol;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private String trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private String keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private String keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${spring.application.name}")
    private String serviceId;

    @Value("${server.ssl.trustStoreRequired:false}")
    private boolean trustStoreRequired;

    @Value("${eureka.client.serviceUrl.defaultZone}")
    private String eurekaServerUrl;

    private CloseableHttpClient secureHttpClient;
    private SSLContext secureSslContext;
    private HostnameVerifier secureHostnameVerifier;
    private EurekaJerseyClientBuilder eurekaJerseyClientBuilder;

    @InjectApimlLogger
    private ApimlLogger apimlLog = ApimlLogger.empty();

    @PostConstruct
    public void init() {
        try {
            HttpsConfig httpsConfig = HttpsConfig.builder().protocol(protocol).keyAlias(keyAlias).keyStore(keyStore).keyPassword(keyPassword)
                    .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).trustStore(trustStore)
                    .trustStoreType(trustStoreType).trustStorePassword(trustStorePassword).trustStoreRequired(trustStoreRequired)
                    .verifySslCertificatesOfServices(verifySslCertificatesOfServices).build();

            log.info("Using HTTPS configuration: {}", httpsConfig.toString());

            HttpsFactory factory = new HttpsFactory(httpsConfig);
            secureHttpClient = factory.createSecureHttpClient();
            secureSslContext = factory.createSslContext();
            secureHostnameVerifier = factory.createHostnameVerifier();
            eurekaJerseyClientBuilder = factory.createEurekaJerseyClientBuilder(eurekaServerUrl, serviceId);

            factory.setSystemSslProperties();
        }
        catch (HttpsConfigError e) {
            System.exit(1); // NOSONAR
        }
        catch (Exception e) {
            apimlLog.log("apiml.common.unknownHttpsConfigError", e.getMessage());
            System.exit(1); // NOSONAR
        }
    }

    @Bean
    public SslContextFactory jettySslContextFactory() {
        SslContextFactory sslContextFactory = new SslContextFactory(SecurityUtils.replaceFourSlashes(keyStore));
        sslContextFactory.setProtocol(protocol);
        sslContextFactory.setKeyStorePassword(keyStorePassword);
        sslContextFactory.setKeyStoreType(keyStoreType);
        sslContextFactory.setCertAlias(keyAlias);

        if (trustStore != null) {
            sslContextFactory.setTrustStorePath(SecurityUtils.replaceFourSlashes(trustStore));
            sslContextFactory.setTrustStoreType(trustStoreType);
            sslContextFactory.setTrustStorePassword(trustStorePassword);
        }
        log.debug("jettySslContextFactory: {}", sslContextFactory.dump());

        if (!verifySslCertificatesOfServices) {
            sslContextFactory.setTrustAll(true);
        }

        return sslContextFactory;
    }

    @Bean
    public RestTemplate restTemplate() {
        HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory(secureHttpClient);
        return new RestTemplate(factory);
    }

    @Bean
    public CloseableHttpClient secureHttpClient() {
        return secureHttpClient;
    }

    @Bean
    public SSLContext secureSslContext() {
        return secureSslContext;
    }

    @Bean
    public HostnameVerifier secureHostnameVerifier() {
        return secureHostnameVerifier;
    }

    @Bean
    public EurekaJerseyClient eurekaJerseyClient() {
        return eurekaJerseyClientBuilder.build();
    }
}
