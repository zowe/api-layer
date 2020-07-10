package org.zowe.apiml.zaasclient.service.internal;
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
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
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
import static org.junit.Assert.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ZaasClientHttpsTest {
    private TokenService tokenService;
    private PassTicketService passTicketService;

    private static final String CONFIG_FILE_PATH = "src/test/resources/configFile.properties";

    private HttpsClientProvider httpsClientProvider;
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
    private static final String EMPTY_STRING = "";

    @BeforeEach
    void setupMethod() throws Exception {
        httpsClientProvider = mock(HttpsClientProvider.class);
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

        when(httpsClientProvider.getHttpsClientWithTrustStore(any(BasicCookieStore.class))).thenReturn(closeableHttpClient);
        when(closeableHttpClient.execute(any(HttpGet.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpClient.execute(any(HttpPost.class))).thenReturn(closeableHttpResponse);
        when(closeableHttpResponse.getEntity()).thenReturn(httpsEntity);
        when(closeableHttpResponse.getStatusLine()).thenReturn(statusLine);
        when(statusLine.getStatusCode()).thenReturn(HttpStatus.SC_OK);

        String baseUrl = "/api/v1/gateway/auth";
        tokenService = new TokenServiceHttpsJwt(httpsClientProvider, baseUrl, "localhost");
        passTicketService = new PassTicketServiceHttps(httpsClientProvider, baseUrl);
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
            when(httpsClientProvider.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(statusLine.getStatusCode()).thenReturn(httpResponseCode);
            when(closeableHttpResponse.getHeaders("Set-Cookie")).thenReturn(headers);
            when(header.getElements()).thenReturn(headerElements);
            when(headerElement.getName()).thenReturn("apimlAuthenticationToken");
            when(headerElement.getValue()).thenReturn("token");
        } catch (ZaasConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForServerUnavailable() {
        try {
            when(httpsClientProvider.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class))).thenThrow(IOException.class);
        } catch (IOException | ZaasConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void prepareResponseForUnexpectedException() {
        try {
            when(httpsClientProvider.getHttpsClientWithTrustStore()).thenReturn(closeableHttpClient);
            when(closeableHttpClient.execute(any(HttpPost.class))).thenAnswer( invocation -> { throw new Exception(); });
        } catch (IOException | ZaasConfigurationException e) {
            e.printStackTrace();
        }
    }

    private void assertThatExceptionContainValidCode(ZaasClientException zce, ZaasClientErrorCodes code) {
        ZaasClientErrorCodes producedErrorCode = zce.getErrorCode();
        assertThat(code.getId(), is(producedErrorCode.getId()));
        assertThat( code.getMessage(), is(producedErrorCode.getMessage()));
        assertThat(code.getReturnCode(), is(producedErrorCode.getReturnCode()));
    }

    @Test
    void testLoginWithCredentials_ValidUserName_ValidPassword() throws ZaasClientException {
        prepareResponse(HttpStatus.SC_NO_CONTENT);
        String token = tokenService.login(VALID_USER, VALID_PASSWORD);
        assertNotNull("null Token obtained", token);
        assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
        assertEquals("Token Mismatch","token", token);
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
                                                                                        String username, String password,
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
        assertNotNull("null Token obtained", token);
        assertNotEquals("Empty Token obtained", EMPTY_STRING, token);
        assertEquals("Token Mismatch","token", token);
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
        when(closeableHttpResponse.getStatusLine().getStatusCode()).thenReturn(404);

        assertThrows(ZaasClientException.class, () -> tokenService.query(token));
    }

    @Test
    void testPassTicketWithToken_ValidToken_ValidPassTicket() throws Exception {

        ZaasPassTicketResponse zaasPassTicketResponse = new ZaasPassTicketResponse();
        zaasPassTicketResponse.setTicket("ticket");

        when(httpsClientProvider.getHttpsClientWithKeyStoreAndTrustStore()).thenReturn(closeableHttpClient);
        when(httpsEntity.getContent()).thenReturn(new ByteArrayInputStream(new ObjectMapper().writeValueAsBytes(zaasPassTicketResponse)));

        assertEquals("ticket", passTicketService.passTicket(token, "ZOWEAPPL"));
    }
}
