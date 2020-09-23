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
import org.apache.http.util.EntityUtils;
import org.zowe.apiml.gateway.security.login.x509.model.CertMapperResponse;

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
@RequiredArgsConstructor
@Slf4j
public class X509ExternalMapper implements X509AuthenticationMapper {

    private final CloseableHttpClient httpClientProxy;
    private final String externalMapperUrl;

    /**
     * Maps certificate to the mainframe user id
     *
     * @param certificate
     * @return the user
     * @throws URISyntaxException           if the certificate mapping URL is wrong
     * @throws CertificateEncodingException when it cannot get encoded data from certificate
     * @throws IOException                  if not able to send certificate to the mapper
     */
    @Override
    public String mapCertificateToMainframeUserId(X509Certificate certificate) {

        try {
            HttpPost httpPost = new HttpPost(new URI(externalMapperUrl));
            HttpEntity httpEntity = new ByteArrayEntity(certificate.getEncoded());
            httpPost.setEntity(httpEntity);
            HttpResponse httpResponse = httpClientProxy.execute(httpPost);
            String response = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            log.error("Mapper response, {}", response);
            ObjectMapper objectMapper = new ObjectMapper();
            CertMapperResponse certMapperResponse = objectMapper.readValue(response, CertMapperResponse.class);
            if (certMapperResponse.getUserId() != null && !"CERTAUTH".equalsIgnoreCase(certMapperResponse.getUserId())) {
                return certMapperResponse.getUserId().trim();
            }
            return null;
        } catch (URISyntaxException e) {
            log.error("Wrong URI provided", e);
        } catch (CertificateEncodingException e) {
            log.error("Can`t get encoded data from certificate", e);
        } catch (IOException e) {
            log.error("Not able to send certificate to mapper", e);
        }
        return null;
    }
}


