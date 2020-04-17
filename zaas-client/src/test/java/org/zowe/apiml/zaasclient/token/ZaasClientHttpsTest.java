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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ZaasClientHttpsTest {
    private ZaasClientHttps zaasClient;
    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";

    private HttpsClient httpsClient;
    StatusLine statusLine;
    HeaderElement headerElement;
    Header header;
    private CloseableHttpResponse closeableHttpResponse;
    private CloseableHttpClient closeableHttpClient;
    private HttpEntity httpsEntity;

    private String token;
    private String expiredToken;
    private String invalidToken;

    private static final String VALID_USER = "user";
    private static final String VALID_PASSWORD = "user";
    private static final String INVALID_USER = "use";
    private static final String INVALID_PASSWORD = "uer";
    private static final String NULL_USER = null;
    private static final String NULL_PASSWORD = null;
    private static final String EMPTY_USER = "";
    private static final String EMPTY_PASSWORD = "";
    private static final String NULL_AUTH_HEADER = null;
    private static final String EMPTY_AUTH_HEADER = "";
    private static final String EMPTY_STRING = "";

    @BeforeEach
    public void setupMethod() throws IOException, CertificateException,
        NoSuchAlgorithmException, KeyStoreException, KeyManagementException, UnrecoverableKeyException, ZaasClientException {
        httpsClient = mock(HttpsClient.class);
        statusLine = mock(StatusLine.class);
        headerElement = mock(HeaderElement.class);
        header = mock(Header.class);
        closeableHttpResponse = mock(CloseableHttpResponse.class);
        closeableHttpClient = mock(CloseableHttpClient.class);
        httpsEntity = mock(HttpEntity.class);

        ConfigProperties configProperties = getConfigProperties();
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
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        zaasClient = new ZaasClientHttps(httpsClient, configProperties);
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

    private static String getAuthHeader(String userName, String password) {
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
        } catch (NoSuchAlgorithmException | KeyStoreException | IOException | CertificateException | KeyManagementException | ZaasClientException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForServerUnavailable() {
        try {
            when(httpsClient.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class))).thenThrow(IOException.class);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException | ZaasClientException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForUnexpectedException() {
        try {
            when(httpsClient.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class))).thenAnswer( invocation -> { throw new Exception(); });
        } catch (IOException | CertificateException | NoSuchAlgorithmException | KeyStoreException | KeyManagementException | ZaasClientException e) {
            e.printStackTrace();
        }
    }

    private void assertThatExceptionContainValidCode(ZaasClientException zce, ZaasClientErrorCodes code) {
        assertThat(code.getId(), is(zce.getErrorCode()));
        assertThat( code.getMessage(), is(zce.getErrorMessage()));
        assertThat(code.getReturnCode(), is(zce.getHttpResponseCode()));
    }

    @Test
    public void testLoginWithCredentials_ValidUserName_ValidPassword() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        String token = zaasClient.login(VALID_USER, VALID_PASSWORD);
        assertNotNull("null Token obtained", token);
        assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
        assertEquals("Token Mismatch","token", token);
    }

    private static Stream<Arguments> provideInvalidUsernamePassword() {
        return Stream.of(
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, INVALID_USER, VALID_PASSWORD, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, VALID_USER, INVALID_PASSWORD, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, EMPTY_USER, VALID_PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, VALID_USER, EMPTY_PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, NULL_USER, VALID_PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, VALID_USER, NULL_PASSWORD, ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(HttpStatus.SC_NOT_FOUND, VALID_USER, VALID_PASSWORD, ZaasClientErrorCodes.GENERIC_EXCEPTION)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUsernamePassword")
    public void giveInvalidCredentials_whenLoginIsRequested_thenProperExceptionIsRaised(int statusCode,
                                                                                        String username, String password,
                                                                                        ZaasClientErrorCodes expectedCode) {
        prepareResponse(statusCode);

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> zaasClient.login(username, password));

        assertThatExceptionContainValidCode(exception, expectedCode);
    }

    @Test
    public void testLoginWithCredentials_ServerUnavailable() {
        prepareResponseForServerUnavailable();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> zaasClient.login(VALID_USER, VALID_PASSWORD));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
    }

    @Test
    public void testLoginWithCredentials_UnexpectedException() {
        prepareResponseForUnexpectedException();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> zaasClient.login(VALID_USER, VALID_PASSWORD));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.GENERIC_EXCEPTION);
    }

    @Test
    public void testLoginWithAuthHeader_ValidUserName_ValidPassword() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        String token = zaasClient.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
        assertNotNull("null Token obtained", token);
        assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
        assertEquals("Token Mismatch","token", token);
    }

    private static Stream<Arguments> provideInvalidAuthHeaders() {
        return Stream.of(
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, getAuthHeader(INVALID_USER, VALID_PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, getAuthHeader(VALID_USER, INVALID_PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, getAuthHeader(EMPTY_USER, VALID_PASSWORD), ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, getAuthHeader(VALID_USER, EMPTY_PASSWORD), ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD),
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, getAuthHeader(NULL_USER, VALID_PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, getAuthHeader(VALID_USER, NULL_PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_NOT_FOUND, getAuthHeader(VALID_USER, NULL_PASSWORD), ZaasClientErrorCodes.GENERIC_EXCEPTION),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, NULL_AUTH_HEADER, ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER),
            Arguments.of(HttpStatus.SC_BAD_REQUEST, EMPTY_AUTH_HEADER, ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidAuthHeaders")
    public void doLoginWithAuthHeaderInValidUsername(int statusCode, String authHeader, ZaasClientErrorCodes expectedCode) {
        prepareResponse(statusCode);

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> zaasClient.login(authHeader));

        assertThatExceptionContainValidCode(exception, expectedCode);
    }

    @Test
    public void testLoginWithAuthHeader_ServerUnavailable() {
        prepareResponseForServerUnavailable();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> zaasClient.login(getAuthHeader(VALID_USER, VALID_PASSWORD)));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
    }

    @Test
    public void testLoginWithAuthHeader_UnexpectedException() {
        prepareResponseForUnexpectedException();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> zaasClient.login(getAuthHeader(VALID_USER, VALID_PASSWORD)));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.GENERIC_EXCEPTION);
    }

    @Test
    public void testQueryWithCorrectToken_ValidToken_ValidTokenDetails() throws ZaasClientException, IOException {
        ZaasToken zaasToken = new ZaasToken();
        zaasToken.setUserId("user");
        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasToken)));

        assertEquals("user", zaasClient.query(token).userId);
    }

    @Test
    public void testQueryWithToken_InvalidToken_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> zaasClient.query(invalidToken));
    }

    @Test
    public void testQueryWithToken_ExpiredToken_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> zaasClient.query(expiredToken));
    }

    @Test
    public void testQueryWithToken_EmptyToken_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> zaasClient.query(""));
    }

    @Test
    public void testQueryWithToken_WhenResponseCodeIs404_ZaasClientException() {
        when(closeableHttpResponse.getStatusLine().getStatusCode()).thenReturn(404);

        assertThrows(ZaasClientException.class, () -> zaasClient.query(token));
    }

    @Test
    public void testPassTicketWithToken_ValidToken_ValidPassTicket() throws ZaasClientException,
        UnrecoverableKeyException, CertificateException, NoSuchAlgorithmException, KeyStoreException, KeyManagementException, IOException {

        ZaasPassTicketResponse zaasPassTicketResponse = new ZaasPassTicketResponse();
        zaasPassTicketResponse.setTicket("ticket");

        when(httpsClient.getHttpsClientWithKeyStoreAndTrustStore()).thenReturn(closeableHttpClient);
        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasPassTicketResponse)));

        assertEquals("ticket", zaasClient.passTicket(token, "ZOWEAPPL"));
    }

    @Test
    public void testPassTicketWithInvalidToken_InvalidToken_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> zaasClient.passTicket(invalidToken, "ZOWEAPPL"));
    }

    @Test
    public void testPassTicketWithEmptyToken_EmptyToken_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> zaasClient.passTicket("", "ZOWEAPPL"));
    }

    @Test
    public void testPassTicketWithEmptyApplicationId_EmptyApplicationId_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> zaasClient.passTicket("", ""));
    }
}
