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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;

import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes.*;
import static org.zowe.apiml.zaasclient.exception.ZaasConfigurationErrorCodes.IO_CONFIGURATION_ISSUE;

class ZaasClientTest {
    private ZaasClient underTest;
    private TokenService tokens;
    private PassTicketService passTickets;

    private static final String VALID_PASSWORD = "password";
    private static final String VALID_USERNAME = "username";
    private static final String VALID_APPLICATION_ID = "APPLID";
    private static final String VALID_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c";

    @BeforeEach
    void setUp() {
        tokens = mock(TokenService.class);
        passTickets = mock(PassTicketService.class);

        underTest = new ZaasClientImpl(tokens, passTickets);
    }

    private void assertThatExceptionContainValidCode(ZaasClientException zce, ZaasClientErrorCodes code) {
        ZaasClientErrorCodes producedErrorCode = zce.getErrorCode();
        assertThat(code.getId(), is(producedErrorCode.getId()));
        assertThat(code.getMessage(), is(producedErrorCode.getMessage()));
        assertThat(code.getReturnCode(), is(producedErrorCode.getReturnCode()));
    }

    private static Stream<Arguments> provideInvalidUsernamePassword() {
        return Stream.of(
            Arguments.of(null, VALID_PASSWORD),
            Arguments.of("", VALID_PASSWORD),
            Arguments.of(VALID_USERNAME, null),
            Arguments.of(VALID_USERNAME, "")
        );
    }

    @ParameterizedTest
    @MethodSource("provideInvalidUsernamePassword")
    void givenFullyInvalidCredentials_whenLoggingIn_thenExceptionIsRaised(String username, String password) {
        ZaasClientException exception = assertThrows(ZaasClientException.class, () -> underTest.login(username, password));

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
        when(tokens.login(anyString(), anyString())).thenThrow(new ZaasClientException(SERVICE_UNAVAILABLE));

        assertThrows(ZaasClientException.class, () -> underTest.login(VALID_USERNAME, VALID_PASSWORD));
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

}
