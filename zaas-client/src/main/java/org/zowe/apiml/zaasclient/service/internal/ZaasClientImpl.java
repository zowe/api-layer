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

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.service.ZaasClient;
import org.zowe.apiml.zaasclient.service.ZaasToken;

import java.util.Objects;

@Slf4j
public class ZaasClientImpl implements ZaasClient {
    private final TokenService tokens;
    private final PassTicketService passTickets;

    public ZaasClientImpl(ConfigProperties configProperties) throws ZaasConfigurationException {
            CloseableClientProvider httpClientProvider = getTokenProvider(configProperties);
            String baseUrl = String.format("%s://%s:%s%s", getScheme(configProperties.isHttpOnly()), configProperties.getApimlHost(), configProperties.getApimlPort(),
                configProperties.getApimlBaseUrl());
            tokens = new ZaasJwtService(httpClientProvider, baseUrl);
            passTickets = new PassTicketServiceImpl(httpClientProvider, baseUrl);

    }

    private CloseableClientProvider getTokenProvider(ConfigProperties configProperties) throws ZaasConfigurationException {
        if (configProperties.isHttpOnly()) {
            return new ZaasHttpClientProvider();
        } else {
            return new ZaasHttpsClientProvider(configProperties);
        }

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
    public String login(String userId, String password) throws ZaasClientException {
        if (userId == null || password == null || userId.isEmpty() || password.isEmpty()) {
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
