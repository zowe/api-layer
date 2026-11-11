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

import java.security.AlgorithmParameters;
import java.security.GeneralSecurityException;
import java.security.KeyStoreException;
import java.security.PublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.ECGenParameterSpec;
import java.security.spec.ECParameterSpec;
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
    private static final String OID_SERVER_AUTH = "1.3.6.1.5.5.7.3.1";
    private static final String OID_CLIENT_AUTH = "1.3.6.1.5.5.7.3.2";

    private static final ECParameterSpec P256 = loadP256();

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

    boolean isMatching(String hostname, String pattern) {
        if (pattern.startsWith("*.")) {
            int firstDot = hostname.indexOf('.');
            if ((firstDot > 0) && pattern.substring(1).equalsIgnoreCase(hostname.substring(firstDot))) {
                return true;
            }
        }

        return pattern.equalsIgnoreCase(hostname);
    }

    boolean isMatching(String hostname, String cn, List<String> alternativeNames) {
        return isMatching(hostname, cn) || alternativeNames.stream().anyMatch(alternativeName -> isMatching(hostname, alternativeName));
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

    boolean verifyServer(List<String> extendedKeyUsage) {

        System.out.println("++++++++");

        boolean serverAuth = extendedKeyUsage.contains(OID_SERVER_AUTH);

        if (serverAuth) {
            System.out.println("Certificate can be used for web server.");
        } else {
            System.out.println("Certificate can't be used for web server. " +
                "Provide certificate with Extended Key Usage (EKU) extension serverAuth OID " + OID_SERVER_AUTH);
        }
        System.out.println("++++++++");

        return serverAuth;

    }

    boolean verifyX509(List<String> extendedKeyUsage) {

        boolean clientAuth = extendedKeyUsage.contains(OID_CLIENT_AUTH);

        System.out.println("++++++++");
        if (clientAuth) {
            System.out.println("Certificate can be used for client authentication.");
        } else {
            System.out.println("Certificate can't be used for client authentication. " +
                "Provide certificate with Extended Key Usage (EKU) extension clientAuth OID " + OID_CLIENT_AUTH);
        }
        System.out.println("++++++++");

        return clientAuth;
    }

    /**
     * The JWT is signed with the private key of the certificate, so what matters is the type of its key,
     * not the algorithm the issuing CA used to sign the certificate. The API ML signs with RS256 for RSA keys
     * and with ES256 for EC keys, which is possible only on the P-256 curve. The service does not check the
     * RSA key size; the minimum of 2048 bits is the requirement of RFC 7518, Section 3.3, applied here as advice.
     * Other key types, such as Ed25519, are not supported by the service.
     */
    static boolean isJwtSupportedKey(PublicKey key) {
        if (key instanceof RSAPublicKey rsaKey) {
            return rsaKey.getModulus().bitLength() >= 2048;
        }
        if (key instanceof ECPublicKey ecKey) {
            return isP256(ecKey.getParams());
        }
        return false;
    }

    /**
     * Compares the complete domain parameters, the field size is not enough: secp256k1 and the brainpoolP256
     * curves are 256 bits as well, but they cannot be used for ES256.
     */
    private static boolean isP256(ECParameterSpec params) {
        return P256 != null && params != null
            && P256.getCurve().equals(params.getCurve())
            && P256.getGenerator().equals(params.getGenerator())
            && P256.getOrder().equals(params.getOrder())
            && P256.getCofactor() == params.getCofactor();
    }

    private static ECParameterSpec loadP256() {
        try {
            AlgorithmParameters parameters = AlgorithmParameters.getInstance("EC");
            parameters.init(new ECGenParameterSpec("secp256r1"));
            return parameters.getParameterSpec(ECParameterSpec.class);
        } catch (GeneralSecurityException e) {
            System.err.println("Unable to load the P-256 curve parameters: " + e.getMessage());
            return null;
        }
    }

    boolean verifyJwt(X509Certificate serverCert) {
        boolean supportedAlgorithm = isJwtSupportedKey(serverCert.getPublicKey());

        System.out.println("++++++++");
        if (supportedAlgorithm) {
            System.out.println("Certificate can be used for generating JWT token.");
        } else {
            System.out.println("Certificate can't be used for generating JWT token. " +
                "Provide certificate with a supported key: " +
                "an RSA key of at least 2048 bits (RSASSA-PKCS1-v1_5 using SHA-256 as defined by RFC 7518, Section 3.3) " +
                "or an EC key on the P-256 curve (ECDSA using P-256 and SHA-256, RFC 7518, Section 3.4). " +
                "The signature algorithm of the certificate itself is not relevant.");
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
        boolean serverCheck = false;
        boolean x509Check = false;
        try {
            if (serverCert.getExtendedKeyUsage() == null) {
                System.out.println("Certificate does not contain extended key usage.");
                serverCheck = true;
                System.out.println("Certificate can be used for web server.");
                x509Check = true;
                System.out.println("Certificate can be used for client authentication.");
            } else {
                serverCheck = verifyServer(serverCert.getExtendedKeyUsage());
                x509Check = verifyX509(serverCert.getExtendedKeyUsage());
            }

        } catch (CertificateParsingException e) {
            System.err.println(e.getMessage());
        }
        boolean verifyJwt = verifyJwt(serverCert);

        return expirationCheck && hostNameCheck && serverCheck && x509Check && verifyJwt;
    }

}
