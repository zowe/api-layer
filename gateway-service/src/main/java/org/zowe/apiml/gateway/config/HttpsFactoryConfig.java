/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.config;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.HttpsFactory;
import org.zowe.apiml.security.SecurityUtils;

@Configuration
@Slf4j
@RequiredArgsConstructor
@Getter
public class HttpsFactoryConfig {

    private static final char[] KEYRING_PASSWORD = "password".toCharArray();

    @Value("${server.ssl.protocol:TLSv1.2}")
    private String protocol;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStorePath;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    private char[] trustStorePassword;

    @Value("${server.ssl.trustStoreType:PKCS12}")
    private String trustStoreType;

    @Value("${server.ssl.keyAlias:#{null}}")
    private String keyAlias;

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStorePath;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    private char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    private char[] keyPassword;

    @Value("${server.ssl.keyStoreType:PKCS12}")
    private String keyStoreType;

    @Value("${apiml.security.ssl.verifySslCertificatesOfServices:true}")
    private boolean verifySslCertificatesOfServices;

    @Value("${apiml.security.ssl.nonStrictVerifySslCertificatesOfServices:false}")
    private boolean nonStrictVerifySslCertificatesOfServices;

    @Value("${server.ssl.trustStoreRequired:false}")
    private boolean trustStoreRequired;

    private final ApplicationContext context;

    @PostConstruct
    public void updateConfigParameters() {
        ServerProperties serverProperties = context.getBean(ServerProperties.class);
        if (SecurityUtils.isKeyring(keyStorePath)) {
            keyStorePath = SecurityUtils.formatKeyringUrl(keyStorePath);
            serverProperties.getSsl().setKeyStore(keyStorePath);
            if (keyStorePassword == null) keyStorePassword = KEYRING_PASSWORD;
        }
        if (SecurityUtils.isKeyring(trustStorePath)) {
            trustStorePath = SecurityUtils.formatKeyringUrl(trustStorePath);
            serverProperties.getSsl().setTrustStore(trustStorePath);
            if (trustStorePassword == null) trustStorePassword = KEYRING_PASSWORD;
        }
    }

    @Bean
    public HttpsConfig httpsConfig() {
        HttpsConfig config = HttpsConfig.builder()
            .protocol(protocol)
            .verifySslCertificatesOfServices(verifySslCertificatesOfServices)
            .nonStrictVerifySslCertificatesOfServices(nonStrictVerifySslCertificatesOfServices)
            .trustStorePassword(trustStorePassword).trustStoreRequired(trustStoreRequired)
            .trustStore(trustStorePath).trustStoreType(trustStoreType)
            .keyAlias(keyAlias).keyStore(keyStorePath).keyPassword(keyPassword)
            .keyStorePassword(keyStorePassword).keyStoreType(keyStoreType).build();
        log.info("Using HTTPS configuration: {}", config.toString());
        return config;
    }

    @Bean
    public HttpsFactory httpsFactory() {
        return new HttpsFactory(httpsConfig());
    }

}
