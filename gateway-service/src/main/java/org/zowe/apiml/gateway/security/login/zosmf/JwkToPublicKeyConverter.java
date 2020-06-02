/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.gateway.security.login.zosmf;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigInteger;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.text.ParseException;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.jwk.JWKSet;

import org.bouncycastle.asn1.DERPrintableString;
import org.bouncycastle.asn1.x500.RDN;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.bc.BcX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.openssl.PEMParser;
import org.bouncycastle.openssl.jcajce.JcaPEMWriter;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.zowe.apiml.security.HttpsConfig;
import org.zowe.apiml.security.SecurityUtils;

public class JwkToPublicKeyConverter {
    /**
     * Converts the first public key in JWK in JSON to a certificate PEM format. The
     * public key is from JWK. The certificate signed by the provided CA.
     *
     * @param caAlias            Alias of the key with the CA.
     * @param caKeystore         Keystore where the CA is stored.
     * @param caKeyStoreType     Type of the keystore (e. g. "pkcs12").
     * @param caKeyStorePassword The password to the key store.
     * @param caKeyPassword      The password to the private key of the CA.
     */
    public String convertFirstPublicKeyJwkToPem(String jwkJson, String caAlias, String caKeyStore,
            String caKeyStoreType, String caKeyStorePassword, String caKeyPassword) {
        try {
            HttpsConfig config = HttpsConfig.builder().keyAlias(caAlias).keyStore(caKeyStore)
                    .keyStoreType(caKeyStoreType).keyStorePassword(caKeyStorePassword).keyPassword(caKeyPassword)
                    .build();
            KeyStore keyStore = SecurityUtils.loadKeyStore(config);
            Certificate caCertificate = keyStore.getCertificate(config.getKeyAlias());
            PrivateKey caPrivateKey = (PrivateKey) keyStore.getKey(config.getKeyAlias(),
                    config.getKeyPassword().toCharArray());
            SubjectPublicKeyInfo publicKey = extractPublicKey(jwkJson);

            ContentSigner signer = new JcaContentSignerBuilder("Sha256With" + caPrivateKey.getAlgorithm())
                    .build(caPrivateKey);
            Date now = new Date();
            Calendar c = Calendar.getInstance();
            c.setTime(now);
            c.add(Calendar.YEAR, 10);

            X500Name name = new X500Name(
                    new RDN[] { new RDN(BCStyle.CN, new DERPrintableString("Zowe JWT Public Key")) });

            JcaX509ExtensionUtils extUtils = new JcaX509ExtensionUtils();

            X509CertificateHolder caX509Certificate = new X509CertificateHolder(caCertificate.getEncoded());

            X509CertificateHolder x509CertificateHolder = new X509v3CertificateBuilder(caX509Certificate.getSubject(),
                    new BigInteger(Long.toString(System.currentTimeMillis())), now, c.getTime(), name, publicKey)
                            .addExtension(Extension.subjectKeyIdentifier, false,
                                    new BcX509ExtensionUtils().createSubjectKeyIdentifier(publicKey))
                            .addExtension(Extension.extendedKeyUsage, false,
                                    new ExtendedKeyUsage(new KeyPurposeId[] { KeyPurposeId.id_kp_clientAuth,
                                            KeyPurposeId.id_kp_serverAuth }))
                            .addExtension(Extension.authorityKeyIdentifier, false,
                                    extUtils.createAuthorityKeyIdentifier(caCertificate.getPublicKey()))
                            .build(signer);

            return certificateHolderToPem(x509CertificateHolder);
        } catch (ParseException | JOSEException | CertificateException | IOException | OperatorCreationException
                | KeyStoreException | UnrecoverableKeyException | NoSuchAlgorithmException e) {
            throw new JwkConversionError(e);
        }
    }

    private SubjectPublicKeyInfo extractPublicKey(String jwkJson) throws JOSEException, ParseException, IOException {
        String publicKeyPem = convertFirstPublicKeyJwkToPublicKeyPem(jwkJson);
        PEMParser pemParser = new PEMParser(new StringReader(publicKeyPem));
        SubjectPublicKeyInfo publicKey = (SubjectPublicKeyInfo) pemParser.readObject();
        return publicKey;
    }

    private String certificateHolderToPem(X509CertificateHolder x509CertificateHolder)
            throws CertificateException, IOException {
        StringWriter sw = new StringWriter();
        Certificate cert = CertificateFactory.getInstance("X.509")
                .generateCertificate(new ByteArrayInputStream(x509CertificateHolder.getEncoded()));
        JcaPEMWriter pemWriter = new JcaPEMWriter(sw);
        pemWriter.writeObject(cert);
        pemWriter.flush();
        return sw.toString();
    }

    String convertFirstPublicKeyJwkToPublicKeyPem(String jwkJson) throws JOSEException, ParseException {
        PublicKey key = JWKSet.parse(jwkJson).toPublicJWKSet().getKeys().get(0).toRSAKey().toPublicKey();
        String encoded = Base64.getEncoder().encodeToString(key.getEncoded());
        StringBuilder s = new StringBuilder();
        s.append("-----BEGIN PUBLIC KEY-----");
        for (int i = 0; i < encoded.length(); i++) {
            if (((i % 64) == 0) && (i != (encoded.length() - 1))) {
                s.append("\n");
            }
            s.append(encoded.charAt(i));
        }
        s.append("\n");
        s.append("-----END PUBLIC KEY-----\n");
        return s.toString();
    }
}
