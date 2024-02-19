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

import org.apache.catalina.connector.Connector;
import org.apache.tomcat.util.net.SSLHostConfig;
import org.apache.tomcat.util.net.SSLHostConfigCertificate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TomcatKeyringFix implements TomcatConnectorCustomizer {

    private static final Pattern KEYRING_PATTERN = Pattern.compile("^(safkeyring[^:]*):/{2,4}([^/]+)/(.+)$");
    private static final String KEYRING_PASSWORD = "password";

    @Value("${server.ssl.keyStore:#{null}}")
    protected String keyStore;
    @Value("${server.ssl.keyAlias:localhost}")
    protected String keyAlias;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    protected char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    protected char[] keyPassword;

    @Value("${server.ssl.trustStore:#{null}}")
    protected String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    protected char[] trustStorePassword;

    void fixDefaultCertificate(SSLHostConfig sslHostConfig) {
        Set<SSLHostConfigCertificate> originalSet = sslHostConfig.getCertificates();
        if (originalSet.isEmpty()) return;

        try {
            Field defaultCertificateField = sslHostConfig.getClass().getDeclaredField("defaultCertificate");
            defaultCertificateField.setAccessible(true); // NOSONAR
            if (defaultCertificateField.get(sslHostConfig) == null) {
                defaultCertificateField.set(sslHostConfig, originalSet.iterator().next()); // NOSONAR
            }
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException("Cannot update Tomcat SSL context", e);
        }
    }

    boolean isKeyring(String input) {
        if (input == null) return false;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        return matcher.matches();
    }

    static String formatKeyringUrl(String keyringUrl) {
        if (keyringUrl == null) return null;
        Matcher matcher = KEYRING_PATTERN.matcher(keyringUrl);
        if (matcher.matches()) {
            keyringUrl = matcher.group(1) + "://" + matcher.group(2) + "/" + matcher.group(3);
        }
        return keyringUrl;
    }

    @Override
    public void customize(Connector connector) {
        Arrays.stream(connector.findSslHostConfigs()).forEach(sslConfig -> {
            fixDefaultCertificate(sslConfig);

            if (isKeyring(keyStore)) {
                sslConfig.getCertificates().forEach(certificate -> {
                    certificate.setCertificateKeystoreFile(formatKeyringUrl(keyStore));
                    certificate.setCertificateKeyAlias(keyAlias);
                    certificate.setCertificateKeystorePassword(keyStorePassword == null ? KEYRING_PASSWORD : keyStorePassword);
                    certificate.setCertificateKeyPassword(keyPassword == null ? KEYRING_PASSWORD : keyPassword);
                });
            }

            if (isKeyring(trustStore)) {
                sslConfig.setTruststoreFile(formatKeyringUrl(trustStore));
                sslConfig.setTruststorePassword(trustStorePassword == null ? KEYRING_PASSWORD : trustStorePassword);
            }
        });
    }

}
