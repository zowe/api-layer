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
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("squid:S106") //ignoring the System.out System.err warinings
public class LocalVerifier implements Verifier {

    private static final SimpleDateFormat DATE_TIME_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss. SSSZ");

    private final Stores stores;
    private final String[] requiredHostnames;

    public LocalVerifier(Stores stores, String[] requiredHostnames) {
        this.stores = stores;
        this.requiredHostnames = requiredHostnames;
    }

    public boolean verify() {
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
                        return verifyCertificate(alias);
                    }
                } catch (Exception e) {
//               this means that cert is not valid, intentionally ignore
                }

            }
            System.err.println("No trusted certificate found. Add " + x509Certificate.getIssuerDN() + " certificate authority to the trust store ");
        } catch (KeyStoreException e) {
            System.err.println("Error loading secret from keystore" + e.getMessage());
        }

        return false;
    }

    boolean verifyExpiration(X509Certificate serverCert) {
        Date expiration = serverCert.getNotAfter();
        boolean expired = expiration.before(new Date());

        System.out.println("++++++++");
        System.out.println("Expiration data: " + DATE_TIME_FORMAT.format(expiration));
        if (expired) {
            System.out.println("The certificate is expired");
        }
        System.out.println("++++++++");

        return !expired;
    }

    boolean isMatching(String hostname, String cn, List<String> alternativeNames) {
        if (cn.startsWith("*.")) {
            int firstDot = hostname.indexOf('.');
            if ((firstDot > 0) && cn.substring(1).equalsIgnoreCase(hostname.substring(firstDot))) {
                return true;
            }
        }

        return alternativeNames.stream().anyMatch(hostname::equalsIgnoreCase);
    }

    boolean verifyHostnames(X509Certificate serverCert) {
        String commonName;
        List<String> alternativeNames;
        try {
            Pattern pattern = Pattern.compile("CN=(.*?)(?:,|$)");
            Matcher matcher = pattern.matcher(serverCert.getSubjectX500Principal().getName());
            commonName = matcher.find() ? matcher.group(1) : "";
            alternativeNames = serverCert.getSubjectAlternativeNames().stream()
                .flatMap(Collection::stream)
                .map(String::valueOf)
                .distinct()
                .sorted()
                .toList();
        } catch (CertificateParsingException e) {
            System.err.println(e.getMessage());
            return false;
        }

        System.out.println("++++++++");
        System.out.println("Possible hostname values:");
        System.out.println("CN: " + commonName);
        System.out.println("alternative names:");
        alternativeNames.forEach(System.out::println);
        System.out.println("++++++++");

        List<String> notMatching = Arrays.stream(requiredHostnames)
            .filter(requiredHostname -> !isMatching(requiredHostname, commonName, alternativeNames))
            .distinct()
            .sorted()
            .toList();
        if (notMatching.isEmpty()) {
            System.out.println("All required hostnames are matched with the certificate");
        } else {
            System.out.println("Not matched hostnames with the certificate:");
            notMatching.forEach(System.out::println);
        }
        System.out.println("++++++++");

        return notMatching.isEmpty();
    }

    boolean verifyServer(X509Certificate serverCert) {
        try {
            boolean serverAuth = serverCert.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.1");

            System.out.println("++++++++");
            if (serverAuth) {
                System.out.println("Certificate can be used for web server.");
            } else {
                System.out.println("Certificate can't be used for web server. " +
                    "Provide certificate with extended key usage: 1.3.6.1.5.5.7.3.1");
            }
            System.out.println("++++++++");

            return serverAuth;
        } catch (CertificateParsingException e) {
            System.err.println(e.getMessage());
        }

        return false;
    }

    boolean verifyX509(X509Certificate serverCert) {
        try {
            boolean clientAuth = serverCert.getExtendedKeyUsage().contains("1.3.6.1.5.5.7.3.2");

            System.out.println("++++++++");
            if (clientAuth) {
                System.out.println("Certificate can be used for client authentication.");
            } else {
                System.out.println("Certificate can't be used for client authentication. " +
                    "Provide certificate with extended key usage: 1.3.6.1.5.5.7.3.2");
            }
            System.out.println("++++++++");

            return clientAuth;
        } catch (CertificateParsingException e) {
            System.err.println(e.getMessage());
        }

        return false;
    }

    boolean verifyJwt(X509Certificate serverCert) {
        boolean supportedAlgorithm = serverCert.getSigAlgOID().contains("1.2.840.113549.1.1.11");

        System.out.println("++++++++");
        if (supportedAlgorithm) {
            System.out.println("Certificate can be used for generating JWT token.");
        } else {
            System.out.println("Certificate can't be used for generating JWT token. " +
                "Provide certificate with supported algorithm: " +
                "RSASSA-PKCS1-v1_5 using SHA-256 signature algorithm as defined by RFC 7518, Section 3.3." +
                " This algorithm requires a 2048-bit key");
        }
        System.out.println("++++++++");

        return supportedAlgorithm;
    }

    boolean verifyCertificate(String keyAlias) throws KeyStoreException {
        Certificate[] certificate = stores.getKeyStore().getCertificateChain(keyAlias);
        X509Certificate serverCert = (X509Certificate) certificate[0];

        boolean expirationCheck = verifyExpiration(serverCert);
        boolean hostNameCheck = true;
        if (requiredHostnames != null) {
            hostNameCheck = verifyHostnames(serverCert);
        }
        boolean serverCheck = verifyServer(serverCert);
        boolean x509Check = verifyX509(serverCert);
        boolean verifyJwt = verifyJwt(serverCert);

        return expirationCheck && hostNameCheck && serverCheck && x509Check && verifyJwt;
    }

}
