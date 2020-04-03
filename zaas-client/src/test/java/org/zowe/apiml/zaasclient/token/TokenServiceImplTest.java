package org.zowe.apiml.zaasclient.token;
/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.commons.codec.binary.Base64;
import org.apache.http.*;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;
import org.zowe.apiml.zaasclient.client.HttpsClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.passticket.ZaasPassTicketResponse;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.*;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Silent.class)
public class TokenServiceImplTest {

    private TokenServiceImpl tokenService;
    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";

    @Mock
    private HttpsClient httpsClient;

    @Mock
    StatusLine statusLine;

    @Mock
    HeaderElement headerElement;

    @Mock
    Header header;

    @Mock
    private CloseableHttpResponse closeableHttpResponse;

    @Mock
    private CloseableHttpClient closeableHttpClient;

    @Mock
    private HttpEntity httpsEntity;

    private String token;
    private String expiredToken;
    private String invalidToken;

    private static final String VALID_USER = "user";
    private static final String VALID_PASSWORD = "user";
    private static final String INAVLID_USER = "use";
    private static final String INVALID_PASSWORD = "uer";
    private static final String NULL_USER = null;
    private static final String NULL_PASSWORD = null;
    private static final String EMPTY_USER = "";
    private static final String EMPTY_PASSWORD = "";
    private static final String NULL_AUTH_HEADER = null;
    private static final String EMPTY_AUTH_HEADER = "";
    private static final String EMPTY_STRING = "";

    @Before
    public void setupMethod() throws IOException, CertificateException,
        NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException {
        MockitoAnnotations.initMocks(TokenServiceImplTest.class);
        ConfigProperties configProperties = getConfigProperties();
        tokenService = new TokenServiceImpl();
        tokenService.init(configProperties);
        long now = System.currentTimeMillis();
        long expiration = now + 10000;
        long expirationForExpiredToken = now - 1000;

        Key jwtSecretKey = getDummyKey(configProperties);

        token = getToken(now, expiration, jwtSecretKey);
        expiredToken = getToken(now, expirationForExpiredToken, jwtSecretKey);
        invalidToken = token + "DUMMY TEXT";

        when(httpsClient.getHttpsClientWithTrustStore(any(BasicCookieStore.class))).thenReturn(closeableHttpClient);
        when(closeableHttpClient.execute(any(HttpGet.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpClient.execute(any(HttpPost.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpsEntity);
        tokenService.setHttpsClient(httpsClient);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

    }

    private String getToken(long now, long expiration, Key jwtSecretKey) {
        return Jwts.builder()
            .setSubject("user")
            .setIssuedAt(new Date(now))
            .setExpiration(new Date(expiration))
            .setIssuer("APIML")
            .setId(UUID.randomUUID().toString())
            .signWith(SignatureAlgorithm.RS256, jwtSecretKey)
            .compact();
    }

    private Key getDummyKey(ConfigProperties configProperties) throws KeyStoreException, IOException, NoSuchAlgorithmException, CertificateException, UnrecoverableKeyException {
        InputStream inputStream;

        KeyStore ks = KeyStore.getInstance(configProperties.getKeyStoreType());

        File keyStoreFile = new File(configProperties.getKeyStorePath());
        inputStream = new FileInputStream(keyStoreFile);
        ks.load(inputStream, configProperties.getKeyStorePassword() == null ? null : configProperties.getKeyStorePassword().toCharArray());

        return ks.getKey("jwtsecret",
            configProperties.getKeyStorePassword() == null ? null : configProperties.getKeyStorePassword().toCharArray());
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

    private String getAuthHeader(String userName, String password) {
        String auth = userName + ":" + password;
        byte[] encodedAuth = Base64.encodeBase64(
            auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }

    private void prepareResponse(int httpResponseCode) {
        Header[] headers = new Header[1];
        headers[0] = header;

        HeaderElement[] headerElements = new HeaderElement[1];
        headerElements[0] = headerElement;
        try {
            when(httpsClient.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(statusLine.getStatusCode()).thenReturn(httpResponseCode);
            when(closeableHttpResponse.getHeaders("Set-Cookie")).thenReturn(headers);
            when(header.getElements()).thenReturn(headerElements);
            when(headerElement.getName()).thenReturn("apimlAuthenticationToken");
            when(headerElement.getValue()).thenReturn("token");
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForServerUnavailable() {
        try {
            when(httpsClient.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class))).thenThrow(IOException.class);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForUnexpectedException() {
        try {
            when(httpsClient.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class))).thenAnswer( invocation -> { throw new Exception(); });
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testLoginWithCredentials_ValidUserName_ValidPassword() {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        try {
            String token = tokenService.login(VALID_USER, VALID_PASSWORD);
            assertNotNull("null Token obtained", token);
            assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
            assertEquals("Token Mismatch","token", token);
        } catch (ZaasClientException zce) {
            fail("Test case failed as it threw an exception");
        }
    }

    @Test
    public void testLoginWithCredentials_InvalidUserName_ValidPassword() {
        prepareResponse(HttpStatus.SC_UNAUTHORIZED);
        try {
            tokenService.login(INAVLID_USER, VALID_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_ValidUserName_InvalidPassword() {
        prepareResponse(HttpStatus.SC_UNAUTHORIZED);
        try {
            tokenService.login(VALID_USER, INVALID_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_EmptyUserName_InvalidPassword() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(EMPTY_USER, VALID_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_ValidUserName_EmptyPassword() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(VALID_USER, EMPTY_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_NullUserName_ValidPassword() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(NULL_USER, VALID_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_ValidUserName_NullPassword() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(VALID_USER, NULL_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_ServerUnavailable() {
        prepareResponseForServerUnavailable();
        try {
            tokenService.login(VALID_USER, VALID_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_GenericException() {
        prepareResponse(HttpStatus.SC_NOT_FOUND);
        try {
            tokenService.login(VALID_USER, VALID_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithCredentials_UnexpectedException() {
        prepareResponseForUnexpectedException();
        try {
            tokenService.login(VALID_USER, VALID_PASSWORD);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_ValidUserName_ValidPassword() {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        try {
            String token = tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
            assertNotNull("null Token obtained", token);
            assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
            assertEquals("Token Mismatch","token", token);
        } catch (ZaasClientException zce) {
            fail("Test case failed as it threw an exception");
        }
    }

    @Test
    public void testLoginWithAuthHeader_InvalidUserName_ValidPassword() {
        prepareResponse(HttpStatus.SC_UNAUTHORIZED);
        try {
            tokenService.login(getAuthHeader(INAVLID_USER, VALID_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_ValidUserName_InvalidPassword() {
        prepareResponse(HttpStatus.SC_UNAUTHORIZED);
        try {
            tokenService.login(getAuthHeader(VALID_USER, INVALID_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_EmptyUserName_InvalidPassword() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(getAuthHeader(EMPTY_USER, VALID_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_ValidUserName_EmptyPassword() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(getAuthHeader(VALID_USER, EMPTY_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_NullUserName_ValidPassword() {
        prepareResponse(HttpStatus.SC_UNAUTHORIZED);
        try {
            tokenService.login(getAuthHeader(NULL_USER, VALID_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_ValidUserName_NullPassword() {
        prepareResponse(HttpStatus.SC_UNAUTHORIZED);
        try {
            tokenService.login(getAuthHeader(VALID_USER, NULL_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.INVALID_AUTHENTICATION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_EmptyHeader() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(EMPTY_AUTH_HEADER);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_NullHeader() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        try {
            tokenService.login(NULL_AUTH_HEADER);
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_ServerUnavailable() {
        prepareResponseForServerUnavailable();
        try {
            tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.SERVICE_UNAVAILABLE.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_GenericException() {
        prepareResponse(HttpStatus.SC_NOT_FOUND);
        try {
            tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test
    public void testLoginWithAuthHeader_UnexpectedException() {
        prepareResponseForUnexpectedException();
        try {
            tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
            fail("Test case failed as it didn't throw an exception");
        } catch (ZaasClientException zce) {
            assertEquals("Error Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getId(), zce.getErrorCode());
            assertEquals("Error Message Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getMessage(), zce.getErrorMessage());
            assertEquals("HTTP Return Code Mismatch", ZaasClientErrorCodes.GENERIC_EXCEPTION.getReturnCode(), zce.getHttpResponseCode());
        }
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithToken_InvalidToken_ZaasClientException() throws ZaasClientException {
        tokenService.query(invalidToken);
    }

    @Test
    public void testQueryWithCorrectToken_ValidToken_ValidTokenDetails() throws ZaasClientException, IOException {
        ZaasToken zaasToken = new ZaasToken();
        zaasToken.setUserId("user");
        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasToken)));

        assertEquals("user", tokenService.query(token).userId);
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithToken_ExpiredToken_ZaasClientException() throws ZaasClientException {
        tokenService.query(expiredToken);
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithToken_EmptyToken_ZaasClientException() throws ZaasClientException {
        tokenService.query("");
    }

    @Test(expected = ZaasClientException.class)
    public void testQueryWithToken_WhenResponseCodeIs404_ZaasClientException() throws ZaasClientException {
        when(closeableHttpResponse.getStatusLine().getStatusCode()).thenReturn(404);
        tokenService.query(token);
    }

    @Test
    public void testPassTicketWithToken_ValidToken_ValidPassTicket() throws ZaasClientException,
        UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

        ZaasPassTicketResponse zaasPassTicketResponse = new ZaasPassTicketResponse();
        zaasPassTicketResponse.setTicket("ticket");

        when(httpsClient.getHttpsClientWithKeyStoreAndTrustStore()).thenReturn(closeableHttpClient);
        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasPassTicketResponse)));

        assertEquals("ticket", tokenService.passTicket(token, "ZOWEAPPL"));
    }

    @Test(expected = ZaasClientException.class)
    public void testPassTicketWithInvalidToken_InvalidToken_ZaasClientException() throws ZaasClientException {
        tokenService.passTicket(invalidToken, "ZOWEAPPL");
    }

    @Test(expected = ZaasClientException.class)
    public void testPassTicketWithEmptyToken_EmptyToken_ZaasClientException() throws ZaasClientException {
        tokenService.passTicket("", "ZOWEAPPL");
    }

    @Test(expected = ZaasClientException.class)
    public void testPassTicketWithEmptyApplicationId_EmptyApplicationId_ZaasClientException() throws ZaasClientException {
        tokenService.passTicket("", "");
    }
}
