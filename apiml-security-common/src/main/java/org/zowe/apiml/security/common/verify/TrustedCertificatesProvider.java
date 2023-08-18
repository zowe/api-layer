package org.zowe.apiml.security.common.verify;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayInputStream;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Collections;

@Service
@Slf4j
public class TrustedCertificatesProvider {

    final RestTemplate restTemplate;

    @Autowired
    public TrustedCertificatesProvider(@Qualifier("restTemplateWithoutKeystore") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
//    @Cacheable(value = "certificates", key = "#certificatesEndpoint", unless = "#result.isEmpty()")
    public Collection<X509Certificate> getTrustedCerts(String certificatesEndpoint) {
        Collection<? extends Certificate> certs = Collections.emptySet();
        ResponseEntity<String> responseEntity =
                restTemplate.exchange(certificatesEndpoint, HttpMethod.GET, null, String.class);
        if (responseEntity.getStatusCode().is2xxSuccessful()) {
            String pem = responseEntity.getBody();
            if (StringUtils.isNotEmpty(pem)) {
                try {
                    certs = CertificateFactory
                        .getInstance("X509")
                        .generateCertificates(new ByteArrayInputStream(pem.getBytes()));
                } catch (CertificateException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return (Collection<X509Certificate>) certs;
    }
}
