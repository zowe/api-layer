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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.classic.methods.HttpUriRequestBase;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HeaderElement;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.apache.hc.core5.http.message.StatusLine;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.passticket.ZaasPassTicketResponse;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Date;
import java.util.Properties;
import java.util.UUID;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZaasClientImplHttpsTests {
    private ZaasJwtService tokenService;
    private PassTicketService passTicketService;

    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";

    private ZaasHttpsClientProvider zaasHttpsClientProvider;
    StatusLine statusLine;
    HeaderElement headerElement;
    Header header;
    private ClassicHttpResponse closeableHttpResponse;
    private CloseableHttpClient closeableHttpClient;
    private HttpEntity httpsEntity;

    private String token;
    private String expiredToken;
    private String invalidToken;

    private static final String VALID_USER = "user";
    private static final char[] VALID_PASSWORD = "user".toCharArray();
    private static final String INVALID_USER = "use";
    private static final char[] INVALID_PASSWORD = "uer".toCharArray();
    private static final String EMPTY_STRING = "";

    @BeforeEach
    void setupMethod() throws Exception {
        zaasHttpsClientProvider = mock(ZaasHttpsClientProvider.class);
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

        when(zaasHttpsClientProvider.getHttpClient()).thenReturn(closeableHttpClient);
        when(closeableHttpClient.execute(any(HttpGet.class), any(HttpClientResponseHandler.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpClient.execute(any(HttpPost.class), any(HttpClientResponseHandler.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpsEntity);
        when(closeableHttpResponse.getCode()).thenReturn(HttpStatus.SC_OK);

        String baseUrl = "/gateway/api/v1/auth";
        tokenService = new ZaasJwtService(zaasHttpsClientProvider, baseUrl, configProperties);
        passTicketService = new PassTicketServiceImpl(zaasHttpsClientProvider, baseUrl, configProperties);
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
        ks.load(inputStream, configProperties.getKeyStorePassword());

        return ks.getKey("localhost", configProperties.getKeyStorePassword());
    }

    // TODO: Change to the way used in enablers.
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
                String keyStorePassword = configProp.getProperty("KEYSTOREPASSWORD");
                configProperties.setKeyStorePassword(keyStorePassword == null ? null : keyStorePassword.toCharArray());
                configProperties.setKeyStoreType(configProp.getProperty("KEYSTORETYPE"));
                configProperties.setTrustStorePath(configProp.getProperty("TRUSTSTOREPATH"));
                String trustStorePassword = configProp.getProperty("TRUSTSTOREPASSWORD");
                configProperties.setTrustStorePassword(trustStorePassword == null ? null : trustStorePassword.toCharArray());
                configProperties.setTrustStoreType(configProp.getProperty("TRUSTSTORETYPE"));
            }
        } catch (IOException e) {
            throw new IOException();
        }
        return configProperties;
    }

    private static String getAuthHeader(String userName, char[] password) {
        String auth = userName + ":" + new String(password);
        byte[] encodedAuth = Base64.encodeBase64(
            auth.getBytes(StandardCharsets.ISO_8859_1));
        return "Basic " + new String(encodedAuth);
    }

    private void prepareResponse(int httpResponseCode) {
        Header[] headers = new Header[1];
        headers[0] = header;

        try {
            ClassicHttpResponse response = mock(ClassicHttpResponse.class);
            doReturn(httpResponseCode)
                .when(response).getCode();
            when(zaasHttpsClientProvider.getHttpClient()).thenReturn(closeableHttpClient);
            doReturn(response).when(closeableHttpClient).execute(any(HttpUriRequestBase.class), any(HttpClientResponseHandler.class));
            when(response.getEntity()).thenReturn(httpsEntity);
            when(response.getHeaders("Cookie")).thenReturn(headers);
            when(header.getName()).thenReturn("apimlAuthenticationToken");
            when(header.getValue()).thenReturn("token");
        } catch (ZaasConfigurationException | IOException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForServerUnavailable() {
        try {
            when(zaasHttpsClientProvider.getHttpClient()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class), any(HttpClientResponseHandler.class))).thenThrow(IOException.class);
        } catch (IOException | ZaasConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForUnexpectedException() {
        try {
            when(zaasHttpsClientProvider.getHttpClient()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class), any(HttpClientResponseHandler.class))).thenAnswer(invocation -> {
                throw new Exception();
            });
        } catch (IOException | ZaasConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void assertThatExceptionContainValidCode(ZaasClientException zce, ZaasClientErrorCodes code) {
        ZaasClientErrorCodes producedErrorCode = zce.getErrorCode();
        assertThat(producedErrorCode.getId(), is(code.getId()));
        assertThat(producedErrorCode.getMessage(), is(code.getMessage()));
        assertThat(producedErrorCode.getReturnCode(), is(code.getReturnCode()));
    }

    @Test
    void testLoginWithCredentials_ValidUserName_ValidPassword() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        String token = tokenService.login(VALID_USER, VALID_PASSWORD);
        assertNotNull(token, "null Token obtained");
        assertNotEquals(EMPTY_STRING, token, "Empty Token obtained");
        assertEquals("token", token, "Token Mismatch");
    }

    private static Stream<Arguments> provideInvalidUsernamePassword() {
        return Stream.of(
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, INVALID_USER, VALID_PASSWORD, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, VALID_USER, INVALID_PASSWORD, ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_NOT_FOUND, VALID_USER, VALID_PASSWORD, ZaasClientErrorCodes.GENERIC_EXCEPTION)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUsernamePassword")
    void giveInvalidCredentials_whenLoginIsRequested_thenProperExceptionIsRaised(int statusCode,
                                                                                 String username, char[] password,
                                                                                 ZaasClientErrorCodes expectedCode) {
        prepareResponse(statusCode);

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(username, password));

        assertThatExceptionContainValidCode(exception, expectedCode);
    }

    @Test
    void testLoginWithCredentials_ServerUnavailable() {
        prepareResponseForServerUnavailable();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(VALID_USER, VALID_PASSWORD));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
    }

    @Test
    void testLoginWithCredentials_UnexpectedException() {
        prepareResponseForUnexpectedException();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(VALID_USER, VALID_PASSWORD));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.GENERIC_EXCEPTION);
    }

    @Test
    void testLoginWithAuthHeader_ValidUserName_ValidPassword() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        String token = tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
        assertNotNull(token, "null Token obtained");
        assertNotEquals(EMPTY_STRING, token, "Empty Token obtained");
        assertEquals("token", token, "Token Mismatch");
    }

    private static Stream<Arguments> provideInvalidAuthHeaders() {
        return Stream.of(
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, getAuthHeader(INVALID_USER, VALID_PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION),
            Arguments.of(HttpStatus.SC_UNAUTHORIZED, getAuthHeader(VALID_USER, INVALID_PASSWORD), ZaasClientErrorCodes.INVALID_AUTHENTICATION)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidAuthHeaders")
    void doLoginWithAuthHeaderInValidUsername(int statusCode, String authHeader, ZaasClientErrorCodes expectedCode) {
        prepareResponse(statusCode);

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(authHeader));

        assertThatExceptionContainValidCode(exception, expectedCode);
    }

    @Test
    void testLoginWithAuthHeader_ServerUnavailable() {
        prepareResponseForServerUnavailable();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD)));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
    }

    @Test
    void testLoginWithAuthHeader_UnexpectedException() {
        prepareResponseForUnexpectedException();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD)));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.GENERIC_EXCEPTION);
    }

    @Test
    void testQueryWithCorrectToken_ValidToken_ValidTokenDetails() throws ZaasClientException, IOException {
        ZaasToken zaasToken = new ZaasToken();
        zaasToken.setUserId("user");
        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasToken)));

        assertEquals("user", tokenService.query(token).getUserId());
    }

    @Test
    void testQueryWithToken_InvalidToken_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> tokenService.query(invalidToken));
    }

    @Test
    void testQueryWithToken_ExpiredToken_ZaasClientException() {
        assertThrows(ZaasClientException.class, () -> tokenService.query(expiredToken));
    }

    @Test
    void testQueryWithToken_WhenResponseCodeIs404_ZaasClientException() {
        when(closeableHttpResponse.getCode()).thenReturn(404);

        assertThrows(ZaasClientException.class, () -> tokenService.query(token));
    }

    @Test
    void testLoginWithToken_WhenResponseCodeIs400_ZaasClientException() {
        when(closeableHttpResponse.getCode()).thenReturn(400);

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(token));
        assertTrue(exception.getMessage().contains("'ZWEAS121E', message='Empty or null username or password values provided'"));
    }

    @Test
    void testPassTicketWithToken_ValidToken_ValidPassTicket() throws Exception {

        ZaasPassTicketResponse zaasPassTicketResponse = new ZaasPassTicketResponse();
        zaasPassTicketResponse.setTicket("ticket");

        when(zaasHttpsClientProvider.getHttpClient()).thenReturn(closeableHttpClient);
        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasPassTicketResponse)));

        assertEquals("ticket", passTicketService.passTicket(token, "ZOWEAPPL"));
    }

    @Test
    void givenValidToken_whenLogout_thenSuccess() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        String token = tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
        assertDoesNotThrow(() -> tokenService.logout(token));
    }

    @Test
    void givenInvalidToken_whenLogout_thenThrowException() {
        prepareResponse(HttpStatus.SC_BAD_REQUEST);
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.logout("invalid"));

        assertTrue(exception.getMessage().contains("'ZWEAS130E', message='Invalid token provided'"));
    }

    @Test
    void givenValidTokenInBearer_whenLogout_thenSuccess() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        String token = tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
        token = "Bearer " + token;
        String finalToken = token;
        assertDoesNotThrow(() -> tokenService.logout(finalToken));
    }

    @Test
    void givenLogoutRequest_whenResponseCodeIs401_thenThrowException() {
        prepareResponse(HttpStatus.SC_UNAUTHORIZED);
//        when(closeableHttpResponse.getCode()).thenReturn(401);
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.logout("token"));

        assertTrue(exception.getMessage().contains("'ZWEAS100E', message='Token is expired for URL'"));
    }

    @Test
    void givenLogoutCall_whenGetIOException_thenThrowException() {
        prepareResponseForServerUnavailable();

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.logout("token"));

        assertThatExceptionContainValidCode(exception, ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
    }
}
