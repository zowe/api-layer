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
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.core5.http.HttpStatus;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.product.logging.annotations.InjectApimlLogger;
import org.zowe.apiml.security.common.error.InvalidCertificateException;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class TrustedCertificatesProvider {

    private static final String BEGIN_CERT = "-----BEGIN CERTIFICATE-----";
    private static final String END_CERT = "-----END CERTIFICATE-----";

    private final CloseableHttpClient httpClient;

    @InjectApimlLogger
    private final ApimlLogger apimlLog = ApimlLogger.empty();

    public TrustedCertificatesProvider(@Qualifier("secureHttpClientWithoutKeystore") CloseableHttpClient httpClient) {
        this.httpClient = httpClient;
    }

    /**
     * Query given rest endpoint to get the certificate chain from remote proxy gateway.
     * The endpoint should be publicly available and should provide the certificate chain in PEM format.
     *
     * @param certificatesEndpoint Given full URL to the remote proxy gateway certificates endpoint
     * @return List of certificates or empty list
     */
    @Cacheable(value = "trustedCertificates", unless = "#result.isEmpty()")
    public List<Certificate> getTrustedCerts(String certificatesEndpoint) {
        List<Certificate> trustedCerts = new ArrayList<>();
        var pem = callCertificatesEndpoint(certificatesEndpoint);
        if (StringUtils.isNotEmpty(pem)) {
            try {
                var splitCertsB64 = splitCerts(pem);
                log.debug("Parsed {} certificates", splitCertsB64.size());
                splitCertsB64.forEach(certB64 -> {
                    try {
                        var genCerts = CertificateFactory
                            .getInstance("X.509")
                            .generateCertificates(new ByteArrayInputStream(certB64.getBytes()));
                        trustedCerts.addAll(genCerts);
                    } catch (CertificateException e) {
                        throw new InvalidCertificateException(e.getMessage());
                    }
                });
            } catch (Exception e) {
                apimlLog.log("org.zowe.apiml.security.common.verify.errorParsingCertificates", certificatesEndpoint, e.getMessage());
            }
        } else {
            log.debug("Empty list of trusted certificates");
        }
        return trustedCerts;
    }

    private List<String> splitCerts(String pem) throws IOException {
        String line = null;
        List<String> certs = new ArrayList<>();

        var builder = new StringBuilder();
        var certBufferedReader = new BufferedReader(new StringReader(pem));

        while ((line = certBufferedReader.readLine()) != null) {
            if (line.equals(BEGIN_CERT)) {
                builder.append(line).append("\n");
                try {
                    while ((line = certBufferedReader.readLine()) != null) {
                        builder.append(line);
                        if (line.equals(END_CERT)) {
                            certs.add(builder.toString());
                            builder = new StringBuilder();
                            break;
                        } else {
                            builder.append("\n");
                        }
                    }
                } catch (IOException ioe2) {
                    throw new IOException("Unable to parse Certificate: " + ioe2.getMessage());
                }
            } else {
                throw new IOException("Certificate is not RFC1421 hex-encoded DER bytes");
            }
        }

        return certs;
    }

    private String callCertificatesEndpoint(String url) {
        try {
            HttpGet httpGet = new HttpGet(new URI(url));
            return httpClient.execute(httpGet, response -> {
                final int statusCode = response.getCode();
                String body = "";
                if (response.getEntity() != null) {
                    body = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                }
                if (statusCode != HttpStatus.SC_OK) {
                    apimlLog.log("org.zowe.apiml.security.common.verify.invalidResponse", url, statusCode, body);
                    return null;
                }
                log.debug("Trusted certificates from {}: {}", url, body);
                return body;
            });
        } catch (URISyntaxException e) {
            apimlLog.log("org.zowe.apiml.security.common.verify.invalidURL", url, e.getMessage());
        } catch (IOException e) {
            apimlLog.log("org.zowe.apiml.security.common.verify.httpError", url, e.getMessage());
        }
        return null;
    }

}
