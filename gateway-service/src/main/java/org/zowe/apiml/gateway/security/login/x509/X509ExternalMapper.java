/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.x509;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicHeader;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;
import org.zowe.apiml.gateway.security.login.x509.model.CertMapperResponse;
import org.zowe.apiml.gateway.security.service.TokenCreationService;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

/**
 * Certificate mapper that allows to return user id of the provided x509 certificate
 * This mapper will be executed when ZSS is used
 */

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!T(org.springframework.util.StringUtils).isEmpty('${apiml.security.x509.externalMapperUrl}')"
)
public class X509ExternalMapper extends X509AbstractMapper {

    private final CloseableHttpClient httpClientProxy;
    private final TokenCreationService tokenCreationService;

    @Value("${apiml.security.x509.externalMapperUrl}")
    private String externalMapperUrl;
    @Value("${apiml.security.x509.externalMapperUser}")
    private String externalMapperUser;

    /**
     * Maps certificate to the mainframe user id.
     *
     * @param certificate Certificate to get mapping for.
     * @return the user id or null if there is either no mapping or problem with certificate
     * @throws URISyntaxException           if the certificate mapping URL is wrong
     * @throws CertificateEncodingException when it cannot get encoded data from certificate
     * @throws IOException                  if not able to send certificate to the mapper
     */
    @Override
    public String mapCertificateToMainframeUserId(X509Certificate certificate) {
        if (isClientAuthCertificate(certificate)) {
            try {
                String jwtToken = tokenCreationService.createJwtTokenWithoutCredentials(externalMapperUser);

                log.error("JWT for call to external mapper: {}",jwtToken);
                HttpPost httpPost = new HttpPost(new URI(externalMapperUrl));
                HttpEntity httpEntity = new ByteArrayEntity(certificate.getEncoded());
                httpPost.setEntity(httpEntity);

                httpPost.setHeader(new BasicHeader("Cookie", "apimlAuthenticationToken=" + jwtToken));

                HttpResponse httpResponse = httpClientProxy.execute(httpPost);
                String response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
                log.error("User ID: {}", response);
                if (response == null || response.isEmpty()) {
                    return null;
                }

                ObjectMapper objectMapper = new ObjectMapper();
                CertMapperResponse certMapperResponse = objectMapper.readValue(response, CertMapperResponse.class);
                return certMapperResponse.getUserId().trim();
            } catch (URISyntaxException e) {
                log.error("Wrong service URI provided", e);
            } catch (CertificateEncodingException e) {
                log.error("Can`t get encoded data from certificate", e);
            } catch (IOException e) {
                log.error("Not able to send certificate to mapper", e);
            }
            return null;
        }
        return null;
    }

}


