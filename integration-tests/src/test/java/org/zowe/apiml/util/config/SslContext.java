/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.util.config;

import io.restassured.config.RestAssuredConfig;
import io.restassured.config.SSLConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.ssl.*;
import org.springframework.util.ResourceUtils;

import javax.net.ssl.SSLContext;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.Socket;
import java.security.KeyStore;
import java.security.cert.*;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
@Slf4j
public class SslContext {

    public static final char[] KEYSTORE_PASSWORD = ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStorePassword();
    public static final String KEYSTORE_LOCALHOST_TEST_JKS = ConfigReader.environmentConfiguration().getTlsConfiguration().getClientKeystore();

    public static RestAssuredConfig clientCertValid;
    public static RestAssuredConfig clientCertApiml;
    public static RestAssuredConfig clientCertUser;
    public static RestAssuredConfig clientCertUnknownUser;
    public static RestAssuredConfig selfSignedUntrusted;
    public static RestAssuredConfig tlsWithoutCert;
    private static AtomicBoolean isInitialized = new AtomicBoolean(false);

    public synchronized static void prepareSslAuthentication() throws Exception {

        if (!isInitialized.get()) {
                log.info("SSLContext is constructing. This should happen only once.");
                TrustStrategy trustStrategy = (X509Certificate[] chain, String authType) -> true;

                SSLContext sslContext = SSLContextBuilder
                    .create()
                    .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                        KEYSTORE_PASSWORD, KEYSTORE_PASSWORD,
                        (Map<String, PrivateKeyDetails> aliases, Socket socket) -> "apimtst")
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
                clientCertValid = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext)));


                SSLContext sslContext2 = SSLContextBuilder
                    .create()
                    .loadKeyMaterial(ResourceUtils.getFile(ConfigReader.environmentConfiguration().getTlsConfiguration().getKeyStore()),
                        KEYSTORE_PASSWORD, KEYSTORE_PASSWORD)
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
                clientCertApiml = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext2)));

                SSLContext sslContext3 = SSLContextBuilder
                    .create()
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
                tlsWithoutCert = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext3)));
                SSLContext sslContext4 = SSLContextBuilder
                    .create()
                    .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                        KEYSTORE_PASSWORD, KEYSTORE_PASSWORD,
                        (Map<String, PrivateKeyDetails> aliases, Socket socket) -> "unknownuser")
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
                clientCertUnknownUser = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext4)));

                SSLContext sslContext5 = SSLContextBuilder
                    .create()
                    .loadKeyMaterial(ResourceUtils.getFile(KEYSTORE_LOCALHOST_TEST_JKS),
                        KEYSTORE_PASSWORD, KEYSTORE_PASSWORD,
                        (Map<String, PrivateKeyDetails> aliases, Socket socket) -> "user")
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
                clientCertUser = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext5)));

                KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
                String cert = "-----BEGIN CERTIFICATE-----\n" +
                    "MIIEFTCCAv2gAwIBAgIEKWdbVTANBgkqhkiG9w0BAQsFADCBjDELMAkGA1UEBhMC\n" +
                    "Q1oxDTALBgNVBAgTBEJybm8xDTALBgNVBAcTBEJybm8xFDASBgNVBAoTC1pvd2Ug\n" +
                    "U2FtcGxlMRwwGgYDVQQLExNBUEkgTWVkaWF0aW9uIExheWVyMSswKQYDVQQDEyJa\n" +
                    "b3dlIFNlbGYtU2lnbmVkIFVudHJ1c3RlZCBTZXJ2aWNlMB4XDTE4MTIwNzIwMDc1\n" +
                    "MloXDTI4MTIwNDIwMDc1MlowgYwxCzAJBgNVBAYTAkNaMQ0wCwYDVQQIEwRCcm5v\n" +
                    "MQ0wCwYDVQQHEwRCcm5vMRQwEgYDVQQKEwtab3dlIFNhbXBsZTEcMBoGA1UECxMT\n" +
                    "QVBJIE1lZGlhdGlvbiBMYXllcjErMCkGA1UEAxMiWm93ZSBTZWxmLVNpZ25lZCBV\n" +
                    "bnRydXN0ZWQgU2VydmljZTCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                    "AJti8p4nr8ztRSbemrAv1ytVLQMbXozhLe3lNaiVADGTFPZYeJ2lDt7oAl238HOY\n" +
                    "ScpOz+JjTeUkL0jsjNYgMhi4J07II/3sJL0SBfVqvvgjUL4BvcpdBl0crSuI/3D4\n" +
                    "OaPue+ZmPFijwdCcw5JbazMoOka/zUwpYYdbwxPUH2BbKfwtmmygX88nkJcRSoQO\n" +
                    "KBdNsUs+QRuUiokZ/FJi7uiOsNZ8eEfQv6qJ7mOJ7l1IrMcNm3jHgodoQi/4jXO1\n" +
                    "np/hZaz/ZDni9kBwcyd64AViB2v7VrrBmjdESt1mtCIMvKMlwAZAqrDO75Q9pepO\n" +
                    "Y7zbN4s9s7IUfyb9431xg2MCAwEAAaN9MHswHQYDVR0lBBYwFAYIKwYBBQUHAwIG\n" +
                    "CCsGAQUFBwMBMA4GA1UdDwEB/wQEAwIE8DArBgNVHREEJDAighVsb2NhbGhvc3Qu\n" +
                    "bG9jYWxkb21haW6CCWxvY2FsaG9zdDAdBgNVHQ4EFgQUIeSN7aNtwH2MnBAGDLre\n" +
                    "TtcSaZ4wDQYJKoZIhvcNAQELBQADggEBAELPbHlG60nO164yrBjZcpQJ/2e5ThOR\n" +
                    "8efXUWExuy/NpwVx0vJg4tb8s9NI3X4pRh3WyD0uGPGkO9w+CAvgUaECePLYjkov\n" +
                    "KIS6Cvlcav9nWqdZau1fywltmOLu8Sq5i42Yvb7ZcPOEwDShpuq0ql7LR7j7P4XH\n" +
                    "+JkA0k9Zi6RfYJAyOOpbD2R4JoMbxBKrxUVs7cEajl2ltckjyRWoB6FBud1IthRR\n" +
                    "mZoPMtlCleKlsKp7yJiE13hpX+qIGnzEQE2gNgQ94dSl4m2xO6pnyDRMAEncmd33\n" +
                    "oehy77omRxNsLzkWe6mjaC8ShMGzG9jYR02iN2h4083/PVXvTZIqwhg=\n" +
                    "-----END CERTIFICATE-----\n";
                ks.load(null);
                CertificateFactory cf = CertificateFactory.getInstance("X.509");
                InputStream certStream = new ByteArrayInputStream(cert.getBytes());
                Certificate certificate = cf.generateCertificate(certStream);
                ks.setCertificateEntry("selfsigned", certificate);

                SSLContext sslContext6 = SSLContextBuilder
                    .create()
                    .loadKeyMaterial(ks, "password".toCharArray())
                    .loadTrustMaterial(null, trustStrategy)
                    .build();
                selfSignedUntrusted = RestAssuredConfig.newConfig().sslConfig(new SSLConfig().sslSocketFactory(new SSLSocketFactory(sslContext6)));

                isInitialized.set(true);
            }
    }
}


