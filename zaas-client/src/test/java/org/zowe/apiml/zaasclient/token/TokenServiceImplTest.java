package org.zowe.apiml.zaasclient.token;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.zowe.apiml.zaasclient.client.HttpsClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TokenServiceImplTest {


    TokenServiceImpl tokenService;
    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";
    private static final String INVALID_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJsdHBhIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE1ODUwOTA3MDAsImV4cCI6MTU4NTE3NzEwMCwiaXNzIjoiQVBJTUwiLCJqdGkiOiI2NDcwNDViOS1hOTEwLTQxNjctOWNmMi1jODhmNDk3MWJjZWEifQ.Mo6qfM699EgcZ5jpgF3y6dSzdYklwrQfLCLZskSExXICwqaWw7E6CTWPc9j1u4MvurTjI5xGx2RsAmzMmxPpS8kgQPUZoGLBhza6Px9DVXzRYEFmIC2wD6b5BY2xP5f2N5Y4Mj5yiAFnOLQRFgKDaUhpohUa4kXCZjD5EanDPtyaHDlXGqVBmJLlzH7ZRYCAB2ROC0jvhKGa3UuHWbsbf4sX2m1NfgI9aGwZFZuQMAat-gvJJcBk5jGY4Cwo-bqlM_NV63LbhBrkMdgMUZFJtUi9qey6tE0CtzcbwCtfHAHMAKmrl1FSwQJHYvZiSoAEUaevovAEfnx6hf-CGrgJQginvalid";


    @Mock
    private HttpsClient httpsClient;

    @Mock
    private CloseableHttpResponse closeableHttpResponse;

    @Mock
    private CloseableHttpClient closeableHttpClient;

    @Mock
    private HttpGet httpGet;

    @Before
    public void setupMethod() throws IOException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException {
        MockitoAnnotations.initMocks(TokenServiceImplTest.class);
        ConfigProperties configProperties = getConfigProperties();
        tokenService = new TokenServiceImpl();
        tokenService.init(configProperties);

        when(httpsClient.getHttpsClientWithTrustStore(any())).thenReturn(closeableHttpClient);
        when(httpsClient.getHttpsClientWithTrustStore(any()).execute(httpGet)).thenReturn(closeableHttpResponse);

    }

    private ConfigProperties getConfigProperties() throws IOException {
        String absoluteFilePath = new File(CONFIG_FILE_PATH).getAbsolutePath();
        ConfigProperties configProperties = new ConfigProperties();
        Properties configProp = new Properties();
        try {
            if (Paths.get(absoluteFilePath).toFile().exists()) {
                configProp.load(new FileReader(absoluteFilePath));

                configProperties.setApimlHost(configProp.getProperty("APIML_HOST"));
                configProperties.setApimlPort(configProp.getProperty("APIML_PORT"));
                configProperties.setApimlBaseUrl(configProp.getProperty("APIML_BASE_URL"));
                configProperties.setKeyStorePath(configProp.getProperty("KEYSTOREPATH"));
                configProperties.setKeyStorePassword(configProp.getProperty("KEYSTOREPASSWORD"));
                configProperties.setKeyStoreType(configProp.getProperty("KEYSTORETYPE"));
                configProperties.setTrustStorePath(configProp.getProperty("TRUSTSTOREPATH"));
                configProperties.setTrustStorePassword(configProp.getProperty("TRUSTSTOREPASSWORD"));
                configProperties.setTrustStoreType(configProp.getProperty("TRUSTSTORETYPE"));
            }
        } catch (IOException e) {
            throw new IOException();
        }
        return configProperties;
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithInvalidToken() throws ZaasClientException {
        tokenService.query(INVALID_TOKEN);
    }

    @Test
    public void testQueryWithCorrectToken() throws ZaasClientException {
        assertNotNull(tokenService.query(tokenService.login("user", "user")));
    }

}
