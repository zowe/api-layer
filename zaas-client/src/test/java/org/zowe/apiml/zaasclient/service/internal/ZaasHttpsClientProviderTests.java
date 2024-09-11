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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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
import static org.junit.jupiter.api.Assertions.*;


class ZaasHttpsClientProviderTests {

    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";
    private static final char[] PASSWORD = "password".toCharArray(); // NOSONAR

    private ZaasHttpsClientProvider zaasHttpsClientProvider;

    @BeforeEach
    void setup() throws Exception {
        this.zaasHttpsClientProvider = new ZaasHttpsClientProvider(getConfigProperties());
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
                String keyStorePassword = configProp.getProperty("KEYSTOREPASSWORD");
                properties.setKeyStorePassword(keyStorePassword == null ? null : keyStorePassword.toCharArray());
                properties.setKeyStoreType(configProp.getProperty("KEYSTORETYPE"));
                properties.setTrustStorePath(configProp.getProperty("TRUSTSTOREPATH"));
                String trustStorePassword = configProp.getProperty("TRUSTSTOREPASSWORD");
                properties.setTrustStorePassword(trustStorePassword == null ? null : trustStorePassword.toCharArray());
                properties.setTrustStoreType(configProp.getProperty("TRUSTSTORETYPE"));
            }
        } catch (IOException e) {
            throw new IOException();
        }
        return properties;
    }

    @Test
    void testGetHttpsClientWithKeyStoreAndTrustStore() throws ZaasConfigurationException {
        assertNotNull(zaasHttpsClientProvider.getHttpClient());
    }

    @Test
    void giveNoKeyStorePath_whenTheClientIsConstructed_thenEmptyKeyStoreIsUsed() throws ZaasConfigurationException, IOException {
        ConfigProperties config = getConfigProperties();
        config.setKeyStorePath(null);
        ZaasHttpsClientProvider provider = new ZaasHttpsClientProvider(config);

        assertNotNull(provider.getHttpClient());
    }

    @Test
    void whenGetHttpsClientWithKeyStoreAndTrustStore_thenIdenticalClientReturned() throws ZaasConfigurationException {
        var client1 = zaasHttpsClientProvider.getHttpClient();
        var client2 = zaasHttpsClientProvider.getHttpClient();

        assertEquals(client1, client2);
    }

    @Test
    void givenNullTrustStore_whenTheClientIsConstructed_thenExceptionsIsThrown() {
        ZaasConfigurationException zaasException =
            assertThrows(ZaasConfigurationException.class, () -> new ZaasHttpsClientProvider(new ConfigProperties()));

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
            new ZaasHttpsClientProvider(config);
        });

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS503E"));
    }

    @Test
    void givenInvalidKeyStorePath_whenTheClientIsConstructed_thenExceptionIsThrown() throws ZaasConfigurationException {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword(PASSWORD);
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("PKCS12");
        config.setKeyStorePath("intentionallyInvalidPath");
        config.setKeyStoreType("PKCS12");
        ZaasHttpsClientProvider provider = new ZaasHttpsClientProvider(config);
        ZaasConfigurationException zaasException = assertThrows(ZaasConfigurationException.class, provider::getHttpClient);

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS503E"));
    }

    @Test
    void givenInvalidKeyStoreType_whenTheClientIsConstructed_thenExceptionIsThrown() throws ZaasConfigurationException {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword(PASSWORD);
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("PKCS12");
        config.setKeyStorePath("src/test/resources/localhost.keystore.p12");
        config.setKeyStoreType("invalidCryptoType");
        ZaasHttpsClientProvider provider = new ZaasHttpsClientProvider(config);
        ZaasConfigurationException zaasException = assertThrows(ZaasConfigurationException.class, provider::getHttpClient);

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS502E"));
    }

    @Test
    void givenInvalidTrustStoreType_whenTheClientIsConstructed_thenExceptionIsThrown() {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword(PASSWORD);
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("invalidCryptoType");
        ZaasConfigurationException zaasException = assertThrows(
            ZaasConfigurationException.class, () -> new ZaasHttpsClientProvider(config)
        );
        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS502E"));
    }

    @Nested
    class WhenKeyringUrl {

        @CsvSource({
            ",false",
            "safkeyring://userid/ringid,true",
            "safkeyring:////userid/ring/id,false",
            "safkeyring:////userid//ringid,false",
            "safkeyring://///userid//ringid,false",
            "safkeyring:////id,false",
            "safkeyringjce:////userid/ringid,true",
            "keystore.p12,false"
        })
        @ParameterizedTest(name = "isKeyring({0}) should return {1}")
        void isKeyringReturnsTrueIfItIsValid(String url, boolean validity) {
            assertEquals(validity, ZaasHttpsClientProvider.isKeyring(url));
        }

        @CsvSource({
            ",",
            "safkeyring://userid/ringid,safkeyring://userid/ringid",
            "safkeyring:////userid/ring/id,safkeyring:////userid/ring/id",
            "safkeyring:////userid//ringid,safkeyring:////userid//ringid",
            "safkeyring:////id,safkeyring:////id",
            "safkeyringjce:////userid/ringid,safkeyringjce://userid/ringid",
            "keystore.p12,keystore.p12"
        })
        @ParameterizedTest(name = "formatKeyringUrl({0}) should return {1}")
        void formatKeyringUrlFixTheFormatIfPossible(String input, String expectedOutput) {
            assertEquals(expectedOutput, ZaasHttpsClientProvider.formatKeyringUrl(input));
        }

    }

}
