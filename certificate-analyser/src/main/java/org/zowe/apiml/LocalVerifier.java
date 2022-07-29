/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml;

import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Map;

@SuppressWarnings("squid:S106") //ignoring the System.out System.err warinings
public class LocalVerifier implements Verifier {

    private Stores stores;

    public LocalVerifier(Stores stores) {
        this.stores = stores;
    }

    public void verify() {
        System.out.println("=============");
        System.out.println("Verifying keystore: " + stores.getConf().getKeyStore() +
            "  against truststore: " + stores.getConf().getTrustStore());
        try {
            String alias = stores.getConf().getKeyAlias();

            Map<String, Certificate> caList = stores.getListOfCertificates();

            X509Certificate x509Certificate = stores.getX509Certificate(alias);
            for (Map.Entry<String, Certificate> cert : caList.entrySet()) {
                try { //NOSONAR
                    x509Certificate.verify(cert.getValue().getPublicKey());
                    if (cert.getValue() instanceof X509Certificate) {
                        X509Certificate trustedCA = (X509Certificate) cert.getValue();
                        System.out.println("Trusted certificate is stored under alias: " + cert.getKey());
                        System.out.println("Certificate authority: " + trustedCA.getSubjectDN());
                        System.out.println("Details about valid certificate:");
                        printDetails(alias);

                        return;
                    }
                } catch (Exception e) {
//               this means that cert is not valid, intentionally ignore
                }

            }
            System.err.println("No trusted certificate found. Add " + x509Certificate.getIssuerDN() + " certificate authority to the trust store ");
        } catch (KeyStoreException e) {
            System.err.println("Error loading secret from keystore" + e.getMessage());
        }

    }

    void printDetails(String keyAlias) throws KeyStoreException {
        Certificate[] certificate = stores.getKeyStore().getCertificateChain(keyAlias);
        X509Certificate serverCert = (X509Certificate) certificate[0];
        try {
            System.out.println("++++++++");
            System.out.println("Possible hostname values:");
            serverCert.getSubjectAlternativeNames().forEach(System.out::println);
            boolean clientAuth = serverCert.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.2");

            if (clientAuth) {
                System.out.println("Certificate can be used for client authentication.");
            } else {
                System.out.println("Certificate can't be used for client authentication. " +
                    "Provide certificate with extended key usage: 1.3.6.1.5.5.7.3.2");
            }
            System.out.println("++++++++");

        } catch (CertificateParsingException e) {
            System.err.println(e.getMessage());
        }

    }
}
