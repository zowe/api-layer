package org.zowe.apiml.zaasclient.token;

import org.junit.Before;
import org.junit.Test;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Properties;

import static org.junit.Assert.assertNotNull;

public class TokenServiceImplTest {

    //TODO: this unit tests run successfuly only when the gateway application is running .. we need to find a way to make them run independently of the gateway

    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";
    TokenServiceImpl tokenService;
    private static final String EXPIRED_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJsdHBhIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE1ODUwOTA3MDAsImV4cCI6MTU4NTE3NzEwMCwiaXNzIjoiQVBJTUwiLCJqdGkiOiI2NDcwNDViOS1hOTEwLTQxNjctOWNmMi1jODhmNDk3MWJjZWEifQ.Mo6qfM699EgcZ5jpgF3y6dSzdYklwrQfLCLZskSExXICwqaWw7E6CTWPc9j1u4MvurTjI5xGx2RsAmzMmxPpS8kgQPUZoGLBhza6Px9DVXzRYEFmIC2wD6b5BY2xP5f2N5Y4Mj5yiAFnOLQRFgKDaUhpohUa4kXCZjD5EanDPtyaHDlXGqVBmJLlzH7ZRYCAB2ROC0jvhKGa3UuHWbsbf4sX2m1NfgI9aGwZFZuQMAat-gvJJcBk5jGY4Cwo-bqlM_NV63LbhBrkMdgMUZFJtUi9qey6tE0CtzcbwCtfHAHMAKmrl1FSwQJHYvZiSoAEUaevovAEfnx6hf-CGrgJQg";
    private static final String INVALID_TOKEN = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyIiwiZG9tIjoiRHVtbXkgcHJvdmlkZXIiLCJsdHBhIjoiRHVtbXkgcHJvdmlkZXIiLCJpYXQiOjE1ODUwOTA3MDAsImV4cCI6MTU4NTE3NzEwMCwiaXNzIjoiQVBJTUwiLCJqdGkiOiI2NDcwNDViOS1hOTEwLTQxNjctOWNmMi1jODhmNDk3MWJjZWEifQ.Mo6qfM699EgcZ5jpgF3y6dSzdYklwrQfLCLZskSExXICwqaWw7E6CTWPc9j1u4MvurTjI5xGx2RsAmzMmxPpS8kgQPUZoGLBhza6Px9DVXzRYEFmIC2wD6b5BY2xP5f2N5Y4Mj5yiAFnOLQRFgKDaUhpohUa4kXCZjD5EanDPtyaHDlXGqVBmJLlzH7ZRYCAB2ROC0jvhKGa3UuHWbsbf4sX2m1NfgI9aGwZFZuQMAat-gvJJcBk5jGY4Cwo-bqlM_NV63LbhBrkMdgMUZFJtUi9qey6tE0CtzcbwCtfHAHMAKmrl1FSwQJHYvZiSoAEUaevovAEfnx6hf-CGrgJQginvalid";


    @Before
    public void setupMethod() throws IOException {
        ConfigProperties configProperties = getConfigProperties();
        tokenService = new TokenServiceImpl();
        tokenService.init(configProperties);

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

    @Test
    public void testQueryApiWithCorrectToken() throws ZaasClientException {
        String token = tokenService.login("user", "user");
        assertNotNull(tokenService.query(token));
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryApiWithExpiredJwtToken() throws ZaasClientException {
        tokenService.query(INVALID_TOKEN);
    }

    @Test
    public void testQueryApiWhenGatewayIsNotRunning() {
        //TODO: how to handle
    }
}
