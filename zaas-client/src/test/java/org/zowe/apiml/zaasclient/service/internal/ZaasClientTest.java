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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.config.DefaultZaasClientConfiguration;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes.*;
import static org.zowe.apiml.zaasclient.exception.ZaasConfigurationErrorCodes.IO_CONFIGURATION_ISSUE;

class ZaasClientTest {
    private ZaasClient underTest;
    private TokenService tokens;
    private PassTicketService passTickets;

    private static final char[] VALID_PASSWORD = "password".toCharArray();
    private static final String VALID_USERNAME = "username";
    private static final char[] VALID_NEW_PASSWORD = "username".toCharArray();
    private static final String VALID_APPLICATION_ID = "APPLID";
    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    private static final String[] SSL_SYSTEM_ENVIRONMENT_VALUES = {
        "javax.net.ssl.keyStore",
        "javax.net.ssl.keyStorePassword",
        "javax.net.ssl.keyStoreType",
        "javax.net.ssl.trustStore",
        "javax.net.ssl.trustStorePassword",
        "javax.net.ssl.trustStoreType"
    };

    @BeforeEach
    void setUp() {
        tokens = mock(TokenService.class);
        passTickets = mock(PassTicketService.class);

        underTest = new ZaasClientImpl(tokens, passTickets);
    }

    @AfterEach
    void assertSystemEnvironmentValues() {
        for (String env : SSL_SYSTEM_ENVIRONMENT_VALUES) {
            assertNull(System.getProperty(env));
        }
    }

    private void assertThatExceptionContainValidCode(ZaasClientException zce, ZaasClientErrorCodes code) {
        ZaasClientErrorCodes producedErrorCode = zce.getErrorCode();
        assertThat(code.getId(), is(producedErrorCode.getId()));
        assertThat(code.getMessage(), is(producedErrorCode.getMessage()));
        assertThat(code.getReturnCode(), is(producedErrorCode.getReturnCode()));
    }

    private static Stream<Arguments> provideInvalidUsernamePassword() {
        return Stream.of(
            Arguments.of(null, VALID_PASSWORD, null),
            Arguments.of("", VALID_PASSWORD, null),
            Arguments.of(VALID_USERNAME, null, null),
            Arguments.of(VALID_USERNAME, "".toCharArray(), null)
        );
    }

    private static Stream<Arguments> provideValidUsernamePasswordWithInvalidNewPassword() {
        return Stream.of(
            Arguments.of(VALID_USERNAME, VALID_PASSWORD, null),
            Arguments.of(VALID_USERNAME, VALID_PASSWORD, "".toCharArray())
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUsernamePassword")
    void givenFullyInvalidCredentials_whenLoggingIn_thenExceptionIsRaised(String username, char[] password) {
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> underTest.login(username, password));

        assertThatExceptionContainValidCode(exception, EMPTY_NULL_USERNAME_PASSWORD);
    }

    @ParameterizedTest
    @MethodSource("provideValidUsernamePasswordWithInvalidNewPassword")
    void givenFullyInvalidCredentials_whenLoggingInWithPasswordChange_thenExceptionIsRaised(String username, char[] password, char[] newPassword) {
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> underTest.login(username, password, newPassword));

        assertThatExceptionContainValidCode(exception, EMPTY_NULL_USERNAME_PASSWORD);
    }

    static Stream<String> provideNullEmptyArguments() {
        return Stream.of("", null);
    }

    @ParameterizedTest
    @MethodSource("provideNullEmptyArguments")
    void givenFullyInvalidAuthorizationHeader_whenLoggingIn_thenExceptionIsRaised(String authorizationHeader) {
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> underTest.login(authorizationHeader));

        assertThatExceptionContainValidCode(exception, EMPTY_NULL_AUTHORIZATION_HEADER);
    }

    private static Stream<Arguments> provideInvalidPassTicketSource() {
        return Stream.of(
            Arguments.of(null, VALID_APPLICATION_ID, TOKEN_NOT_PROVIDED),
            Arguments.of("", VALID_APPLICATION_ID, TOKEN_NOT_PROVIDED),
            Arguments.of(VALID_TOKEN, null, APPLICATION_NAME_NOT_FOUND),
            Arguments.of(VALID_TOKEN, "", APPLICATION_NAME_NOT_FOUND)
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidPassTicketSource")
    void givenFullyInvalidApplicationId_whenGettingPassticket_thenExceptionIsRaised(String token,
                                                                                           String applicationId,
                                                                                           ZaasClientErrorCodes errorCode) {
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> underTest.passTicket(token, applicationId));

        assertThatExceptionContainValidCode(exception, errorCode);
    }

    @Test
    void givenValidCredentials_whenLoginApiIsCalled_thenRaisedExceptionIsRethrown() throws Exception {
        when(tokens.login(anyString(), any())).thenThrow(new ZaasClientException(SERVICE_UNAVAILABLE));

        assertThrows(ZaasClientException.class, () -> underTest.login(VALID_USERNAME, VALID_PASSWORD));
    }

    @Test
    void givenValidCredentials_whenLoginWithPasswordChangeApiIsCalled_thenRaisedExceptionIsRethrown() throws Exception {
        when(tokens.login(anyString(), any(), any())).thenThrow(new ZaasClientException(SERVICE_UNAVAILABLE));

        assertThrows(ZaasClientException.class, () -> underTest.login(VALID_USERNAME, VALID_PASSWORD, VALID_NEW_PASSWORD));
    }

    @Test
    void givenValidToken_whenLoginApiIsCalled_thenRaisedExceptionIsRethrown() throws Exception {
        when(tokens.login(anyString())).thenThrow(new ZaasClientException(SERVICE_UNAVAILABLE));

        assertThrows(ZaasClientException.class, () -> underTest.login(VALID_TOKEN));
    }

    @Test
    void givenValidToken_whenQueryApiIsCalled_thenRaisedExceptionIsRethrown() throws Exception {
        when(tokens.query(anyString())).thenThrow(new ZaasClientException(SERVICE_UNAVAILABLE));

        assertThrows(ZaasClientException.class, () -> underTest.query(VALID_TOKEN));
    }

    @Test
    void givenValidTokenApplId_whenPassTicketApiIsCalled_thenRaisedClientExceptionIsRethrown() throws Exception {
        when(passTickets.passTicket(anyString(), anyString())).thenThrow(new ZaasClientException(SERVICE_UNAVAILABLE));

        assertThrows(ZaasClientException.class, () -> underTest.passTicket(VALID_TOKEN, VALID_APPLICATION_ID));
    }

    @Test
    void givenInvalidKeyConfiguration_whenPassTicketApiIsCalled_thenRaisedConfigurationExceptionIsRethrown() throws Exception {
        when(passTickets.passTicket(anyString(), anyString())).thenThrow(
            new ZaasConfigurationException(IO_CONFIGURATION_ISSUE)
        );

        assertThrows(ZaasConfigurationException.class, () -> underTest.passTicket(VALID_TOKEN, VALID_APPLICATION_ID));
    }

    @Test
    void givenValidToken_whenLogoutIsCalled_thenSuccessLogout() {
        assertDoesNotThrow(() -> underTest.logout("apimlAuthenticationToken=" + VALID_TOKEN));
    }

    @Test
    void givenNullKeyStorePath_whenTheClientIsConstructed_thenExceptionIsThrown() {
        ConfigProperties config = new ConfigProperties();
        config.setTrustStorePassword(VALID_PASSWORD);
        config.setTrustStorePath("src/test/resources/localhost.truststore.p12");
        config.setTrustStoreType("PKCS12");
        ZaasConfigurationException zaasException = assertThrows(ZaasConfigurationException.class, () -> new ZaasClientImpl(config));

        assertThat(zaasException.getErrorCode().getId(), is("ZWEAS501E"));
    }

    @Test
    void createConfigProperties() {
        DefaultZaasClientConfiguration configuration = new DefaultZaasClientConfiguration();
        ConfigProperties properties = configuration.getConfigProperties();
        assertNotNull(properties);
    }

    @Nested
    class DeprecatedMethods {

        private ConfigProperties configProperties = new ConfigProperties();

        {
            configProperties.setHttpOnly(true);
        }

        @Test
        void whenCallDeprecatedLogin_thenTheNewMethodIsCalledAndTempMemoryErased() throws ZaasClientException, ZaasConfigurationException {
            ZaasClientImpl zaasClient = spy(new ZaasClientImpl(configProperties));
            AtomicReference<char[]> passwordHolder = new AtomicReference<>();
            doAnswer(invocation -> {
                assertEquals("userId", invocation.getArgument(0));
                passwordHolder.set(invocation.getArgument(1));
                assertEquals("password", String.valueOf(passwordHolder.get()));
                return null;
            }).when(zaasClient).login(any(), (char[]) any());

            zaasClient.login("userId", "password");

            assertArrayEquals(new char[passwordHolder.get().length], passwordHolder.get());
        }

        @Test
        void whenCallDeprecatedLoginWithNulls_thenItWorks() throws ZaasClientException, ZaasConfigurationException {
            ZaasClientImpl zaasClient = spy(new ZaasClientImpl(configProperties));
            doAnswer(invocation -> {
                assertNull(invocation.getArgument(0));
                assertNull(invocation.getArgument(1));
                return null;
            }).when(zaasClient).login(any(), (char[]) any());

            zaasClient.login(null, (String) null);
        }

        @Test
        void whenCallDeprecatedLoginToChangePassword_thenTheNewMethodIsCalledAndTempMemoryErased() throws ZaasClientException, ZaasConfigurationException {
            ZaasClientImpl zaasClient = spy(new ZaasClientImpl(configProperties));
            AtomicReference<char[]> passwordHolder = new AtomicReference<>();
            AtomicReference<char[]> newPasswordHolder = new AtomicReference<>();
            doAnswer(invocation -> {
                assertEquals("userId", invocation.getArgument(0));
                passwordHolder.set(invocation.getArgument(1));
                newPasswordHolder.set(invocation.getArgument(2));
                assertEquals("password", String.valueOf(passwordHolder.get()));
                assertEquals("newPassword", String.valueOf(newPasswordHolder.get()));
                return null;
            }).when(zaasClient).login(any(), any(), (char[]) any());

            zaasClient.login("userId", "password", "newPassword");

            assertArrayEquals(new char[passwordHolder.get().length], passwordHolder.get());
            assertArrayEquals(new char[newPasswordHolder.get().length], newPasswordHolder.get());
        }

        @Test
        void whenCallDeprecatedLoginToChangePasswordWithNulls_thenItWorks() throws ZaasClientException, ZaasConfigurationException {
            ZaasClientImpl zaasClient = spy(new ZaasClientImpl(configProperties));
            doAnswer(invocation -> {
                assertNull(invocation.getArgument(0));
                assertNull(invocation.getArgument(1));
                assertNull(invocation.getArgument(2));
                return null;
            }).when(zaasClient).login(any(), any(), (char[]) any());

            zaasClient.login(null, null, (String) null);
        }

    }

}
