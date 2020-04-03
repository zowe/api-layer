/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.client;

import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.junit.Before;
import org.junit.Test;
import org.zowe.apiml.zaasclient.config.ConfigProperties;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class HttpsClientTest {
    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";
    private ConfigProperties configProperties;
    private HttpsClient httpsClient;

    @Before
    public void setup() throws IOException {
        this.configProperties = getConfigProperties();
        this.httpsClient = new HttpsClient(configProperties);
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
    public void testGetHttpClientWithTrustStore() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        assertNotNull(httpsClient.getHttpsClientWithTrustStore());
    }

    @Test
    public void testGetHttpClientWithTrustStoreWithCookies() throws CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {
        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie cookie = new BasicClientCookie("apimlAuthenticationToken", "token");
        cookie.setDomain(configProperties.getApimlHost());
        cookie.setPath("/");
        cookieStore.addCookie(cookie);

        assertNotNull(httpsClient.getHttpsClientWithTrustStore(cookieStore));
    }

    @Test
    public void testGetHttpsClientWithKeyStoreAndTrustStore() throws CertificateException, UnrecoverableKeyException, NoSuchAlgorithmException, IOException, KeyManagementException, KeyStoreException {
        assertNotNull(httpsClient.getHttpsClientWithKeyStoreAndTrustStore());
    }
}
