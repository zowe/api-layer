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

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

public class LocalVerifier {

    boolean verifyLocalKeystore(Stores stores) throws KeyStoreException {
        Certificate[] certificate = stores.getKeyStore().getCertificateChain(stores.getConf().getKeyAlias());
        Map<String, Certificate> caList = new HashMap<>();
        Enumeration<String> aliases = stores.getTrustStore().aliases();

        while (aliases.hasMoreElements()) {
            String certAuthAlias = aliases.nextElement();
            caList.put(certAuthAlias, stores.getTrustStore().getCertificate(certAuthAlias));
        }
        X509Certificate x509Certificate = (X509Certificate) certificate[0];
        for (Map.Entry<String, Certificate> cert : caList.entrySet()) {
            try {
                x509Certificate.verify(cert.getValue().getPublicKey());
                if (cert.getValue() instanceof X509Certificate) {
                    X509Certificate trustedCA = (X509Certificate) cert.getValue();
                    System.out.println("Trusted certificate is stored under alias: "+cert.getKey());
                    System.out.println("valid CA " + trustedCA.getSubjectDN());
                    return true;
                }
            } catch (Exception e) {
//                intentionally ignore, this means that cert is not valid
            }

        }
        System.out.println("Add this CA to trustStore " + x509Certificate.getIssuerDN());
        return false;
    }

    void printDetails(Stores stores) throws KeyStoreException{
        Certificate[] certificate = stores.getKeyStore().getCertificateChain(stores.getConf().getKeyAlias());
        X509Certificate serverCert = (X509Certificate)certificate[0];
        try {
            System.out.println("Possible hostname values:");
            serverCert.getSubjectAlternativeNames().forEach(System.out::println);
            System.out.println("++++++++");

        } catch (CertificateParsingException e) {
            e.printStackTrace();
        }

    }
}
