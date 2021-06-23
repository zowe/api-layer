/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.product.tomcat;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

import static org.junit.jupiter.api.Assertions.*;

class ApimlTomcatCustomizerTest {

    @Test
    void parseCertificateFromPem () throws Exception{
        String pemCert = "-----BEGIN CERTIFICATE-----\n" +
            " MIIHZjCCBk6gAwIBAgIQBhv8eupzzw6gJyKT1TpwQjANBgkqhkiG9w0BAQsFADBNMQswCQYDVQQGEwJVUzEVMBMGA1UEChMMRGlnaUNlcnQgSW5jMScwJQYDVQQDEx5EaWdp\n" +
            " Q2VydCBTSEEyIFNlY3VyZSBTZXJ2ZXIgQ0EwHhcNMTkwOTI2MDAwMDAwWhcNMjEwOTI1MTIwMDAwWjB9MQswCQYDVQQGEwJVUzETMBEGA1UECBMKQ2FsaWZvcm5pYTERMA8G\n" +
            " A1UEBxMIU2FuIEpvc2UxFTATBgNVBAoTDEJyb2FkY29tIEluYzELMAkGA1UECxMCSVQxIjAgBgNVBAMTGXVzaWxjYTMyLmx2bi5icm9hZGNvbS5uZXQwggEiMA0GCSqGSIb3\n" +
            " DQEBAQUAA4IBDwAwggEKAoIBAQCgeO4HCH5f19WqnWa9UTJ/YX3QrgXXZUz2AwEcxRi8EmYQFgM2Kw8Vbxy9D8mU0HtCxqs7GNgfB/62wMDxWUX3ecrghCuKp3ldFpNNppEQ\n" +
            " tb0+ZsHWd48KsK3P1HXPkWg81Py5vIGUnOK7rdxiBDXR3M9Yi4hXybymQPMjuZR2vRqvLmtSjKRHmW4xe7ywmADGaQt4ct0bvONfGr+oB7iN44gmmJSDo263ksShfuCQVHIl\n" +
            " 9E7HumxezzSXUJpI+VlHyR226fPqlULYFhWOmE6YWn4/8q3sVxqF3oCX+8wuP9y/diQbPHE+3ZNB7khVTm+CcSinJZjPpVkvJ+pjGErVAgMBAAGjggQQMIIEDDAfBgNVHSME\n" +
            " GDAWgBQPgGEcgjFh1S8o541GOLQs4cbZ4jAdBgNVHQ4EFgQUF8WDN8YhBliwDXsor4LbIwaKgbMwgdQGA1UdEQSBzDCByYIZdXNpbGNhMzIubHZuLmJyb2FkY29tLm5ldIIV\n" +
            " Y2EzMi5sdm4uYnJvYWRjb20ubmV0ghl1c2lsY2EzMS5sdm4uYnJvYWRjb20ubmV0ghVjYTMxLmx2bi5icm9hZGNvbS5uZXSCGXVzaWxjYTN4Lmx2bi5icm9hZGNvbS5uZXSC\n" +
            " FWNhM3gubHZuLmJyb2FkY29tLm5ldIIWdHNvNjEubHZuLmJyb2FkY29tLm5ldIIZdXNpMTYxbWUubHZuLmJyb2FkY29tLm5ldDAOBgNVHQ8BAf8EBAMCBaAwHQYDVR0lBBYw\n" +
            " FAYIKwYBBQUHAwEGCCsGAQUFBwMCMGsGA1UdHwRkMGIwL6AtoCuGKWh0dHA6Ly9jcmwzLmRpZ2ljZXJ0LmNvbS9zc2NhLXNoYTItZzYuY3JsMC+gLaArhilodHRwOi8vY3Js\n" +
            " NC5kaWdpY2VydC5jb20vc3NjYS1zaGEyLWc2LmNybDBMBgNVHSAERTBDMDcGCWCGSAGG/WwBATAqMCgGCCsGAQUFBwIBFhxodHRwczovL3d3dy5kaWdpY2VydC5jb20vQ1BT\n" +
            " MAgGBmeBDAECAjB8BggrBgEFBQcBAQRwMG4wJAYIKwYBBQUHMAGGGGh0dHA6Ly9vY3NwLmRpZ2ljZXJ0LmNvbTBGBggrBgEFBQcwAoY6aHR0cDovL2NhY2VydHMuZGlnaWNl\n" +
            " cnQuY29tL0RpZ2lDZXJ0U0hBMlNlY3VyZVNlcnZlckNBLmNydDAJBgNVHRMEAjAAMIIBfgYKKwYBBAHWeQIEAgSCAW4EggFqAWgAdgC72d+8H4pxtZOUI5eqkntHOFeVCqtS\n" +
            " 6BqQlmQ2jh7RhQAAAW1t4HDSAAAEAwBHMEUCIBNtt6wsxMzaC6+91gVtG5rG2zJXgf7RGbYxUlW2GxFyAiEAlUg+33tAP0bY7mIgMT1OZPnZGgcVL7/GiFvDHlPOBJAAdwCH\n" +
            " db/nWXz4jEOZX73zbv9WjUdWNv9KtWDBtOr/XqCDDwAAAW1t4HElAAAEAwBIMEYCIQCjoqJ/H2t0EU8qeG6YIw+3DlYY+8CbdWneWWY2hXfdaQIhAPMNGN7lU/fHWXBN4vz2\n" +
            " WJ0npBmihYswYp/8UKBwUDPgAHUARJRlLrDuzq/EQAfYqP4owNrmgr7YyzG1P9MzlrW2gagAAAFtbeBwJwAABAMARjBEAiB4GxEm+RSowlqjBwsBfGTuorGaHQD8rkEAGNsW\n" +
            " ayyz3wIgRN4sFVmIeJQ2yHaCp86WTu6+sTrrMORm+uXiyboq78owDQYJKoZIhvcNAQELBQADggEBAJx4yCQoVtjHo4Omej09Uha1ZY19R7lAceg60AR9LQxN61SjmVXRPcQO\n" +
            " Q6x4d/irbtns64WiV9ATMqunEyx0djdFVvdxUKGdsniWAHlRkGbWOi1LUt6jNRoHhxa2hZWihk8ayxukapceaKnOwTZ7UQkQFNQFKQeziDG3AOZXXrzwiULV/VwAaKpVI5ED\n" +
            " HyCoPCVeripvLTDiegpzWu/OkiQvdBbSy7YeqqMJ3jEqIVMi/N/o8nTi4AcsQ8yiBGZC51oeZ085e5kZuVQxUlWxV810n8O/jL0wpsj4h7IZ/eEBHl8szOlOTogDEREml/zQ\n" +
            " e5QJti778D8pUmSvByQKSAE=" +
            "-----END CERTIFICATE-----";
        X509Certificate certificate = (X509Certificate) CertificateFactory
            .getInstance("X509")
            .generateCertificate(new ByteArrayInputStream(pemCert.getBytes()));
        assertEquals("CN=usilca32.lvn.broadcom.net, OU=IT, O=Broadcom Inc, L=San Jose, ST=California, C=US",certificate.getSubjectDN().toString());
    }
}
