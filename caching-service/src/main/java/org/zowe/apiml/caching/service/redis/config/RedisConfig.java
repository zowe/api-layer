/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.caching.service.redis.config;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.caching.config.GeneralConfig;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.List;

@Data
@Configuration
@ConfigurationProperties(value = "caching.storage.redis")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "caching.storage.mode", havingValue = "redis")
public class RedisConfig {
    private static final int DEFAULT_PORT = 6379;
    private static final String DEFAULT_USER = "default";
    private static final char[] DEFAULT_PASSWORD = "".toCharArray();

    private static final String AUTHENTICATION_SEPARATOR = "@";
    private static final String PORT_SEPARATOR = ":";
    private static final char CREDENTIALS_SEPARATOR = ':';

    private Integer timeout = 60;
    private String masterNodeUri;

    private final GeneralConfig generalConfig;
    private String host;
    private Integer port = DEFAULT_PORT;
    private String username = DEFAULT_USER;
    private char[] password = DEFAULT_PASSWORD;
    private Sentinel sentinel;
    private SslConfig ssl;

    @PostConstruct
    public void init() {
        NodeUriCredentials credentials = parseCredentialsFromUri(masterNodeUri);

        username = credentials.getUsername();
        password = credentials.getPassword();
        port = parsePortFromUri(masterNodeUri);
        host = parseHostFromUri(masterNodeUri);
    }

    public boolean usesSentinel() {
        return sentinel != null && sentinel.isEnabled();
    }

    public boolean usesSsl() {
        return ssl != null && ssl.getEnabled();
    }

    @Data
    public static class Sentinel {
        private boolean enabled = false;
        private String masterInstance;
        private List<SentinelNode> nodes;

        @Data
        public static class SentinelNode {
            private String host;
            private Integer port;
            private char[] password;

            public SentinelNode(String nodeUri) {
                NodeUriCredentials credentials = parseCredentialsFromUri(nodeUri);
                password = credentials.getPassword();

                host = parseHostFromUri(nodeUri);
                port = parsePortFromUri(nodeUri);
            }
        }
    }

    @Data
    public static class SslConfig {
        private Boolean enabled = true;
        private String keyStore;
        private String keyStorePassword;
        private String trustStore;
        private String trustStorePassword;
    }

    private static boolean uriContainsCredentials(String nodeUri) {
        return nodeUri.contains(AUTHENTICATION_SEPARATOR);
    }

    private static boolean uriContainsPort(String nodeUri) {
        if (uriContainsCredentials(nodeUri)) {
            return nodeUri.substring(nodeUri.indexOf(AUTHENTICATION_SEPARATOR) + 1).contains(PORT_SEPARATOR);
        } else {
            return nodeUri.contains(PORT_SEPARATOR);
        }
    }

    private static NodeUriCredentials parseCredentialsFromUri(String nodeUri) {
        if (!uriContainsCredentials(nodeUri)) {
            return new NodeUriCredentials(DEFAULT_USER, DEFAULT_PASSWORD);
        }

        String credentials = nodeUri.substring(0, nodeUri.indexOf(AUTHENTICATION_SEPARATOR));

        char[] credentialsChars = null;
        try {
            credentialsChars = credentials.toCharArray();
            int index = ArrayUtils.indexOf(credentialsChars, CREDENTIALS_SEPARATOR);
            if (index >= 0) {
                return new NodeUriCredentials(credentials.substring(0, index), Arrays.copyOfRange(credentialsChars, index + 1, credentialsChars.length));
            } else {
                NodeUriCredentials output = new NodeUriCredentials(DEFAULT_USER, credentialsChars);
                credentialsChars = null; // do not clean up it
                return output;
            }
        } finally {
            if (credentialsChars != null) {
                Arrays.fill(credentialsChars, (char) 0);
            }
        }
    }

    private static String parseHostFromUri(String nodeUri) {
        if (uriContainsCredentials(nodeUri)) {
            if (uriContainsPort(nodeUri)) {
                String hostAndPort = nodeUri.substring(nodeUri.indexOf(AUTHENTICATION_SEPARATOR) + 1);
                return hostAndPort.substring(0, hostAndPort.indexOf(PORT_SEPARATOR));
            } else {
                return nodeUri.substring(nodeUri.indexOf(AUTHENTICATION_SEPARATOR) + 1);
            }
        } else if (uriContainsPort(nodeUri)) {
            return nodeUri.substring(0, nodeUri.indexOf(PORT_SEPARATOR));
        } else {
            return nodeUri;
        }
    }

    private static int parsePortFromUri(String nodeUri) {
        if (!uriContainsPort(nodeUri)) {
            return DEFAULT_PORT;
        }

        if (uriContainsCredentials(nodeUri)) {
            String hostAndPort = nodeUri.substring(nodeUri.indexOf(AUTHENTICATION_SEPARATOR) + 1);
            return Integer.parseInt(hostAndPort.substring(hostAndPort.indexOf(PORT_SEPARATOR) + 1));
        } else {
            return Integer.parseInt(nodeUri.substring(nodeUri.indexOf(PORT_SEPARATOR) + 1));
        }
    }

    @Data
    @AllArgsConstructor
    private static class NodeUriCredentials {
        private String username;
        private char[] password;
    }
}
