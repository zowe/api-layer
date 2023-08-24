/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.security.common.verify;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.*;

@Service
@Slf4j
public class TrustedCertificatesProvider {

    private final Set<String> publicKeyCertificatesBase64;
    private final CloseableHttpClient httpClient;

    @Autowired
    public TrustedCertificatesProvider(@Qualifier("secureHttpClientWithoutKeystore") CloseableHttpClient httpClient,
                                       @Qualifier("publicKeyCertificatesBase64") Set<String> publicKeyCertificatesBase64) {
        this.httpClient = httpClient;
        this.publicKeyCertificatesBase64 = publicKeyCertificatesBase64;
    }

    //    @Cacheable(value = "certificates", key = "#certificatesEndpoint", unless = "#result.isEmpty()")

    public List<Certificate> getTrustedCerts(String certificatesEndpoint) {
        List<Certificate> trustedCerts = new ArrayList<>();
        String pem = callCertificatesEndpoint(certificatesEndpoint);
        if (StringUtils.isNotEmpty(pem)) {
            try {
                Collection<? extends Certificate> certs = CertificateFactory
                    .getInstance("X.509")
                    .generateCertificates(new ByteArrayInputStream(pem.getBytes()));
                trustedCerts.addAll(certs);
                updateTrustedPublicKeys(trustedCerts);
            } catch (CertificateException e) {
                throw new RuntimeException(e);
            }
        }
        return trustedCerts;
    }

    private String callCertificatesEndpoint(String url) {
        try {
            HttpGet httpGet = new HttpGet(new URI(url));
            HttpResponse httpResponse = httpClient.execute(httpGet);
            final int statusCode = httpResponse.getStatusLine() != null ? httpResponse.getStatusLine().getStatusCode() : 0;
            String body = "";
            if (httpResponse.getEntity() != null) {
                body = EntityUtils.toString(httpResponse.getEntity(), StandardCharsets.UTF_8);
            }
            if (statusCode != HttpStatus.SC_OK) {
                log.warn("Unexpected response from {} endpoint. Status: {} body: {}", url, statusCode, body);
                return null;
            }
            log.debug("Trusted certificates from {}: {}", url, body);
            return body;

        } catch (URISyntaxException e) {
            log.warn("Configuration error: Invalid URL specified in {} parameter.", e.getMessage());
        } catch (ClientProtocolException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    private void updateTrustedPublicKeys(List<Certificate> certs) {
        for (Certificate cert : certs) {
            String publicKey = Base64.getEncoder().encodeToString(cert.getPublicKey().getEncoded());
            publicKeyCertificatesBase64.add(publicKey);
        }
    }
}
