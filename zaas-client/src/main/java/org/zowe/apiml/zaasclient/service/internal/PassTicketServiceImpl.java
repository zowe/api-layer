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
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.exception.ZaasConfigurationException;
import org.zowe.apiml.zaasclient.passticket.ZaasClientTicketRequest;
import org.zowe.apiml.zaasclient.passticket.ZaasPassTicketResponse;

import java.io.IOException;
import org.apache.http.HttpHeaders;
import org.apache.http.cookie.SM;

@Slf4j
class PassTicketServiceImpl implements PassTicketService {
    private static final String TOKEN_PREFIX = "apimlAuthenticationToken";

    private final CloseableClientProvider httpClientProvider;
    private final String ticketUrl;

    public PassTicketServiceImpl(CloseableClientProvider client, String baseUrl) {
        httpClientProvider = client;
        ticketUrl = baseUrl + "/ticket";
    }

    @Override
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException, ZaasConfigurationException {
        try (CloseableHttpClient closeableHttpsClient = httpClientProvider.getHttpClient()) {
            ZaasClientTicketRequest zaasClientTicketRequest = new ZaasClientTicketRequest();
            ObjectMapper mapper = new ObjectMapper();
            zaasClientTicketRequest.setApplicationName(applicationId);

            HttpPost httpPost = new HttpPost(ticketUrl);
            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(zaasClientTicketRequest)));
            httpPost.setHeader("Content-Type", "application/json");
            httpPost.setHeader("Cookie", TOKEN_PREFIX + "=" + jwtToken);

            CloseableHttpResponse response = closeableHttpsClient.execute(httpPost);
            return extractPassTicket(response);
        } catch (ZaasConfigurationException e) {
            throw e;
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e);
        }
    }

    private String extractPassTicket(CloseableHttpResponse response) throws IOException, ZaasClientException {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            ObjectMapper mapper = new ObjectMapper();
            ZaasPassTicketResponse zaasPassTicketResponse = mapper
                .readValue(response.getEntity().getContent(), ZaasPassTicketResponse.class);
            return zaasPassTicketResponse.getTicket();
        } else {
            String obtainedMessage = EntityUtils.toString(response.getEntity());
            if (statusCode == 401) {
                throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION, obtainedMessage);
            } else if (statusCode == 400) {
                throw new ZaasClientException(ZaasClientErrorCodes.BAD_REQUEST, obtainedMessage);
            } else if (statusCode == 500) {
                throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, obtainedMessage);
            } else {
                throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION, obtainedMessage);
            }
        }
    }
}
