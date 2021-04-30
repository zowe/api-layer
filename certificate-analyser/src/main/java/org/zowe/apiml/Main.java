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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        try (InputStream keyStoreIStream = new FileInputStream("./keystore/localhost/localhost.keystore.p12");
             InputStream trustStoreIStream = new FileInputStream("./keystore/localhost/localhost.trustore.p12")) {
            KeyStore keyStore = readKeyStore(keyStoreIStream, "password".toCharArray());
            Certificate[] certificate = keyStore.getCertificateChain("localhost");
            KeyStore trustStore = readKeyStore(trustStoreIStream, "password".toCharArray());
            List<Certificate> caList = new ArrayList<>();
            Enumeration<String> aliases = trustStore.aliases();
            while (aliases.hasMoreElements()) {
                caList.add(trustStore.getCertificate(aliases.nextElement()));
            }
            X509Certificate x509Certificate = (X509Certificate) certificate[0];
            for (Certificate cert : caList) {
                try {
                    x509Certificate.verify(cert.getPublicKey());
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                }

            }

        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }

    }

    static KeyStore readKeyStore(InputStream is, char[] pass) throws KeyStoreException, CertificateException, NoSuchAlgorithmException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(is, pass);
        return keyStore;
    }
}
