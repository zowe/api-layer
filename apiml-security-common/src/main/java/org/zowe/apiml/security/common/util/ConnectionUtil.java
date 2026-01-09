/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.util;

import com.google.common.annotations.VisibleForTesting;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.product.web.HttpConfig;
import org.zowe.apiml.security.SecurityUtils;
import reactor.netty.http.client.HttpClient;
import reactor.netty.http.client.HttpClientSecurityUtils;
import reactor.netty.tcp.SslProvider;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;

import java.io.IOException;
import java.net.Socket;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

@UtilityClass
@Slf4j
public class ConnectionUtil {

    /**
     * @return io.netty.handler.ssl.SslContext for http client.
     */
    public SslContext getSslContext(HttpConfig config, boolean setKeystore) throws CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException, UnrecoverableKeyException {
        var builder = SslContextBuilder.forClient();

        var trustStore = SecurityUtils.loadKeyStore(config.getTrustStoreType(), config.getTrustStorePath(), config.getTrustStorePassword());
        var trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);
        builder.trustManager(trustManagerFactory);

        var keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());

        if (setKeystore) {
            log.debug("Loading keystore: {}: {}", config.getKeyStoreType(), config.getKeyStorePath());
            var keyStore = SecurityUtils.loadKeyStore(
                config.getKeyStoreType(), config.getKeyStorePath(), config.getKeyStorePassword());
            keyManagerFactory.init(keyStore, config.getKeyStorePassword());
            builder.keyManager(x509KeyManagerSelectedAlias(config, keyManagerFactory));
        } else {
            log.debug("ConnectionUtil.getSslContext - no keystore: using empty keystore");
            var emptyKeystore = KeyStore.getInstance(KeyStore.getDefaultType());
            emptyKeystore.load(null, null);
            keyManagerFactory.init(emptyKeystore, null);
            builder.keyManager(keyManagerFactory);
        }

        if (config.isVerifySslCertificatesOfServices() && config.isNonStrictVerifySslCertificatesOfServices()) {
            log.debug("ConnectionUtil.getSslContext - NONSTRICT mode: disabling endpointIdentificationAlgorithm");
            builder.endpointIdentificationAlgorithm(null);
        }

        return builder.build();
    }

    public HttpClient getHttpClient(HttpConfig config, HttpClient httpClient, boolean useClientCert) throws UnrecoverableKeyException, CertificateException, IOException, NoSuchAlgorithmException, KeyStoreException {
        var sslContextBuilder = SslProvider.builder().sslContext(ConnectionUtil.getSslContext(config, useClientCert));
        boolean hostnameVerificationEnabled = !config.isNonStrictVerifySslCertificatesOfServices();
        log.debug("ConnectionUtil.getHttpClient - SSL config: verifySslCertificatesOfServices={}, nonStrictVerifySslCertificatesOfServices={}, hostnameVerificationEnabled={}, useClientCert={}",
            config.isVerifySslCertificatesOfServices(),
            config.isNonStrictVerifySslCertificatesOfServices(),
            hostnameVerificationEnabled,
            useClientCert);
        if (hostnameVerificationEnabled) {
            log.debug("ConnectionUtil.getHttpClient - hostname verification enabled");
            sslContextBuilder.handlerConfigurator(HttpClientSecurityUtils.HOSTNAME_VERIFICATION_CONFIGURER);
        } else {
            log.debug("ConnectionUtil.getHttpClient - hostname verification disabled");
            sslContextBuilder.handlerConfigurator(handler -> {
                var sslEngine = handler.engine();
                var sslParameters = sslEngine.getSSLParameters();
                sslParameters.setEndpointIdentificationAlgorithm(null);
                sslEngine.setSSLParameters(sslParameters);
            });
        }
        return httpClient.secure(sslContextBuilder.build());
    }

    @VisibleForTesting
    public X509KeyManager x509KeyManagerSelectedAlias(HttpConfig config, KeyManagerFactory keyManagerFactory) {
        return new ConnectionUtil.X509KeyManagerSelectedAlias(keyManagerFactory, config.getKeyAlias());
    }

    public static class X509KeyManagerSelectedAlias implements X509KeyManager {

        private final X509KeyManager originalKm;
        private final String keyAlias;

        public X509KeyManagerSelectedAlias(KeyManagerFactory keyManagerFactory, String keyAlias) {
            this.originalKm = (X509KeyManager) keyManagerFactory.getKeyManagers()[0];
            this.keyAlias = keyAlias;
        }

        @Override
        public String[] getClientAliases(String keyType, Principal[] issuers) {
            return originalKm.getClientAliases(keyType, issuers);
        }

        @Override
        public String chooseClientAlias(String[] keyType, Principal[] issuers, Socket socket) {
            if (keyAlias != null) {
                return keyAlias;
            }
            return originalKm.chooseClientAlias(keyType, issuers, socket);
        }

        @Override
        public String[] getServerAliases(String keyType, Principal[] issuers) {
            return originalKm.getServerAliases(keyType, issuers);
        }

        @Override
        public String chooseServerAlias(String keyType, Principal[] issuers, Socket socket) {
            if (keyAlias != null) {
                return keyAlias;
            }
            return originalKm.chooseServerAlias(keyType, issuers, socket);
        }

        @Override
        public X509Certificate[] getCertificateChain(String alias) {
            return originalKm.getCertificateChain(alias);
        }

        @Override
        public PrivateKey getPrivateKey(String alias) {
            return originalKm.getPrivateKey(alias);
        }

    }

}
