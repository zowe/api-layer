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
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.utils.Base64;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.HttpClientResponseHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.zowe.apiml.util.HttpClientMockHelper;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.passticket.ZaasPassTicketResponse;
import org.zowe.apiml.zaasclient.service.ZaasToken;

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
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ZaasClientImplHttpsTests {

    private ZaasJwtService tokenService;
    private PassTicketService passTicketService;

    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";

    @Mock
    private Header header;
    @Mock
    private CloseableHttpClient closeableHttpClient;
    @Mock
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
        ConfigProperties configProperties = getConfigProperties();
        long now = System.currentTimeMillis();
        long expiration = now + 10000;
        long expirationForExpiredToken = now - 1000;

        Key jwtSecretKey = getDummyKey(configProperties);

        token = getToken(now, expiration, jwtSecretKey);
        expiredToken = getToken(now, expirationForExpiredToken, jwtSecretKey);
        invalidToken = token + "DUMMY TEXT";

        String baseUrl = "/gateway/api/v1/auth";
        tokenService = new ZaasJwtService(closeableHttpClient, baseUrl, configProperties);
        passTicketService = new PassTicketServiceImpl(closeableHttpClient, baseUrl, configProperties);
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

    private CloseableHttpResponse prepareResponse(int httpResponseCode, boolean withResponseHeaders) {
        CloseableHttpResponse response = mock(CloseableHttpResponse.class);
        doReturn(httpResponseCode).when(response).getCode();
        HttpClientMockHelper.mockExecuteWithResponse(closeableHttpClient, response);
        if (withResponseHeaders) {
            Header[] headers = new Header[]{header};
            when(response.getHeaders(HttpHeaders.SET_COOKIE)).thenReturn(headers);
            when(header.getValue()).thenReturn("apimlAuthenticationToken=token");
        }
        return response;
    }

    private void prepareResponseForServerUnavailable() {
        HttpClientMockHelper.whenExecuteThenThrow(closeableHttpClient, new IOException("An IO Exception"));
    }

    private void prepareResponseForUnexpectedException() {
        try {
            when(closeableHttpClient.execute(any(HttpPost.class), any(HttpClientResponseHandler.class)))
                .thenAnswer(invocation -> {
                    throw new Exception();
                });
        } catch (IOException e) {
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
        prepareResponse(HttpStatus.SC_NO_CONTENT, true);

        String token = tokenService.login(VALID_USER, VALID_PASSWORD);
        assertNotNull(token, "null Token obtained");
        assertNotEquals(EMPTY_STRING, token, "Empty Token obtained");
        assertEquals("token", token, "Token Mismatch");
    }

    @Test
    void testLoginWithCredentials_ValidUserName_ValidPassword_multipleResponseHeaders() throws ZaasClientException {
        var response = prepareResponse(HttpStatus.SC_NO_CONTENT, false);
        var tokenCookieHeader = mock(Header.class);
        Header[] headers = new Header[]{header, tokenCookieHeader};
        when(response.getHeaders(HttpHeaders.SET_COOKIE)).thenReturn(headers);
        when(header.getValue()).thenReturn("someCookie=cookieValue");
        when(tokenCookieHeader.getValue()).thenReturn("apimlAuthenticationToken=token");

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
        var response = prepareResponse(statusCode, false);
        when(response.getEntity()).thenReturn(httpsEntity);
        when(response.getHeaders(HttpHeaders.SET_COOKIE)).thenReturn(new Header[0]);

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
        prepareResponse(HttpStatus.SC_NO_CONTENT, true);
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
    void doLoginWithAuthHeaderInvalidUsername(int statusCode, String authHeader, ZaasClientErrorCodes expectedCode) {
        var response = prepareResponse(statusCode, false);
        when(response.getEntity()).thenReturn(httpsEntity);
        when(response.getHeaders(HttpHeaders.SET_COOKIE)).thenReturn(new Header[0]);

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
        var response = prepareResponse(200, false);
        when(response.getEntity()).thenReturn(httpsEntity);
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
        var response = prepareResponse(404, false);
        when(response.getEntity()).thenReturn(httpsEntity);

        assertThrows(ZaasClientException.class, () -> tokenService.query(token));
    }

    @Test
    void testLoginWithToken_WhenResponseCodeIs400_ZaasClientException() {
        var response = prepareResponse(400, false);
        when(response.getEntity()).thenReturn(httpsEntity);
        when(response.getHeaders(HttpHeaders.SET_COOKIE)).thenReturn(new Header[0]);

        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.login(token));
        assertTrue(exception.getMessage().contains("'ZWEAS121E', message='Empty or null username or password values provided'"), "Message was: " + exception.getMessage());
    }

    @Test
    void testPassTicketWithToken_ValidToken_ValidPassTicket() throws Exception {
        var response = prepareResponse(200, false);
        when(response.getEntity()).thenReturn(httpsEntity);
        ZaasPassTicketResponse zaasPassTicketResponse = new ZaasPassTicketResponse();
        zaasPassTicketResponse.setTicket("ticket");

        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasPassTicketResponse)));

        assertEquals("ticket", passTicketService.passTicket(token, "ZOWEAPPL"));
    }

    @Test
    void givenValidToken_whenLogout_thenSuccess() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT, true);
        String token = tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
        assertDoesNotThrow(() -> tokenService.logout(token));
    }

    @Test
    void givenInvalidToken_whenLogout_thenThrowException() {
        var response = prepareResponse(HttpStatus.SC_BAD_REQUEST, false);
        when(response.getEntity()).thenReturn(httpsEntity);
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> tokenService.logout("invalid"));

        assertTrue(exception.getMessage().contains("'ZWEAS130E', message='Invalid token provided'"));
    }

    @Test
    void givenValidTokenInBearer_whenLogout_thenSuccess() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT, true);
        String token = tokenService.login(getAuthHeader(VALID_USER, VALID_PASSWORD));
        token = "Bearer " + token;
        String finalToken = token;
        assertDoesNotThrow(() -> tokenService.logout(finalToken));
    }

    @Test
    void givenLogoutRequest_whenResponseCodeIs401_thenThrowException() {
        var response = prepareResponse(HttpStatus.SC_UNAUTHORIZED, false);
        when(response.getEntity()).thenReturn(httpsEntity);
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
