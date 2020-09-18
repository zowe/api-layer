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

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

@AllArgsConstructor
@Slf4j
public class X509ExternalMapper implements X509AuthenticationMapper {

    private CloseableHttpClient httpClientProxy;

    @Override
    public String mapCertificateToMainframeUserId(X509Certificate certificate) {

        try {
            HttpPost httpPost = new HttpPost(new URI("http://localhost:8542/usermap"));
            HttpEntity httpEntity = null;
            try {
                httpEntity = new ByteArrayEntity(certificate.getEncoded());
            } catch (CertificateEncodingException e) {
                log.error("Can`t get encoded data from certificate", e);
            }
            httpPost.setEntity(httpEntity);
            try {
                HttpResponse httpResponse = httpClientProxy.execute(httpPost);
                return EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                log.error("Not able to send certificate to mapper", e);
            }
        }catch (URISyntaxException e) {
            log.error("Wrong URI provided",e);
        }
        return null;
    }
}
