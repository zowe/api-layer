/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.zaas.security.service.token;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.zowe.apiml.constants.ApimlConstants;
import org.zowe.apiml.security.common.token.OIDCProvider;

import java.io.IOException;

@RequiredArgsConstructor
@Service
@Slf4j
@ConditionalOnExpression("'${apiml.security.oidc.validationType:JWK}' == 'endpoint' && '${apiml.security.oidc.enabled:false}' == 'true'")
public class OIDCTokenProviderEndpoint implements OIDCProvider {

    @Value("${apiml.security.oidc.userInfo.uri}")
    private String endpointUrl;

    private final CloseableHttpClient secureHttpClientWithKeystore;

    @Override
    public boolean isValid(String token) {
        try {
            HttpGet httpGet = new HttpGet(endpointUrl);
            httpGet.addHeader(HttpHeaders.AUTHORIZATION, ApimlConstants.BEARER_AUTHENTICATION_PREFIX + " " + token);

            HttpResponse httpResponse = secureHttpClientWithKeystore.execute(httpGet);

            int responseCode = httpResponse.getStatusLine().getStatusCode();
            return HttpStatus.valueOf(responseCode).is2xxSuccessful();
        } catch (IOException e) {
            log.error("An error occurred during validation of OIDC token using userInfo URI {}: {}", endpointUrl, e.getMessage());
            return false;
        }
    }

}
