/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.service.internal;

import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


public class CloseableClientProviderTest {
    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";
    private ConfigProperties configProperties;
    private HttpsClientProvider httpsClientProvider;

    @BeforeEach
    public void setup() throws Exception {
        this.configProperties = getConfigProperties();
        this.httpsClientProvider = new HttpsClientProvider(configProperties);
    }

    private ConfigProperties getConfigProperties() throws IOException {
        String absoluteFilePath = new File(CONFIG_FILE_PATH).getAbsolutePath();
        ConfigProperties properties = new ConfigProperties();
        Properties configProp = new Properties();
        try {
            if (Paths.get(absoluteFilePath).toFile().exists()) {
                configProp.load(new FileReader(absoluteFilePath));

                properties.setApimlHost(configProp.getProperty("APIML_HOST"));
                properties.setApimlPort(configProp.getProperty("APIML_PORT"));
                properties.setApimlBaseUrl(configProp.getProperty("APIML_BASE_URL"));
                properties.setKeyStorePath(configProp.getProperty("KEYSTOREPATH"));
                properties.setKeyStorePassword(configProp.getProperty("KEYSTOREPASSWORD"));
                properties.setKeyStoreType(configProp.getProperty("KEYSTORETYPE"));
                properties.setTrustStorePath(configProp.getProperty("TRUSTSTOREPATH"));
                properties.setTrustStorePassword(configProp.getProperty("TRUSTSTOREPASSWORD"));
                properties.setTrustStoreType(configProp.getProperty("TRUSTSTORETYPE"));
            }
        } catch (IOException e) {
            throw new IOException();
        }
        return properties;
    }

    //tests
    @Test
    void testGetHttpClientWithTrustStore() throws ZaasConfigurationException {
        assertNotNull(httpsClientProvider.getHttpsClientWithTrustStore());
    }

    @Test
    void testGetHttpClientWithTrustStoreWithCookies() throws ZaasConfigurationException {
        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("apimlAuthenticationToken", "token");
        cookie.setDomain(configProperties.getApimlHost());
        cookie.setPath("/");
        cookieStore.addCookie(cookie);

        assertNotNull(httpsClientProvider.getHttpsClientWithTrustStore(cookieStore));
    }

    @Test
    void testGetHttpsClientWithKeyStoreAndTrustStore() throws ZaasConfigurationException {
        assertNotNull(httpsClientProvider.getHttpsClientWithKeyStoreAndTrustStore());
    }

    @Test
    void givenNullTrustStore_whenTheClientIsConstructed_thenExceptionsIsThrown() {
        ZaasConfigurationException zaasException =
            assertThrows(ZaasConfigurationException.class, () -> new HttpsClientProvider(new ConfigProperties()));

        ZaasConfigurationErrorCodes errorCode = zaasException.getErrorCode();
        assertThat(errorCode.getId(), is("ZWEAS500E"));
        assertThat(errorCode.getMessage(), is("There was no path to the trust store."));
    }

    @Test
    void giveInvalidTrustStorePath_whenTheClientIsConstructed_thenExceptionsIsThrown() {
        ZaasConfigurationException zaasException = assertThrows(ZaasConfigurationException.class, () -> {
            ConfigProperties config = new ConfigProperties();
            config.setTrustStorePath("intentionallyInvalidPath");
            config.setTrustStoreType("PKCS12");
            new HttpsClientProvider(config);
        });

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS503E"));
    }

    @Test
    void givenNullKeyStorePath_whenTheClientIsConstructed_thenExceptionIsThrown() throws ZaasConfigurationException {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword("password");
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("PKCS12");
        HttpsClientProvider provider = new HttpsClientProvider(config);
        ZaasConfigurationException zaasException = assertThrows(ZaasConfigurationException.class, () -> {
            provider.getHttpsClientWithKeyStoreAndTrustStore();
        });

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS501E"));
    }

    @Test
    void givenInvalidKeyStorePath_whenTheClientIsConstructed_thenExceptionIsThrown() throws ZaasConfigurationException {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword("password");
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("PKCS12");
        config.setKeyStorePath("intentionallyInvalidPath");
        config.setKeyStoreType("PKCS12");
        HttpsClientProvider provider = new HttpsClientProvider(config);
        ZaasConfigurationException zaasException = assertThrows(ZaasConfigurationException.class, () -> {
            provider.getHttpsClientWithKeyStoreAndTrustStore();
        });

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS503E"));
    }

    @Test
    void givenInvalidKeyStoreType_whenTheClientIsConstructed_thenExceptionIsThrown() throws ZaasConfigurationException {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword("password");
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("PKCS12");
        config.setKeyStorePath("src/test/resources/localhost.keystore.p12");
        config.setKeyStoreType("invalidCryptoType");
        HttpsClientProvider provider = new HttpsClientProvider(config);
        ZaasConfigurationException zaasException = assertThrows(ZaasConfigurationException.class, () -> {
            provider.getHttpsClientWithKeyStoreAndTrustStore();
        });

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS502E"));
    }

    @Test
    void givenInvalidTrustStoreType_whenTheClientIsConstructed_thenExceptionIsThrown() {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword("password");
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("invalidCryptoType");
        ZaasConfigurationException zaasException = assertThrows(
            ZaasConfigurationException.class, () -> new HttpsClientProvider(config)
        );
        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS502E"));
    }

    @Test
    void testReplaceFourSlashes() {
        String newUrl = HttpsClientProvider.replaceFourSlashes("safkeyring:////userId/keyRing");
        assertEquals("safkeyring://userId/keyRing", newUrl);
    }
}
