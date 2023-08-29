/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.cloudgatewayservice.service;

import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;
import org.zowe.apiml.security.HttpsConfigError;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import java.io.File;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;

/**
 * Utility class for the custom Netty {@link SslContext} creation.
 * Does not support keyring because client keystore override mainly used in development mode and not supposed be run on the Mainframe.
 */
@Slf4j
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WebClientHelper {
    private static final ApimlLogger apimlLog = ApimlLogger.of(WebClientHelper.class, YamlMessageServiceInstance.getInstance());

    /**
     * Load {@link SslContext} from the specified keystore
     *
     * @param keystorePath path to the keystore file
     * @param password     keystore password
     * @throws IllegalArgumentException if keystore file does not exist.
     * @throws HttpsConfigError         if any error occur during the context creation.
     */
    public static SslContext load(String keystorePath, char[] password) {
        File keyStoreFile = new File(keystorePath);
        if (keyStoreFile.exists()) {
            try (InputStream is = Files.newInputStream(Paths.get(keystorePath))) {
                KeyStore keyStore = KeyStore.getInstance("PKCS12");
                keyStore.load(is, password);
                return initSslContext(keyStore, password);
            } catch (Exception e) {
                apimlLog.log("org.zowe.apiml.common.sslContextInitializationError", e.getMessage());
                throw new HttpsConfigError("Error initializing SSL Context: " + e.getMessage(), e,
                        HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED);
            }
        } else {
            throw new IllegalArgumentException("Not existing file: " + keystorePath);
        }
    }

    private static SslContext initSslContext(KeyStore keyStore, char[] password) throws NoSuchAlgorithmException, UnrecoverableKeyException, KeyStoreException, SSLException {
        KeyManagerFactory kmf = KeyManagerFactory.getInstance("PKIX");
        kmf.init(keyStore, password);

        return SslContextBuilder.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .keyManager(kmf).build();
    }


}
