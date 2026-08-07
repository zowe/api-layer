/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gradle.keystore;

import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.bouncycastle.asn1.x500.style.BCStyle;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.Extension;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.asn1.x509.KeyUsage;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cert.jcajce.JcaX509ExtensionUtils;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;

import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertPathValidator;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Generates every TLS keystore, truststore and convenience export the API Mediation Layer needs to
 * run locally and under test.
 *
 * <p>This replaces a shell script that drove openssl and keytool. That needed a POSIX shell, which
 * made it awkward on Windows: it depended on a {@code bash} that Git for Windows only puts on the
 * PATH optionally, on MSYS not mangling {@code -subj "/C=CZ/..."} into a filesystem path, and on
 * the script being checked out with LF endings. None of that applies here — this runs wherever
 * Gradle runs.
 *
 * <p>It is also deterministic in a way the script is not. OpenSSL 3 adds subject and authority key
 * identifiers to a certificate signed with {@code x509 -req -CA} of its own accord; LibreSSL, the
 * openssl macOS ships, does not. Which one a developer happened to have on the PATH therefore
 * changed the generated tree. Here every extension is set explicitly.
 *
 * <p>Everything is done through the JDK — key generation, PKCS12 read/write, PEM/DER encoding, and
 * reading the public trust anchors. BouncyCastle covers the one thing the JDK has no public API
 * for: issuing an X.509 certificate with explicit extensions.
 */
public final class KeystoreGenerator {

    /**
     * Bumped by hand whenever the shape of the generated PKI changes. Written to
     * keystore/generation.stamp so a tree produced by an older generator is regenerated even where
     * there is no Gradle execution history to consult — a fresh clone, or a CI job that unpacked a
     * keystore artifact. Hashing the compiled classes instead would be fragile: class files are not
     * reproducible across compiler versions, so the tree would look stale at random.
     */
    public static final int GENERATOR_VERSION = 1;

    private static final char[] PASSWORD = "password".toCharArray();
    private static final char[] CA_PASSWORD = "local_ca_password".toCharArray();

    private static final int KEY_SIZE = 2048;
    private static final int VALIDITY_DAYS = 3650;
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static final String LOCAL_CA_CN = "Zowe Development Instances Certificate Authority";
    /** The alias the Docker keystores and truststores give the local CA. */
    private static final String LOCAL_CA_ALIAS = "zowe development instances certificate authority";

    /**
     * Public roots the generated truststores must trust: OIDC providers and the other external
     * endpoints the tests reach. Importing all ~150 JDK roots bloats the truststore and caused
     * startup timing regressions in CI, so only these are pulled in.
     *
     * <p>Matched against the CN relative distinguished name of the trust anchor's subject rather
     * than against the JDK's alias for it. Alias spelling differs between JDK vendors, so an alias
     * list silently imports nothing on a JDK that names them differently. Comparing a parsed RDN
     * also means "GTS Root R1" cannot match "GTS Root R11", which a substring match would.
     */
    private static final List<String> WANTED_PUBLIC_CAS = List.of(
        "DigiCert Global Root CA",
        "DigiCert Global Root G2",
        "DigiCert Global Root G3",
        "ISRG Root X1",
        "GTS Root R1",
        "GTS Root R2",
        "GTS Root R3",
        "GTS Root R4"
    );

    /**
     * Default subject alternative names for a certificate that has to answer for the local machine.
     * 127.0.0.1 is an IP entry, not a DNS one: a DNS name that happens to look like an address is
     * not what a hostname verifier consults when the URL holds an IP literal.
     */
    private static final List<GeneralName> LOCALHOST_SANS = List.of(
        dns("localhost"),
        dns("localhost.localdomain"),
        new GeneralName(GeneralName.iPAddress, "127.0.0.1")
    );

    /**
     * Every name a container may be addressed by in the Docker and modulith compose setups. Note
     * that 127.0.0.1 is a dNSName here, not an iPAddress — unlike in {@link #LOCALHOST_SANS}. That
     * is how the committed certificate had it, and nothing in the Docker setup connects to the
     * all-services certificate by IP literal, so it is preserved rather than corrected.
     */
    private static final List<GeneralName> ALL_SERVICES_SANS = Stream.of(
        "localhost", "127.0.0.1",
        "zaas-service", "zaas-service-2",
        "api-catalog-services", "api-catalog-services-2",
        "caching-service", "caching-service-2", "caching-service-3",
        "discovery-service", "discovery-service-2",
        "discoverable-client", "discoverable-client-1", "discoverable-client-2",
        "discoverable-client-3", "discoverable-client-4", "discoverable-client-unknown",
        "mock-services", "mock-services-2", "mock-services-unknown",
        "reverse-proxy",
        "gateway-service", "gateway-service-2",
        "central-gateway-service", "central-gateway-service-2",
        "apiml", "apiml-2", "apiml-3",
        "nodejs-sample-app", "python-sample-app"
    ).map(KeystoreGenerator::dns).toList();

    /** mockserver-netty presents this certificate during TLS negotiation in the zaas-client tests. */
    private static final String MOCKSERVER_CERT = """
        -----BEGIN CERTIFICATE-----
        MIIDqDCCApCgAwIBAgIEPhwe6TANBgkqhkiG9w0BAQsFADBiMRswGQYDVQQDDBJ3
        d3cubW9ja3NlcnZlci5jb20xEzARBgNVBAoMCk1vY2tTZXJ2ZXIxDzANBgNVBAcM
        BkxvbmRvbjEQMA4GA1UECAwHRW5nbGFuZDELMAkGA1UEBhMCVUswIBcNMTYwNjIw
        MTYzNDE0WhgPMjExNzA1MjcxNjM0MTRaMGIxGzAZBgNVBAMMEnd3dy5tb2Nrc2Vy
        dmVyLmNvbTETMBEGA1UECgwKTW9ja1NlcnZlcjEPMA0GA1UEBwwGTG9uZG9uMRAw
        DgYDVQQIDAdFbmdsYW5kMQswCQYDVQQGEwJVSzCCASIwDQYJKoZIhvcNAQEBBQAD
        ggEPADCCAQoCggEBAPGORrdkwTY1H1dvQPYaA+RpD+pSbsvHTtUSU6H7NQS2qu1p
        sE6TEG2fE+Vb0QIXkeH+jjKzcfzHGCpIU/0qQCu4RVycrIW4CCdXjl+T3L4C0I3R
        mIMciTig5qcAvY9P5bQAdWDkU36YGrCjGaX3QlndGxD9M974JdpVK4cqFyc6N4gA
        Onys3uS8MMmSHTjTFAgR/WFeJiciQnal+Zy4ZF2x66CdjN+hP8ch2yH/CBwrSBc0
        ZeH2flbYGgkh3PwKEqATqhVa+mft4dCrvqBwGhBTnzEGWK/qrl9xB4mTs4GQ/Z5E
        8rXzlvpKzVJbfDHfqVzgFw4fQFGV0XMLTKyvOX0CAwEAAaNkMGIwHQYDVR0OBBYE
        FH3W3sL4XRDM/VnRayaSamVLISndMA8GA1UdEwEB/wQFMAMBAf8wCwYDVR0PBAQD
        AgG2MCMGA1UdJQQcMBoGCCsGAQUFBwMBBggrBgEFBQcDAgYEVR0lADANBgkqhkiG
        9w0BAQsFAAOCAQEAecfgKuMxCBe/NxVqoc4kzacf9rjgz2houvXdZU2UDBY3hCs4
        MBbM7U9Oi/3nAoU1zsA8Rg2nBwc76T8kSsfG1TK3iJkfGIOVjcwOoIjy3Z8zLM2V
        YjYbOUyAQdO/s2uShAmzzjh9SV2NKtcNNdoE9e6udvwDV8s3NGMTUpY5d7BHYQqV
        sqaPGlsKi8dN+gdLcRbtQo29bY8EYR5QJm7QJFDI1njODEnrUjjMvWw2yjFlje59
        j/7LBRe2wfNmjXFYm5GqWft10UJ7Ypb3XYoGwcDac+IUvrgmgTHD+E3klV3SUi8i
        Gm5MBedhPkXrLWmwuoMJd7tzARRHHT6PBH/ZGw==
        -----END CERTIFICATE-----
        """;

    /**
     * Subject shared by the localhost service certificates. CN=Zowe Service is what
     * config/local/gateway-service.yml lists in apiml.security.x509.registry.allowedUsers, which is
     * matched against the client certificate's common name.
     */
    private static final X500Name ZOWE_SERVICE_DN =
        dn("CZ", "Prague", "Prague", "Zowe Sample", "API Mediation Layer", "Zowe Service");

    private final Path repoRoot;
    private final Path keystoreDir;
    private final SecureRandom random = new SecureRandom();
    private final Instant now = Instant.now();

    /** A certificate authority and the key that signs with it. */
    private record Ca(X509Certificate cert, PrivateKey key) { }

    /** A generated key pair and the certificate issued over it. */
    private record Issued(KeyPair keyPair, X509Certificate cert) {
        PrivateKey key() {
            return keyPair.getPrivate();
        }
    }

    public KeystoreGenerator(Path repoRoot) {
        this.repoRoot = repoRoot;
        this.keystoreDir = repoRoot.resolve("keystore");
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 1) {
            System.err.println("usage: KeystoreGenerator <repo-root>");
            System.exit(2);
        }
        new KeystoreGenerator(Path.of(args[0])).generate();
    }

    public void generate() throws Exception {
        Path localCaDir = directory(keystoreDir.resolve("local_ca"));
        Path localhostDir = directory(keystoreDir.resolve("localhost"));
        Path selfsignedDir = directory(keystoreDir.resolve("selfsigned"));
        Path dockerDir = directory(keystoreDir.resolve("docker"));
        Path clientCertDir = directory(keystoreDir.resolve("client_cert"));
        Path clientCertCaDir = directory(clientCertDir.resolve("ca"));

        clean(localCaDir, localhostDir, selfsignedDir, dockerDir, clientCertDir, clientCertCaDir);

        // ── 1. Local Certificate Authority ─────────────────────────────────────
        // The distinguished name reproduces the one the committed keystores carried:
        // config/local/gateway-service.yml authorizes the x509 registry by certificate common name,
        // and docker/redis/run-redis.sh builds its own CA with this exact DN.
        System.out.println("=== Generating Local Certificate Authority ===");
        Ca ca = generateCa(dn("CZ", "Prague", "Prague", "Zowe Sample", "API Mediation Layer",
            LOCAL_CA_CN));

        writeKeyPem(localCaDir.resolve("local_ca.key"), ca.key());
        writeCertPem(localCaDir.resolve("local_ca.pem"), ca.cert());
        writeCertPem(localCaDir.resolve("localca.pem"), ca.cert());
        writeCertDer(localCaDir.resolve("localca.cer"), ca.cert());
        writeCertDer(localCaDir.resolve("zowe-dev-ca.cer"), ca.cert());
        writeKeystore(localCaDir.resolve("localca.keystore.p12"), CA_PASSWORD,
            "localca", ca.key(), List.of(ca.cert()), Map.of());

        // Convenience copies read by the NGINX AT-TLS proxy.
        writeCertDer(localhostDir.resolve("localca.cer"), ca.cert());
        writeCertDer(localhostDir.resolve(
            "Zowe_Service_Zowe_Development_Instances_Certificate_Authority_.cer"), ca.cert());
        writeCertDer(localhostDir.resolve("trusted_CAs.cer"), ca.cert());

        // Second, intentionally unrelated CA. It signs the whole localhost2 pair — keystore and
        // truststore — so that localhost2 is a self-consistent PKI with nothing in common with the
        // main one. Both directions are load-bearing in TomcatHttpsTest:
        //   trustStoreWithDifferentCertificateAuthorityShouldFail — a client trusting only this CA
        //       must reject a server holding the main CA's certificate, and
        //   wrongClientCertificateShouldNotFailWhenClientAuthIsWant — the localhost2 certificate
        //       must be an untrusted client certificate as far as the main truststore is concerned.
        // Signing localhost2 with the main CA instead leaves the second test asserting nothing.
        System.out.println("=== Generating Secondary Certificate Authority ===");
        Ca ca2 = generateCa(dn("CZ", "Prague", "Prague", "Zowe Sample", "API Mediation Layer",
            LOCAL_CA_CN + " 2"));
        writeKeyPem(localCaDir.resolve("local_ca2.key"), ca2.key());
        writeCertPem(localCaDir.resolve("localca2.pem"), ca2.cert());
        writeCertDer(localCaDir.resolve("localca2.cer"), ca2.cert());

        // ── 2. Localhost keystores ─────────────────────────────────────────────
        System.out.println("=== Generating localhost keystores ===");

        Issued localhost = signServiceCert(ca, ZOWE_SERVICE_DN, LOCALHOST_SANS);
        writeKeystore(localhostDir.resolve("localhost.keystore.p12"), PASSWORD,
            "localhost", localhost.key(), List.of(localhost.cert()), Map.of("localca", ca.cert()));
        writeTruststore(localhostDir.resolve("localhost.truststore.p12"), Map.of("localca", ca.cert()));

        // Convenience exports. localhost.keystore.key is mounted into the OpenTelemetry collector
        // container, which needs the key in PEM.
        writeCertPem(localhostDir.resolve("localhost.pem"), localhost.cert());
        writeCertPem(localhostDir.resolve("localhost.keystore.cer"), localhost.cert());
        writeKeyPem(localhostDir.resolve("localhost.keystore.key"), localhost.key());

        // localhost2: signed by the secondary CA, and its truststore trusts only that CA.
        Issued localhost2 = signServiceCert(ca2,
            dn("CZ", "Prague", "Prague", "Zowe Sample", "API Mediation Layer", "Zowe Service 2"),
            LOCALHOST_SANS);
        writeKeystore(localhostDir.resolve("localhost2.keystore.p12"), PASSWORD,
            "localhost", localhost2.key(), List.of(localhost2.cert()), Map.of("localca", ca2.cert()));
        writeTruststore(localhostDir.resolve("localhost2.truststore.p12"), Map.of("localca", ca2.cert()));

        // nonlocalhost intentionally does NOT match "localhost". This keystore exists to prove that
        // strict hostname verification rejects a service whose certificate is for another host, so a
        // SAN entry for localhost here silently disables the tests that depend on it. The script
        // creates a matching truststore and deletes it again; nothing consumes it, so it is not
        // produced at all.
        Issued nonlocalhost = signServiceCert(ca,
            dn("CZ", "Czechia", "Prague", "Broadcom", "MSD", "nonlocalhost.local"),
            List.of(dns("nonlocalhost.local")));
        writeKeystore(localhostDir.resolve("nonlocalhost.keystore.p12"), PASSWORD,
            "nonlocalhost", nonlocalhost.key(), List.of(nonlocalhost.cert()),
            Map.of("localca", ca.cert()));

        // localhost-multi serves the multi-instance local setup, where config/local-multi addresses
        // the second instance as https://localhost2:10021 — so the certificate has to answer for
        // localhost2 and localhost3 as well, not just localhost.
        List<GeneralName> multiSans = new ArrayList<>(LOCALHOST_SANS);
        multiSans.add(dns("localhost2"));
        multiSans.add(dns("localhost3"));
        Issued multi = signServiceCert(ca, ZOWE_SERVICE_DN, multiSans);
        writeKeystore(localhostDir.resolve("localhost-multi.keystore.p12"), PASSWORD,
            "localhost-multi", multi.key(), List.of(multi.cert()), Map.of("localca", ca.cert()));
        writeTruststore(localhostDir.resolve("localhost-multi.truststore.p12"),
            Map.of("localca", ca.cert()));

        for (String name : List.of("localhost.truststore.p12", "localhost2.truststore.p12",
                "localhost-multi.truststore.p12")) {
            finalizeTruststore(localhostDir.resolve(name));
        }
        addTrustedCert(localhostDir.resolve("localhost.truststore.p12"), "www.mockserver.com",
            parseCert(MOCKSERVER_CERT));

        // ── 3. Self-signed keystores ───────────────────────────────────────────
        // These certificates are their own trust anchor, so they carry basicConstraints CA:TRUE —
        // openssl req -x509 sets that by default and the truststores below rely on it.
        System.out.println("=== Generating self-signed keystores ===");

        Issued selfSigned = selfSignedServiceCert(ZOWE_SERVICE_DN);
        writeKeystore(selfsignedDir.resolve("localhost.keystore.p12"), PASSWORD,
            "localhost", selfSigned.key(), List.of(selfSigned.cert()), Map.of());
        writeTruststore(selfsignedDir.resolve("localhost.truststore.p12"),
            Map.of("localca", selfSigned.cert()));

        // Untrusted pair: the certificate is signed by nobody the services know, and the matching
        // truststore trusts an unrelated CA — so neither side of the pair validates the other.
        Issued untrusted = selfSignedServiceCert(dn("CZ", "Brno", "Brno", "Zowe Sample",
            "API Mediation Layer", "Zowe Self-Signed Untrusted Service"));
        writeKeystore(selfsignedDir.resolve("localhost-untrusted.keystore.p12"), PASSWORD,
            "localhost", untrusted.key(), List.of(untrusted.cert()), Map.of());
        Ca untrustedCa = generateCa(dn("CZ", "Czechia", "Prague", "Untrusted", "IT", "Untrusted CA"));
        writeTruststore(selfsignedDir.resolve("localhost-untrusted.truststore.p12"),
            Map.of("localca", untrustedCa.cert()));

        // ── 4. Docker keystores ────────────────────────────────────────────────
        System.out.println("=== Generating Docker keystores ===");

        Issued allServices = signCert(ca,
            dn("CZ", "Czechia", "Prague", "Broadcom", "MSD", "Zowe Component"),
            builder -> {
                builder.addExtension(Extension.extendedKeyUsage, false, clientAndServerAuth());
                builder.addExtension(Extension.subjectAlternativeName, false, sans(ALL_SERVICES_SANS));
            });

        // all-services.keystore.p12 carries the CA in the key entry's own chain; server-only.p12
        // presents just the leaf and lets the peer's truststore supply the CA.
        writeKeystore(dockerDir.resolve("all-services.keystore.p12"), PASSWORD,
            "localhost", allServices.key(), List.of(allServices.cert(), ca.cert()),
            Map.of(LOCAL_CA_ALIAS, ca.cert()));
        writeKeystore(dockerDir.resolve("server-only.p12"), PASSWORD,
            "localhost", allServices.key(), List.of(allServices.cert()),
            Map.of(LOCAL_CA_ALIAS, ca.cert()));
        writeTruststore(dockerDir.resolve("all-services.truststore.p12"),
            Map.of(LOCAL_CA_ALIAS, ca.cert()));
        finalizeTruststore(dockerDir.resolve("all-services.truststore.p12"));

        writeCertPem(dockerDir.resolve("all-services.keystore.cer"), allServices.cert());
        writeCertPem(dockerDir.resolve("all-services.cer"), allServices.cert());
        writeKeyPem(dockerDir.resolve("all-services.keystore.key"), allServices.key());
        // Key first, then certificate — the order the consuming tooling expects.
        Files.writeString(dockerDir.resolve("all-services.pem"),
            pem("PRIVATE KEY", allServices.key().getEncoded())
                + pem("CERTIFICATE", allServices.cert().getEncoded()));

        // client-cert.p12 is used by the integration tests as a client keystore. Its key entry keeps
        // the "CN=zowe component, O=OMP" subject that test assertions match on — two RDNs only, in
        // that order.
        Issued dockerClient = signCert(ca,
            new X500NameBuilder(BCStyle.INSTANCE)
                .addRDN(BCStyle.CN, "zowe component")
                .addRDN(BCStyle.O, "OMP")
                .build(),
            builder -> builder.addExtension(Extension.extendedKeyUsage, false,
                new ExtendedKeyUsage(KeyPurposeId.id_kp_clientAuth)));
        writeKeystore(dockerDir.resolve("client-cert.p12"), PASSWORD,
            "localhost", dockerClient.key(), List.of(dockerClient.cert()),
            Map.of(LOCAL_CA_ALIAS, ca.cert()));

        // ── 5. Client certificates ─────────────────────────────────────────────
        System.out.println("=== Generating client certificates ===");

        Ca apimlCa = generateCa(dn("CZ", "Czechia", "Prague", "OMF", "Zowe", "APIML CA"));
        writeKeystore(clientCertCaDir.resolve("apiml_ca.p12"), PASSWORD,
            "apiml_ca", apimlCa.key(), List.of(apimlCa.cert()), Map.of());

        // One combined keystore holding all three client identities plus the CA that signed them.
        // These certificates carry no extensions beyond the key identifiers, matching the script.
        Map<String, Issued> clientCerts = new LinkedHashMap<>();
        for (String cn : List.of("APIMTST", "USER", "UNKNOWNUSER")) {
            clientCerts.put(cn.toLowerCase(), signCert(apimlCa,
                dn("CZ", "Czechia", "Prague", "OMF", "Zowe", cn), builder -> { }));
        }
        KeyStore clientCertsStore = newKeystore();
        for (Map.Entry<String, Issued> entry : clientCerts.entrySet()) {
            clientCertsStore.setKeyEntry(entry.getKey(), entry.getValue().key(), PASSWORD,
                new X509Certificate[]{entry.getValue().cert()});
        }
        clientCertsStore.setCertificateEntry("apiml_ca", apimlCa.cert());
        storeKeystore(clientCertsStore, clientCertDir.resolve("client-certs.p12"), PASSWORD);

        // The mock services do X509 client authentication against these, so both the Docker and the
        // localhost truststores have to trust the APIML CA.
        addTrustedCert(dockerDir.resolve("all-services.truststore.p12"), "apiml ca", apimlCa.cert());
        addTrustedCert(localhostDir.resolve("localhost.truststore.p12"), "apiml ca", apimlCa.cert());

        // ── 6. Test fixtures ───────────────────────────────────────────────────
        System.out.println("=== Copying keystores to test resources ===");
        Path zaasResources = directory(repoRoot.resolve("zaas-client/src/test/resources"));
        Files.copy(localhostDir.resolve("localhost.keystore.p12"),
            zaasResources.resolve("localhost.keystore.p12"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        Files.copy(localhostDir.resolve("localhost.truststore.p12"),
            zaasResources.resolve("localhost.truststore.p12"), java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        // SecurityUtilsTest reads this: the localhost public key, DER SubjectPublicKeyInfo, base64
        // on a single line with no trailing newline.
        Path commonResources = directory(repoRoot.resolve("common-service-core/src/test/resources"));
        Files.writeString(commonResources.resolve("jwt-public-key.pub"),
            Base64.getEncoder().encodeToString(localhost.cert().getPublicKey().getEncoded()));

        // ── 7. Sanity checks ───────────────────────────────────────────────────
        // Assert the properties the tests depend on, so a change to the generation logic fails here
        // instead of surfacing as an unrelated TLS failure much later.
        System.out.println("=== Verifying generated keystores ===");
        assertSanAbsent(localhostDir.resolve("nonlocalhost.keystore.p12"), "localhost");
        assertSanPresent(localhostDir.resolve("localhost.keystore.p12"), "localhost");
        assertSanPresent(selfsignedDir.resolve("localhost.keystore.p12"), "localhost");
        assertSanPresent(dockerDir.resolve("all-services.keystore.p12"), "gateway-service");
        assertSanPresent(dockerDir.resolve("all-services.keystore.p12"), "discovery-service");
        assertSanPresent(localhostDir.resolve("localhost-multi.keystore.p12"), "localhost2");
        assertSanPresent(localhostDir.resolve("localhost-multi.keystore.p12"), "localhost3");
        assertSubjectCn(localhostDir.resolve("localhost.keystore.p12"), "Zowe Service");
        assertIssuerCn(localhostDir.resolve("localhost2.keystore.p12"), LOCAL_CA_CN + " 2");

        // Trust paths, read back off disk so this also proves the stores were written correctly.
        // The negative cases carry the weight: a truststore that trusts too much does not fail
        // anything at generation time, it quietly turns the tests that rely on rejection into
        // tests that assert nothing.
        Path mainTrust = localhostDir.resolve("localhost.truststore.p12");
        Path secondTrust = localhostDir.resolve("localhost2.truststore.p12");
        Path dockerTrust = dockerDir.resolve("all-services.truststore.p12");

        assertTrust(localhostDir.resolve("localhost.keystore.p12"), "localhost", mainTrust, true);
        assertTrust(localhostDir.resolve("localhost-multi.keystore.p12"), "localhost-multi",
            mainTrust, true);
        assertTrust(dockerDir.resolve("all-services.keystore.p12"), "localhost", dockerTrust, true);
        assertTrust(dockerDir.resolve("all-services.keystore.p12"), "localhost", mainTrust, true);
        // TomcatHttpsTest relies on both directions of the localhost/localhost2 mismatch.
        assertTrust(localhostDir.resolve("localhost.keystore.p12"), "localhost", secondTrust, false);
        assertTrust(localhostDir.resolve("localhost2.keystore.p12"), "localhost", mainTrust, false);
        // The self-signed pair must be foreign to the main PKI in both directions.
        assertTrust(selfsignedDir.resolve("localhost.keystore.p12"), "localhost", mainTrust, false);
        assertTrust(selfsignedDir.resolve("localhost-untrusted.keystore.p12"), "localhost",
            selfsignedDir.resolve("localhost-untrusted.truststore.p12"), false);
        // The mock services do X509 client authentication against these, which only works because
        // section 5 imported the APIML CA into both truststores.
        for (String alias : clientCerts.keySet()) {
            assertTrust(clientCertDir.resolve("client-certs.p12"), alias, mainTrust, true);
            assertTrust(clientCertDir.resolve("client-certs.p12"), alias, dockerTrust, true);
            assertTrust(clientCertDir.resolve("client-certs.p12"), alias, secondTrust, false);
        }
        // The local CA plus the public roots; a truststore short of these has silently lost its
        // external trust material.
        assertTrustAnchorsAtLeast(mainTrust, WANTED_PUBLIC_CAS.size() + 2);
        assertTrustAnchorsAtLeast(dockerTrust, WANTED_PUBLIC_CAS.size() + 2);
        System.out.println("All checks passed.");

        Files.writeString(keystoreDir.resolve("generation.stamp"), GENERATOR_VERSION + "\n");
        System.out.println("=== Keystore generation complete ===");
    }

    // ── Certificate issuance ───────────────────────────────────────────────────

    private KeyPair newKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(KEY_SIZE, random);
        return generator.generateKeyPair();
    }

    /**
     * Generate a self-signed CA.
     *
     * <p>No AuthorityKeyIdentifier is added, deliberately: a self-referential AKI on a root breaks
     * chain building for some consumers. In the shell script this took a config-file workaround,
     * because LibreSSL rejects {@code authorityKeyIdentifier=none} outright and OpenSSL 3 adds one
     * automatically unless an extension section is supplied. Here it is simply never added.
     */
    private Ca generateCa(X500Name subject) throws Exception {
        KeyPair keyPair = newKeyPair();
        JcaX509v3CertificateBuilder builder = certificateBuilder(subject, subject, keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.keyUsage, true,
            new KeyUsage(KeyUsage.keyCertSign | KeyUsage.cRLSign));
        builder.addExtension(Extension.subjectKeyIdentifier, false,
            extensionUtils().createSubjectKeyIdentifier(keyPair.getPublic()));
        return new Ca(sign(builder, keyPair.getPrivate()), keyPair.getPrivate());
    }

    /**
     * Issue a localhost service certificate: keyUsage, extendedKeyUsage and subjectAltName, all
     * non-critical, and no basicConstraints — the extension set the script's {@code [v3_req]}
     * section produces.
     */
    private Issued signServiceCert(Ca ca, X500Name subject, List<GeneralName> sans) throws Exception {
        return signCert(ca, subject, builder -> {
            builder.addExtension(Extension.keyUsage, false, serviceKeyUsage());
            builder.addExtension(Extension.extendedKeyUsage, false, clientAndServerAuth());
            builder.addExtension(Extension.subjectAlternativeName, false, sans(sans));
        });
    }

    /**
     * Issue a certificate signed by {@code ca}, with whatever extensions the caller adds on top of
     * the subject and authority key identifiers that every issued certificate carries.
     */
    private Issued signCert(Ca ca, X500Name subject, ExtensionCustomizer customizer) throws Exception {
        KeyPair keyPair = newKeyPair();
        X500Name issuer = X500Name.getInstance(ca.cert().getSubjectX500Principal().getEncoded());
        JcaX509v3CertificateBuilder builder = certificateBuilder(issuer, subject, keyPair.getPublic());
        customizer.customize(builder);
        addKeyIdentifiers(builder, keyPair.getPublic(), ca.cert().getPublicKey());
        return new Issued(keyPair, sign(builder, ca.key()));
    }

    /**
     * A self-signed service certificate, which is also its own trust anchor — hence CA:TRUE, which
     * is what {@code openssl req -x509} emits by default and what the paired truststore relies on.
     */
    private Issued selfSignedServiceCert(X500Name subject) throws Exception {
        KeyPair keyPair = newKeyPair();
        JcaX509v3CertificateBuilder builder =
            certificateBuilder(subject, subject, keyPair.getPublic());
        addKeyIdentifiers(builder, keyPair.getPublic(), keyPair.getPublic());
        builder.addExtension(Extension.basicConstraints, true, new BasicConstraints(true));
        builder.addExtension(Extension.subjectAlternativeName, false, sans(LOCALHOST_SANS));
        builder.addExtension(Extension.keyUsage, false, serviceKeyUsage());
        builder.addExtension(Extension.extendedKeyUsage, false, clientAndServerAuth());
        return new Issued(keyPair, sign(builder, keyPair.getPrivate()));
    }

    /**
     * OpenSSL 3 adds both of these when signing; LibreSSL does not. Setting them explicitly is what
     * makes the output independent of which openssl a developer has — and of having one at all.
     */
    private void addKeyIdentifiers(JcaX509v3CertificateBuilder builder, PublicKey subjectKey,
                                   PublicKey issuerKey) throws Exception {
        JcaX509ExtensionUtils utils = extensionUtils();
        builder.addExtension(Extension.subjectKeyIdentifier, false,
            utils.createSubjectKeyIdentifier(subjectKey));
        // Keyid form only, matching OpenSSL's default; the issuer-and-serial form is not used here.
        builder.addExtension(Extension.authorityKeyIdentifier, false,
            utils.createAuthorityKeyIdentifier(issuerKey));
    }

    @FunctionalInterface
    private interface ExtensionCustomizer {
        void customize(JcaX509v3CertificateBuilder builder) throws Exception;
    }

    private JcaX509v3CertificateBuilder certificateBuilder(X500Name issuer, X500Name subject,
                                                           PublicKey publicKey) {
        return new JcaX509v3CertificateBuilder(
            issuer,
            new BigInteger(159, random).add(BigInteger.ONE),
            Date.from(now),
            Date.from(now.plus(VALIDITY_DAYS, ChronoUnit.DAYS)),
            subject,
            publicKey);
    }

    private X509Certificate sign(JcaX509v3CertificateBuilder builder, PrivateKey signingKey)
            throws Exception {
        return new JcaX509CertificateConverter()
            .getCertificate(builder.build(new JcaContentSignerBuilder(SIGNATURE_ALGORITHM)
                .build(signingKey)));
    }

    private static JcaX509ExtensionUtils extensionUtils() throws Exception {
        return new JcaX509ExtensionUtils();
    }

    private static KeyUsage serviceKeyUsage() {
        return new KeyUsage(
            KeyUsage.digitalSignature | KeyUsage.nonRepudiation | KeyUsage.keyEncipherment);
    }

    private static ExtendedKeyUsage clientAndServerAuth() {
        return new ExtendedKeyUsage(
            new KeyPurposeId[]{KeyPurposeId.id_kp_clientAuth, KeyPurposeId.id_kp_serverAuth});
    }

    private static GeneralNames sans(List<GeneralName> names) {
        return new GeneralNames(names.toArray(new GeneralName[0]));
    }

    private static GeneralName dns(String name) {
        return new GeneralName(GeneralName.dNSName, name);
    }

    private static X500Name dn(String c, String st, String l, String o, String ou, String cn) {
        // Order matters: it is the encoded order of the DN, and the script's openssl config lists
        // the fields C, ST, L, O, OU, CN in exactly this sequence.
        return new X500NameBuilder(BCStyle.INSTANCE)
            .addRDN(BCStyle.C, c)
            .addRDN(BCStyle.ST, st)
            .addRDN(BCStyle.L, l)
            .addRDN(BCStyle.O, o)
            .addRDN(BCStyle.OU, ou)
            .addRDN(BCStyle.CN, cn)
            .build();
    }

    // ── Trust material ─────────────────────────────────────────────────────────

    private void finalizeTruststore(Path truststore) throws Exception {
        importPublicRoots(truststore);
        importExtraCas(truststore);
    }

    /**
     * Import the wanted public roots from the trust anchors of the running JVM. The shell script has
     * to locate a cacerts file on disk and parse {@code keytool -list -v} with awk to do this; here
     * the anchors are an API call away, and they are the same ones the build itself trusts.
     */
    private void importPublicRoots(Path truststore) throws Exception {
        List<X509Certificate> anchors = jvmTrustAnchors();
        KeyStore ks = loadKeystore(truststore, PASSWORD);
        int imported = 0;
        for (String wanted : WANTED_PUBLIC_CAS) {
            X509Certificate match = anchors.stream()
                .filter(cert -> wanted.equals(commonName(cert)))
                .findFirst()
                .orElse(null);
            if (match == null) {
                System.out.println("WARNING: '" + wanted
                    + "' is not a trust anchor of this JVM; TLS connections relying on it will fail");
                continue;
            }
            ks.setCertificateEntry(wanted, match);
            imported++;
        }
        storeKeystore(ks, truststore, PASSWORD);
        System.out.println("  imported " + imported + " of " + WANTED_PUBLIC_CAS.size()
            + " public CA certificates into " + keystoreDir.relativize(truststore));
    }

    private static List<X509Certificate> jvmTrustAnchors() throws Exception {
        TrustManagerFactory factory =
            TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        factory.init((KeyStore) null);
        for (TrustManager manager : factory.getTrustManagers()) {
            if (manager instanceof X509TrustManager x509) {
                return List.of(x509.getAcceptedIssuers());
            }
        }
        return List.of();
    }

    /**
     * Compare the CN as a parsed relative distinguished name rather than as a substring of the DN
     * string, so that "GTS Root R1" cannot match "GTS Root R11".
     */
    private static String commonName(X509Certificate cert) {
        try {
            for (Rdn rdn : new LdapName(cert.getSubjectX500Principal().getName()).getRdns()) {
                if ("CN".equalsIgnoreCase(rdn.getType())) {
                    return rdn.getValue().toString();
                }
            }
        } catch (Exception ignored) {
            // A trust anchor with an unparseable subject is simply not one of the wanted roots.
        }
        return null;
    }

    /**
     * Trust everything dropped into keystore/extra_ca. This is the supported way to restore CAs that
     * used to live inside the committed truststores and cannot be rebuilt from public material —
     * corporate roots needed to reach an internal z/OSMF or OIDC provider. That directory is never
     * cleaned and nothing in it is committed.
     */
    private void importExtraCas(Path truststore) throws Exception {
        Path extraDir = keystoreDir.resolve("extra_ca");
        if (!Files.isDirectory(extraDir)) {
            return;
        }
        KeyStore ks = loadKeystore(truststore, PASSWORD);
        try (Stream<Path> files = Files.list(extraDir)) {
            for (Path file : files.sorted().toList()) {
                String name = file.getFileName().toString();
                if (!name.endsWith(".pem") && !name.endsWith(".cer") && !name.endsWith(".crt")) {
                    continue;
                }
                System.out.println("Importing extra CA " + name + " into "
                    + keystoreDir.relativize(truststore));
                ks.setCertificateEntry("extra_" + name.substring(0, name.lastIndexOf('.')),
                    parseCert(Files.readString(file)));
            }
        }
        storeKeystore(ks, truststore, PASSWORD);
    }

    private void addTrustedCert(Path truststore, String alias, X509Certificate cert)
            throws Exception {
        KeyStore ks = loadKeystore(truststore, PASSWORD);
        ks.setCertificateEntry(alias, cert);
        storeKeystore(ks, truststore, PASSWORD);
    }

    private static X509Certificate parseCert(String pem) throws Exception {
        return (X509Certificate) CertificateFactory.getInstance("X.509")
            .generateCertificate(new ByteArrayInputStream(pem.getBytes(StandardCharsets.UTF_8)));
    }

    // ── Sanity checks ──────────────────────────────────────────────────────────

    private void assertSanPresent(Path keystore, String host) throws Exception {
        if (!sanHosts(keystore).contains(host)) {
            throw new IllegalStateException(
                keystoreDir.relativize(keystore) + " is missing a SAN entry for '" + host + "'");
        }
    }

    private void assertSanAbsent(Path keystore, String host) throws Exception {
        if (sanHosts(keystore).contains(host)) {
            throw new IllegalStateException(
                keystoreDir.relativize(keystore) + " must NOT carry a SAN entry for '" + host + "'");
        }
    }

    private void assertSubjectCn(Path keystore, String cn) throws Exception {
        if (!cn.equals(commonName(firstCert(keystore)))) {
            throw new IllegalStateException(keystoreDir.relativize(keystore)
                + " is missing a certificate with common name '" + cn + "'");
        }
    }

    private void assertIssuerCn(Path keystore, String cn) throws Exception {
        X509Certificate cert = firstCert(keystore);
        String issuer = cert.getIssuerX500Principal().getName();
        if (!issuer.contains("CN=" + cn)) {
            throw new IllegalStateException(
                keystoreDir.relativize(keystore) + " must be signed by '" + cn + "', not " + issuer);
        }
    }

    /**
     * Assert that the leaf of {@code alias} in {@code keystore} does — or does not — build a valid
     * chain to a trust anchor in {@code truststore}.
     */
    private void assertTrust(Path keystore, String alias, Path truststore, boolean expected)
            throws Exception {
        X509Certificate leaf = (X509Certificate) loadKeystore(keystore, PASSWORD)
            .getCertificateChain(alias)[0];
        if (validates(leaf, truststore) != expected) {
            throw new IllegalStateException(String.format(
                "%s [%s] must %sbe trusted by %s",
                keystoreDir.relativize(keystore), alias, expected ? "" : "NOT ",
                keystoreDir.relativize(truststore)));
        }
    }

    private boolean validates(X509Certificate leaf, Path truststore) throws Exception {
        KeyStore ks = loadKeystore(truststore, PASSWORD);
        Set<TrustAnchor> anchors = new HashSet<>();
        for (String alias : java.util.Collections.list(ks.aliases())) {
            if (ks.isCertificateEntry(alias)) {
                anchors.add(new TrustAnchor((X509Certificate) ks.getCertificate(alias), null));
            }
        }
        try {
            PKIXParameters parameters = new PKIXParameters(anchors);
            parameters.setRevocationEnabled(false);
            CertPathValidator.getInstance("PKIX").validate(
                CertificateFactory.getInstance("X.509").generateCertPath(List.of(leaf)), parameters);
            return true;
        } catch (GeneralSecurityException rejected) {
            return false;
        }
    }

    private void assertTrustAnchorsAtLeast(Path truststore, int expected) throws Exception {
        KeyStore ks = loadKeystore(truststore, PASSWORD);
        long anchors = java.util.Collections.list(ks.aliases()).stream()
            .filter(alias -> {
                try {
                    return ks.isCertificateEntry(alias);
                } catch (KeyStoreException e) {
                    return false;
                }
            })
            .count();
        if (anchors < expected) {
            throw new IllegalStateException(keystoreDir.relativize(truststore) + " holds only "
                + anchors + " trust anchors, expected at least " + expected);
        }
    }

    private List<String> sanHosts(Path keystore) throws Exception {
        var names = firstCert(keystore).getSubjectAlternativeNames();
        if (names == null) {
            return List.of();
        }
        return names.stream().map(entry -> String.valueOf(entry.get(1))).toList();
    }

    /** The leaf of the keystore's single key entry. */
    private X509Certificate firstCert(Path keystore) throws Exception {
        KeyStore ks = loadKeystore(keystore, PASSWORD);
        for (String alias : java.util.Collections.list(ks.aliases())) {
            if (ks.isKeyEntry(alias)) {
                return (X509Certificate) ks.getCertificateChain(alias)[0];
            }
        }
        throw new IllegalStateException("no key entry in " + keystore);
    }

    // ── Keystore and file output ───────────────────────────────────────────────

    /**
     * Write a PKCS12 holding one key entry plus any number of trusted certificate entries.
     *
     * <p>Note that a chain of {@code [leaf]} plus the CA as a separate trusted entry reads back as a
     * two-element chain: the JDK reconstructs the chain from every certificate in the file when it
     * loads one. The shell script's output behaves the same way, so callers pass whichever the
     * script passed and the result is identical either way.
     */
    private void writeKeystore(Path path, char[] password, String keyAlias, PrivateKey key,
                               List<X509Certificate> chain, Map<String, X509Certificate> trusted)
            throws Exception {
        KeyStore ks = newKeystore();
        ks.setKeyEntry(keyAlias, key, password, chain.toArray(new X509Certificate[0]));
        for (Map.Entry<String, X509Certificate> entry : trusted.entrySet()) {
            ks.setCertificateEntry(entry.getKey(), entry.getValue());
        }
        storeKeystore(ks, path, password);
    }

    private void writeTruststore(Path path, Map<String, X509Certificate> trusted) throws Exception {
        KeyStore ks = newKeystore();
        for (Map.Entry<String, X509Certificate> entry : trusted.entrySet()) {
            ks.setCertificateEntry(entry.getKey(), entry.getValue());
        }
        storeKeystore(ks, path, PASSWORD);
    }

    private static KeyStore newKeystore() throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        ks.load(null, null);
        return ks;
    }

    private static KeyStore loadKeystore(Path path, char[] password) throws Exception {
        KeyStore ks = KeyStore.getInstance("PKCS12");
        try (var in = Files.newInputStream(path)) {
            ks.load(in, password);
        }
        return ks;
    }

    private static void storeKeystore(KeyStore ks, Path path, char[] password) throws Exception {
        try (OutputStream out = Files.newOutputStream(path)) {
            ks.store(out, password);
        }
    }

    private static void writeCertDer(Path path, X509Certificate cert) throws Exception {
        Files.write(path, cert.getEncoded());
    }

    private static void writeCertPem(Path path, X509Certificate cert) throws Exception {
        Files.writeString(path, pem("CERTIFICATE", cert.getEncoded()));
    }

    /** PKCS#8, unencrypted — the form {@code openssl pkcs12 -nocerts -nodes} produces. */
    private static void writeKeyPem(Path path, Key key) throws IOException {
        Files.writeString(path, pem("PRIVATE KEY", key.getEncoded()));
    }

    private static String pem(String type, byte[] der) {
        return "-----BEGIN " + type + "-----\n"
            + Base64.getMimeEncoder(64, new byte[]{'\n'}).encodeToString(der)
            + "\n-----END " + type + "-----\n";
    }

    private static Path directory(Path path) throws IOException {
        Files.createDirectories(path);
        return path;
    }

    /**
     * Remove previously generated material so an interrupted or older run cannot leave a file behind
     * that nothing regenerates. keystore/extra_ca is deliberately not touched.
     */
    private void clean(Path... directories) throws IOException {
        List<String> generated =
            List.of(".p12", ".cer", ".key", ".pem", ".csr", ".crt", ".srl");
        for (Path directory : directories) {
            try (Stream<Path> files = Files.list(directory)) {
                for (Path file : files.filter(Files::isRegularFile).toList()) {
                    String name = file.getFileName().toString();
                    if (generated.stream().anyMatch(name::endsWith)) {
                        Files.delete(file);
                    }
                }
            }
        }
    }
}
