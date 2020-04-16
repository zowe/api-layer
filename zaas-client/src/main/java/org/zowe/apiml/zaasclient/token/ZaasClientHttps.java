/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.zaasclient.token;

import lombok.extern.slf4j.Slf4j;
import org.zowe.apiml.zaasclient.client.HttpsClient;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;

import java.util.Objects;

@Slf4j
public class ZaasClientHttps implements ZaasClient {
    private TokenService tokens;
    private PassTicketService passTickets;

    public ZaasClientHttps(HttpsClient client, ConfigProperties configProperties) {
        String baseUrl = String.format("https://%s:%s%s", configProperties.getApimlHost(), configProperties.getApimlPort(),
            configProperties.getApimlBaseUrl());

        tokens = new TokenServiceHttpsJwt(client, baseUrl, configProperties.getApimlHost());
        passTickets = new PassTicketServiceHttps(client, baseUrl);
    }

    public ZaasClientHttps(ConfigProperties configProperties) {
        this(new HttpsClient(configProperties), configProperties);
    }

    @Override
    public String login(String userId, String password) throws ZaasClientException {
        if (userId == null || password == null) {
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
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException {
        if (Objects.isNull(applicationId) || applicationId.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.APPLICATION_NAME_NOT_FOUND);
        }
        if (Objects.isNull(jwtToken) || jwtToken.isEmpty()) {
            throw new ZaasClientException(ZaasClientErrorCodes.TOKEN_NOT_PROVIDED);
        }

        return passTickets.passTicket(jwtToken, applicationId);
    }
}
