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

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.server.AbstractConfigurableWebServerFactory;
import org.springframework.boot.web.server.Ssl;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Customizer for Tomcat SSL configuration that fixes SAF keyring URLs.
 * <p>
 * If a keyStore or trustStore uses a SAF keyring (e.g. {@code safkeyring:///USER/KEYRING}),
 * this class reformats the URL and sets default passwords if none are provided.
 * </p>
 */
@Slf4j
@Component
public class TomcatKeyringFix implements WebServerFactoryCustomizer<AbstractConfigurableWebServerFactory> {

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

    boolean isKeyring(String input) {
        if (input == null) return false;
        Matcher matcher = KEYRING_PATTERN.matcher(input);
        return matcher.matches();
    }

    /**
     * Normalizes the given keyring URL into a Tomcat-compatible format.
     * <p>
     * For example, converts:
     * <pre>
     * safkeyring:///USER/KEYRING → safkeyring://USER/KEYRING
     * </pre>
     *
     * @param keyringUrl the original keyring URL
     * @return the normalized URL or {@code null} if the input was {@code null}
     */
    static String formatKeyringUrl(String keyringUrl) {
        if (keyringUrl == null) return null;
        Matcher matcher = KEYRING_PATTERN.matcher(keyringUrl);
        if (matcher.matches()) {
            keyringUrl = matcher.group(1) + "://" + matcher.group(2) + "/" + matcher.group(3);
        }
        return keyringUrl;
    }

    /**
     * Customizes the Tomcat {@link Ssl} configuration before the web server starts.
     * <p>
     * If keyring-based stores are detected, this method updates their configuration
     * (URL format, alias, and password) to ensure that Tomcat can correctly access
     * the keyring at runtime.
     * </p>
     *
     * @param factory the Tomcat web server factory being customized
     */
    @Override
    public void customize(AbstractConfigurableWebServerFactory factory) {
        Ssl ssl = factory.getSsl();
        if (isKeyring(keyStore)) {
            ssl.setKeyStore(formatKeyringUrl(keyStore));
            ssl.setKeyAlias(keyAlias);
            ssl.setKeyStorePassword(keyStorePassword == null ? KEYRING_PASSWORD : String.valueOf(keyStorePassword));
            ssl.setKeyPassword(keyPassword == null ? KEYRING_PASSWORD : String.valueOf(keyPassword));
        }

        if (isKeyring(trustStore)) {
            ssl.setTrustStore(formatKeyringUrl(trustStore));
            ssl.setTrustStorePassword(trustStorePassword == null ? KEYRING_PASSWORD : String.valueOf(trustStorePassword));
        }

        log.debug("TomcatKeyringFix applied");
    }

}
