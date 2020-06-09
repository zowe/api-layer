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
public class ZaasClientHttps implements ZaasClient {
    private TokenService tokens;
    private PassTicketService passTickets;

    public ZaasClientHttps(ConfigProperties configProperties) throws ZaasConfigurationException {
        try {
            HttpsClientProvider provider = new HttpsClientProvider(configProperties);
            String baseUrl = String.format("https://%s:%s%s", configProperties.getApimlHost(), configProperties.getApimlPort(),
                configProperties.getApimlBaseUrl());
            tokens = new TokenServiceHttpsJwt(provider, baseUrl, configProperties.getApimlHost());
            passTickets = new PassTicketServiceHttps(provider, baseUrl);
        } catch (ZaasConfigurationException e) {
            log.error(e.getErrorCode().toString());
            throw e;
        }
    }

    ZaasClientHttps(TokenService tokens, PassTicketService passTickets) {
        this.tokens = tokens;
        this.passTickets = passTickets;
    }

    @Override
    public String login(String userId, String password) throws ZaasClientException {
        if (userId == null || password == null || userId.isEmpty() || password.isEmpty()) {
            log.error(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD.toString());
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_USERNAME_PASSWORD);
        }

        try {
            return tokens.login(userId, password);
        } catch (ZaasClientException e) {
            log.error(e.getErrorCode().toString());
            throw e;
        }
    }

    @Override
    public String login(String authorizationHeader) throws ZaasClientException {
        if (authorizationHeader == null || authorizationHeader.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.EMPTY_NULL_AUTHORIZATION_HEADER);
        }

        try {
            return tokens.login(authorizationHeader);
        } catch (ZaasClientException e) {
            log.error(e.getErrorCode().toString());
            throw e;
        }
    }

    @Override
    public ZaasToken query(String token) throws ZaasClientException {
        try {
            return tokens.query(token);
        } catch (ZaasClientException e) {
            log.error(e.getErrorCode().toString());
            throw e;
        }
    }

    @Override
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException, ZaasConfigurationException {
        if (Objects.isNull(applicationId) || applicationId.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.APPLICATION_NAME_NOT_FOUND);
        }
        if (Objects.isNull(jwtToken) || jwtToken.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED);
        }

        try {
            return passTickets.passTicket(jwtToken, applicationId);
        } catch (ZaasClientException e) {
            log.error(e.getErrorCode().toString());
            throw e;
        } catch (ZaasConfigurationException e) {
            log.error(e.getErrorCode().toString());
            throw e;
        }
    }
}
