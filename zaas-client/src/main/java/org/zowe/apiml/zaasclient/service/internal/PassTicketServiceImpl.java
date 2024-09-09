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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.zowe.apiml.zaasclient.config.ConfigProperties;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.passticket.ZaasClientTicketRequest;
import org.zowe.apiml.zaasclient.passticket.ZaasPassTicketResponse;
import org.zowe.apiml.zaasclient.util.SimpleHttpResponse;

import java.io.IOException;

class PassTicketServiceImpl implements PassTicketService {

    private final CloseableHttpClient httpClient;
    private final String ticketUrl;

    ConfigProperties passConfigProperties;

    public PassTicketServiceImpl(CloseableHttpClient client, String baseUrl, ConfigProperties configProperties) {
        httpClient = client;
        ticketUrl = baseUrl + "/ticket";
        passConfigProperties = configProperties;
    }

    @Override
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException {
        try {
            HttpPost httpPost = getHttpPost(jwtToken, applicationId);
            var passTicketResponse = httpClient.execute(httpPost, SimpleHttpResponse::fromResponseWithBytesBodyOnSuccess);
            return extractPassTicket(passTicketResponse);
        } catch (ZaasClientException e) {
            throw e;
        } catch (Exception e) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, e);
        }
    }

    private HttpPost getHttpPost(String jwtToken, String applicationId) throws JsonProcessingException {
        var zaasClientTicketRequest = new ZaasClientTicketRequest();
        var mapper = new ObjectMapper();
        zaasClientTicketRequest.setApplicationName(applicationId);

        var httpPost = new HttpPost(ticketUrl);
        httpPost.setEntity(new StringEntity(mapper.writeValueAsString(zaasClientTicketRequest)));
        httpPost.setHeader(HttpHeaders.CONTENT_TYPE, "application/json");
        httpPost.setHeader(HttpHeaders.COOKIE, passConfigProperties.getTokenPrefix() + "=" + jwtToken);
        return httpPost;
    }

    private String extractPassTicket(SimpleHttpResponse response) throws IOException, ZaasClientException {
        int statusCode = response.getCode();
        if (statusCode == 200) {
            ObjectMapper mapper = new ObjectMapper();
            ZaasPassTicketResponse zaasPassTicketResponse = mapper
                .readValue(response.getByteBody(), ZaasPassTicketResponse.class);
            return zaasPassTicketResponse.getTicket();
        } else {
            String obtainedMessage = response.getStringBody();
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
