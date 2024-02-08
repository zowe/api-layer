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

import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Objects;

public class ZaasClientImpl implements ZaasClient {
    private final TokenService tokens;
    private final PassTicketService passTickets;

    public ZaasClientImpl(ConfigProperties configProperties) throws ZaasConfigurationException {
        if (!configProperties.isHttpOnly() && (configProperties.getKeyStorePath() == null)) {
            throw new ZaasConfigurationException(ZaasConfigurationErrorCodes.KEY_STORE_NOT_PROVIDED);
        }

        CloseableClientProvider httpClientProvider = getTokenProvider(configProperties);
        CloseableClientProvider httpClientProviderWithoutCert = getTokenProviderWithoutCert(configProperties, httpClientProvider);

        String baseUrl = String.format("%s://%s:%s%s", getScheme(configProperties.isHttpOnly()), configProperties.getApimlHost(), configProperties.getApimlPort(),
            configProperties.getApimlBaseUrl());
        tokens = new ZaasJwtService(httpClientProviderWithoutCert, baseUrl, configProperties);
        passTickets = new PassTicketServiceImpl(httpClientProvider, baseUrl, configProperties);
    }

    private CloseableClientProvider getTokenProvider(ConfigProperties configProperties) throws ZaasConfigurationException {
        if (configProperties.isHttpOnly()) {
            return new ZaasHttpClientProvider();
        } else {
            return new ZaasHttpsClientProvider(configProperties);
        }
    }

    private CloseableClientProvider getTokenProviderWithoutCert (
        ConfigProperties configProperties,
        CloseableClientProvider defaultCloseableClientProvider) throws ZaasConfigurationException
    {
        if (configProperties.isHttpOnly()) {
            return defaultCloseableClientProvider;
        }
        return getTokenProvider(configProperties.withoutKeyStore());
    }

    private Object getScheme(boolean httpOnly) {
        if (httpOnly) {
            return "http";
        } else {
            return "https";
        }
    }

    ZaasClientImpl(TokenService tokens, PassTicketService passTickets) {
        this.tokens = tokens;
        this.passTickets = passTickets;
    }

    @Override
    public String login(String userId, String password, String newPassword) throws ZaasClientException {
        char[] passwordChars = password == null ? null : password.toCharArray();
        char[] newPasswordChars = newPassword == null ? null : newPassword.toCharArray();
        try {
            return login(userId, passwordChars, newPasswordChars);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, (char) 0);
            }
            if (newPasswordChars != null) {
                Arrays.fill(newPasswordChars, (char) 0);
            }
        }
    }

    @Override
    public String login(String userId, String password) throws ZaasClientException {
        char[] passwordChars = password == null ? null : password.toCharArray();
        try {
            return login(userId, passwordChars);
        } finally {
            if (passwordChars != null) {
                Arrays.fill(passwordChars, (char) 0);
            }
        }
    }

    @Override
    public String login(String userId, char[] password, char[] newPassword) throws ZaasClientException {
        if (userId == null || password == null || newPassword == null || userId.isEmpty() || password.length == 0 || newPassword.length == 0) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);
        }
        return tokens.login(userId, password, newPassword);
    }

    @Override
    public String login(String userId, char[] password) throws ZaasClientException {
        if (userId == null || password == null || userId.isEmpty() || password.length == 0) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);
        }
        return tokens.login(userId, password);
    }

    @Override
    public String login(String authorizationHeader) throws ZaasClientException {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER);
        }
        return tokens.login(authorizationHeader);

    }

    @Override
    public ZaasToken query(String token) throws ZaasClientException {
        return tokens.query(token);
    }

    @Override
    public ZaasToken query(HttpServletRequest request) throws ZaasClientException {
        return tokens.query(request);
    }

    @Override
    @SuppressWarnings("squid:S2147")
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException, ZaasConfigurationException {
        if (Objects.isNull(applicationId) || applicationId.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.APPLICATION_NAME_NOT_FOUND);
        }
        if (Objects.isNull(jwtToken) || jwtToken.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED);
        }
        return passTickets.passTicket(jwtToken, applicationId);
    }

    @Override
    public void logout(String jwtToken) throws ZaasConfigurationException, ZaasClientException {
        tokens.logout(jwtToken);
    }
}
