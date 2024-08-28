package org.zowe.apiml.metrics.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.zowe.apiml.security.SecurityUtils;

import javax.annotation.PostConstruct;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

@Configuration
public class SslConfig {

    private static final char[] KEYRING_PASSWORD = "password".toCharArray();

    @Value("${server.ssl.trustStoreType:#{null}}")
    protected String trustStoreType;

    @Value("${server.ssl.trustStore:#{null}}")
    protected String trustStore;

    @Value("${server.ssl.trustStorePassword:#{null}}")
    protected char[] trustStorePassword;

    @Value("${server.ssl.keyStoreType:#{null}}")
    protected String keyStoreType;

    @Value("${server.ssl.keyStore:#{null}}")
    protected String keyStore;

    @Value("${server.ssl.keyStorePassword:#{null}}")
    protected char[] keyStorePassword;

    private void setSystemProperty(String key, String value) {
        if (value == null) {
            System.clearProperty(key);
        } else {
            System.setProperty(key, value);
        }
    }

    private void initStore(String javaxPrefix, String name, String type, char[] password) {
        char[] storePassword = password;
        if (isEmpty(storePassword) && SecurityUtils.isKeyring(name)) {
            storePassword = KEYRING_PASSWORD;
        }
        setSystemProperty(javaxPrefix + "Store", SecurityUtils.formatKeyringUrl(name));
        setSystemProperty(javaxPrefix + "StorePassword", storePassword == null ? null : String.valueOf(storePassword));
        setSystemProperty(javaxPrefix + "StoreType", type);
    }

    @PostConstruct
    void init() {
        initStore("javax.net.ssl.trust", trustStore, trustStoreType, trustStorePassword);
        initStore("javax.net.ssl.key", keyStore, keyStoreType, keyStorePassword);
    }

}
