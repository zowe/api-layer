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

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.zowe.apiml.zaasclient.client.HttpsClient;
import org.zowe.apiml.zaasclient.exception.ZaasClientErrorCodes;
import org.zowe.apiml.zaasclient.exception.ZaasClientException;
import org.zowe.apiml.zaasclient.passticket.ZaasClientTicketRequest;
import org.zowe.apiml.zaasclient.passticket.ZaasPassTicketResponse;

import java.io.IOException;

@Slf4j
public class PassTicketServiceHttps implements PassTicketService {
    private static final String COOKIE_PREFIX = "apimlAuthenticationToken";

    private HttpsClient httpsClient;
    private final String ticketUrl;

    public PassTicketServiceHttps(HttpsClient client, String baseUrl) {
        this.httpsClient = client;

        ticketUrl = baseUrl + "/ticket";
    }

    @Override
    public String passTicket(String jwtToken, String applicationId) throws ZaasClientException {
        CloseableHttpResponse response = null;
        try {
            CloseableHttpClient closeableHttpsClient = httpsClient.getHttpsClientWithKeyStoreAndTrustStore();
            ZaasClientTicketRequest zaasClientTicketRequest = new ZaasClientTicketRequest();
            ObjectMapper mapper = new ObjectMapper();
            zaasClientTicketRequest.setApplicationName(applicationId);

            HttpPost httpPost = new HttpPost(ticketUrl);
            httpPost.setEntity(new StringEntity(mapper.writeValueAsString(zaasClientTicketRequest)));
            httpPost.setHeader("Content-type", "application/json");
            httpPost.setHeader("Cookie", COOKIE_PREFIX + "=" + jwtToken);

            response = closeableHttpsClient.execute(httpPost);
            return extractPassTicket(response);
        } catch (Exception ioe) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE, ioe.getMessage());
        } finally {
            finallyClose(response);
        }
    }

    private String extractPassTicket(CloseableHttpResponse response) throws IOException, ZaasClientException {
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode == 200) {
            ObjectMapper mapper = new ObjectMapper();
            ZaasPassTicketResponse zaasPassTicketResponse = mapper
                .readValue(response.getEntity().getContent(), ZaasPassTicketResponse.class);
            return zaasPassTicketResponse.getTicket();
        } else if (statusCode == 401) {
            throw new ZaasClientException(ZaasClientErrorCodes.INVALID_AUTHENTICATION);
        } else if (statusCode == 400) {
            throw new ZaasClientException(ZaasClientErrorCodes.BAD_REQUEST);
        } else if (statusCode == 500) {
            throw new ZaasClientException(ZaasClientErrorCodes.SERVICE_UNAVAILABLE);
        } else {
            throw new ZaasClientException(ZaasClientErrorCodes.GENERIC_EXCEPTION);
        }
    }

    private void finallyClose(CloseableHttpResponse response) {
        try {
            if (response != null)
                response.close();
            if (httpsClient != null)
                httpsClient.close();
        } catch (IOException e) {
            log.warn("It wasn't possible to close the resources. " + e.getMessage());
        }
    }
}
