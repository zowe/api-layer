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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class TomcatKeyringFix implements TomcatConnectorCustomizer {

    private static final Pattern KEYRING_PATTERN = Pattern.compile("^(safkeyring[^:]*):/{2,4}([^/]+)[/](.+)$");
    private static final String KEYRING_PASSWORD = "password";

    @Value("${server.ssl.keyStore:#{null}}")
    private String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    protected char[] keyStorePassword;

    @Value("${server.ssl.keyPassword:#{null}}")
    protected char[] keyPassword;

    @Value("${server.ssl.trustStore:#{null}}")
    private String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    protected char[] trustStorePassword;

    boolean isKeyring(String input) {
        if (input == null) return false;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        return matcher.matches();
    }

    public static String formatKeyringUrl(String input) {
        if (input == null) return null;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        if (matcher.matches()) {
            return matcher.group(1) + "://" + matcher.group(2) + "/" + matcher.group(3);
        }
        return input;
    }

    @Override
    public void customize(Connector connector) {
        Arrays.stream(connector.findSslHostConfigs()).forEach(sslConfig -> {
            if (isKeyring(keyStore)) {
                sslConfig.setCertificateKeystoreFile(formatKeyringUrl(keyStore));
                if (keyStorePassword == null) sslConfig.setCertificateKeystorePassword(KEYRING_PASSWORD);
                if (keyPassword == null) sslConfig.setCertificateKeyPassword(KEYRING_PASSWORD);
            }

            if (isKeyring(trustStore)) {
                sslConfig.setTruststoreFile(formatKeyringUrl(trustStore));
                if (trustStorePassword == null) sslConfig.setTruststorePassword(KEYRING_PASSWORD);
            }
        });
    }

}
