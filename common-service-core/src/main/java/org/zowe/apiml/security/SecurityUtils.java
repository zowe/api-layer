/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */
package org.zowe.apiml.security;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.zowe.apiml.message.log.ApimlLogger;
import org.zowe.apiml.message.yaml.YamlMessageServiceInstance;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.*;
import java.util.*;

@Slf4j
@UtilityClass
public class SecurityUtils {

    private ApimlLogger apimlLog = ApimlLogger.of(SecurityUtils.class, YamlMessageServiceInstance.getInstance());

    public static final String SAFKEYRING = "safkeyring";

    /**
     * Loads secret key from keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}
     * @param config - {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                                  and optional filled: keyAlias and trustStore
     * @return {@link PrivateKey} or {@link javax.crypto.SecretKey} from keystore or key ring
     */
    public static Key loadKey(HttpsConfig config) {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            try {
                KeyStore ks = loadKeyStore(config);
                char[] keyPasswordInChars = config.getKeyPassword();
                final Key key;
                if (config.getKeyAlias() != null) {
                    key = ks.getKey(config.getKeyAlias(), keyPasswordInChars);
                } else {
                    throw new KeyStoreException("No key alias provided.");
                }
                return key;
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException
                | UnrecoverableKeyException e) {
                apimlLog.log("org.zowe.apiml.common.errorLoadingSecretKey", e.getMessage());
                throw new HttpsConfigError(e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }


    /**
     * Temp method for getting JWTSecret keys
     * //TODO loading from embedded string to be replaced from another source
     * @throws Exception
     */
    public static RSAPrivateKey readPemPrivateKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String key = "-----BEGIN PRIVATE KEY-----\n" +
            "MIIEvQIBADANBgkqhkiG9w0BAQEFAASCBKcwggSjAgEAAoIBAQCOkSanuY2fHxuL\n" +
            "+cXaWosSQ3VzFSfWotq9TYQDLvReZf2tlLHWGUDWTxK3VqkPrvygj45vfwxpv69O\n" +
            "ueT2e8mCzp7rua2ybTQ4/WakYfNBJjZYPADk4Yu/70V4MjodOEAfVwTPXjylEG2I\n" +
            "31WRUz47NXb6+ofmFc7a+dKd5SeciUxVnX4nsblYg8ksPGh1dYWqd7yXnpJghFbP\n" +
            "ratMrQfB7SCEAyuyJPPR5UqB9Wvvqs/SEhhkzALdvVvF+GPBQ65DGMR5gyHaMQv7\n" +
            "k2/YhwR87NdgQ4L8isP+stribid8Gz4kmLDiE6Ae+PN03P0TNXQP4dJasicdcR3+\n" +
            "BaMOfiwXAgMBAAECggEAMU1xGL/KgiS32gheq8x0G7TIgSvnwwo+qwiLhq5OQ/bx\n" +
            "a33ooinJilN+HXkSriHNq5j5oQVGvatUbN1MmRDl9x6NRufHcdTiInM/c8mL3hPg\n" +
            "51KY3I5DTfTpCVAVWNWDF1N4jl4AivTLbHIPnVo0QzWSF+lb5e3Uw1VxyLjeofs0\n" +
            "RFNQacxhAE8TK1kAx5HXpmbB20MuX+rkvKXKoQ/ppaj4gWifdl1pKz7xPL6okcYz\n" +
            "goXIShdtAL4IDOvyE7A8jhjBH9Bf3Vftn1umzUxvTUrAZgSI7FYGIUEKBcw+3ygG\n" +
            "bLy5j8tSsWJTeLykVEc+ZwjZee6VCUMFNn+9my4QQQKBgQDVNYJYDazUiTg8Sqh7\n" +
            "941etnimZOjYFfbcBww2qid6Rw6MxvAGo5fOnBqqbgvttFDw6mKkPzvEbjx0fySS\n" +
            "S+ZfMB1Nqd5xhSxgm2Jrsr1wT/9HsPbOi316E4EDePyy9bpt9NSI5vwcteHU8Wpt\n" +
            "mkaQzjXm+/+OXoyDru9p6veqewKBgQCrLh38qEmg++8RsDEzPXKwNm6AiH+U2H/f\n" +
            "XRHJI0LVb9DFrsjbJp+VtJIBzzyobT7h+B3vw/lY0eAMHJUeACMFiXq0bsGy+nnt\n" +
            "h6p8UgdtB1BDrijXrG7DYCJxUG6Z5aJDhu53LbsLFVthE6qedlzUdNNnC9Vl9o5p\n" +
            "xDt/OliQFQKBgQDIOn9ViEo2M0Pfw1FlUn+uYfj+cygEvuPdkLTUpYl7mT290Zpa\n" +
            "8cnAW/Pi+IQ1UTDuf3/xtfzAJbKayUikJ6mK3Vm3tP7VZ3bcpzCP6gVkc4xPXI78\n" +
            "PB2zxptTkozm2ESjvNjYVOyRXfJfE/WaRtdcaHxQl3pRztNxW5k1xFeg/wKBgFu9\n" +
            "bH7C5irrujVdmxCeBwAfO9uQy+dGnElmBKkqR6BBu76mLKkeqvo9et6TZSvS2Jec\n" +
            "NNcRzWlnmU6EZvpcEmjeRC+9B/xWts+xHJJiF+67s62CAguMMxRsSik2dP/vjKXq\n" +
            "A5VFoe+Ps5h0RMWGI7wNHFsmgWiS2cIfU8+cwmf9AoGAR3vutUBt0+pMqPlAREPQ\n" +
            "BHUjZXhYrxHoD2kvxFkDVlMvPP9GVy6lgjEt1S28oRNjiES2AZWoSoqEbe0ZHg7t\n" +
            "OfsydqmJqfwJaLoAkRzxdqJ66KH2m/BEOBapxMr8B79hfcjpMf2O+T76+6hMFF6j\n" +
            "RIpBLV1t4pDsd7fvwxpR3vA=\n" +
            "-----END PRIVATE KEY-----\n";

        String privateKeyPEM = key
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replaceAll("\n", "")
            .replace("-----END PRIVATE KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(privateKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(encoded);
        return (RSAPrivateKey) keyFactory.generatePrivate(keySpec);
    }

    /**
     * Temp method for getting JWTSecret keys
     * //TODO loading from embedded string to be replaced from another source
     * @throws Exception
     */
    public static RSAPublicKey readPemPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
        String key = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjpEmp7mNnx8bi/nF2lqL\n" +
            "EkN1cxUn1qLavU2EAy70XmX9rZSx1hlA1k8St1apD678oI+Ob38Mab+vTrnk9nvJ\n" +
            "gs6e67mtsm00OP1mpGHzQSY2WDwA5OGLv+9FeDI6HThAH1cEz148pRBtiN9VkVM+\n" +
            "OzV2+vqH5hXO2vnSneUnnIlMVZ1+J7G5WIPJLDxodXWFqne8l56SYIRWz62rTK0H\n" +
            "we0ghAMrsiTz0eVKgfVr76rP0hIYZMwC3b1bxfhjwUOuQxjEeYMh2jEL+5Nv2IcE\n" +
            "fOzXYEOC/IrD/rLa4m4nfBs+JJiw4hOgHvjzdNz9EzV0D+HSWrInHXEd/gWjDn4s\n" +
            "FwIDAQAB\n" +
            "-----END PUBLIC KEY-----";

        String publicKeyPEM = key
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll("\n", "")
            .replace("-----END PUBLIC KEY-----", "");

        byte[] encoded = Base64.getDecoder().decode(publicKeyPEM);

        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(encoded);
        return (RSAPublicKey) keyFactory.generatePublic(keySpec);
    }

    /**
     * Temp method for getting chain of Apiml's certchain public keys for cert filtering
     * //TODO loading from embedded string to be replaced from another source
     * @throws Exception
     */
    public static Set<String> readApimlCertChainPemPublicKeys() {
        String key = "-----BEGIN PUBLIC KEY-----\n" +
            "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAjo7rxDzO51tfSmqahMbY\n" +
            "6lsXLO+/tXYk1ZcIufsh5L+UMs5StHlfSglbiRgWhfdJDTZb9R760klXL7QRYwBc\n" +
            "Yn3yhdYTsTB0+RJddPlTQzxAx45xV7b+fCtsQqBFZk5aes/TduyHCHXQRl+iLos1\n" +
            "3isrl5LSB66ohKxMtflPBeqTM/ptNBbq72XqFCQIZClClvMMYnxrW2FNfftxpLQb\n" +
            "eFu3KN/8V4gcQoSUvE8YU8PYbVUnuhURActywrxHpke5q/tYQR8iDb6D1ZwLU8+/\n" +
            "rTrnPbZq+O2DP7vRyBP9pHS/WNSxY1sTnz7gQ2OlUL+BEQLgRXRPc5ev1kwn0kVd\n" +
            "8QIDAQAB\n" +
            "-----END PUBLIC KEY-----";

        String publicKeyPEM = key
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replaceAll("\n", "")
            .replace("-----END PUBLIC KEY-----", "");

        HashSet<String> setOfKeys = new HashSet<>();
        setOfKeys.add(publicKeyPEM);
        return setOfKeys;
    }

    /**
     * Return certificate chain of certificate using to sing of HTTPS communication (certificate stored in keystore,
     * under keyAlias, both values are determinated via config)
     * @param config defines path to KeyStore and key alias
     * @return chain of certificates loaded from Keystore to alias keyAlias from HttpsConfig
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static Certificate[] loadCertificateChain(HttpsConfig config) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            KeyStore ks = loadKeyStore(config);
            return ks.getCertificateChain(config.getKeyAlias());
        }
        return new Certificate[0];
    }

    /**
     * Method load certification chain from KeyStore for KeyAlias stored in the HttpsConfig. Then it takes public keys
     * and convert them into Base64 code and return them as a Set.
     * @param config defines path to KeyStore and key alias
     * @return Base64 codes of all public certificates determinated for KeyAlias in KeyStore
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     * @throws KeyStoreException
     * @throws IOException
     */
    public static Set<String> loadCertificateChainBase64(HttpsConfig config) throws CertificateException, NoSuchAlgorithmException, KeyStoreException, IOException {
        final Set<String> out = new HashSet<>();
        for (Certificate certificate : loadCertificateChain(config)) {
            final byte[] certificateEncoded = certificate.getPublicKey().getEncoded();
            final String base64 = Base64.getEncoder().encodeToString(certificateEncoded);
            out.add(base64);
        }
        return out;
    }

    /**
     * Loads public key from keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}
     * @param config - {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                                  and optional filled: keyAlias and trustStore
     * @return {@link PublicKey} from keystore or key ring
     */
    public static PublicKey loadPublicKey(HttpsConfig config) {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            try {
                KeyStore ks = loadKeyStore(config);
                Certificate cert = null;
                if (config.getKeyAlias() != null) {
                    cert = ks.getCertificate(config.getKeyAlias());
                } else {
                    for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                        String alias = e.nextElement();
                        cert = ks.getCertificate(alias);
                        if (cert != null) {
                            break;
                        }
                    }
                }
                if (cert != null) {
                    return cert.getPublicKey();
                }
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException e) {
                apimlLog.log("org.zowe.apiml.common.errorLoadingPublicKey", e.getMessage());
                throw new HttpsConfigError(e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Finds a private key by public key in keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}
     * @param config {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword, keyPassword,
     *                                 and optional filled: keyAlias and trustStore
     * @param publicKey in byte[]
     * @return {@link PrivateKey} from keystore or key ring
     */
    public static Key findPrivateKeyByPublic(HttpsConfig config, byte[] publicKey) {
        if (StringUtils.isNotEmpty(config.getKeyStore())) {
            try {
                KeyStore ks = loadKeyStore(config);
                char[] keyPasswordInChars = config.getKeyPassword();
                Key key = null;
                for (Enumeration<String> e = ks.aliases(); e.hasMoreElements(); ) {
                    String alias = e.nextElement();
                    Certificate cert = ks.getCertificate(alias);
                    if (Arrays.equals(cert.getPublicKey().getEncoded(), publicKey)) {
                        key = ks.getKey(alias, keyPasswordInChars);
                        if (key != null) {
                            break;
                        }
                    }
                }
                return key;
            } catch (NoSuchAlgorithmException | KeyStoreException | CertificateException | IOException | UnrecoverableKeyException e) {
                apimlLog.log("org.zowe.apiml.common.errorLoadingSecretKey", e.getMessage());
                throw new HttpsConfigError("Error loading secret key: " + e.getMessage(), e,
                    HttpsConfigError.ErrorCode.HTTP_CLIENT_INITIALIZATION_FAILED, config);
            }
        }
        return null;
    }

    /**
     * Loads keystore or key ring, if keystore URL starts with {@value #SAFKEYRING}, from specified location
     * @param config {@link HttpsConfig} with mandatory filled fields: keyStore, keyStoreType, keyStorePassword,
     *                                 and optional filled: trustStore
     * @return the new {@link KeyStore} or key ring as {@link KeyStore}
     * @throws KeyStoreException
     * @throws IOException
     * @throws CertificateException
     * @throws NoSuchAlgorithmException
     */
    public static KeyStore loadKeyStore(HttpsConfig config) throws KeyStoreException, IOException, CertificateException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(config.getKeyStoreType());
        InputStream inputStream;
        if (config.getKeyStore().startsWith(SAFKEYRING)) {
            URL url = keyRingUrl(config.getKeyStore(), config.getTrustStore());
            inputStream = url.openStream();
        } else {
            File keyStoreFile = new File(config.getKeyStore());
            inputStream = new FileInputStream(keyStoreFile);
        }
        ks.load(inputStream, config.getKeyStorePassword());
        return ks;
    }

    /**
     * Creates an {@link URL} to key ring location
     * @param uri - key ring location
     * @param trustStore - truststore location
     * @return the new {@link URL} with 2 slashes instead of 4
     * @throws MalformedURLException throws in case of incorrect key ring format
     */
    public static URL keyRingUrl(String uri, String trustStore) throws MalformedURLException {
        if (!uri.startsWith(SAFKEYRING + ":////")) {
            throw new MalformedURLException("Incorrect key ring format: " + trustStore
                + ". Make sure you use format safkeyring:////userId/keyRing");
        }
        return new URL(replaceFourSlashes(uri));
    }

    /**
     * Replaces 4 slashes on 2 in URI
     * @param storeUri - URI as {@link String}
     * @return same URI, but with 2 slashes, or null, if {@code storeUri} is null
     */
    public static String replaceFourSlashes(String storeUri) {
        return storeUri == null ? null : storeUri.replaceFirst("////", "//");
    }

    /**
     * Creates a pair of {@link PrivateKey} and {@link PublicKey} keys
     * @param algorithm - key algorithm
     * @param keySize - key size
     * @return the new {@link KeyPair}
     */
    public static KeyPair generateKeyPair(String algorithm, int keySize) {
        KeyPair kp = null;
        try {
            KeyPairGenerator kpg = KeyPairGenerator.getInstance(algorithm);
            kpg.initialize(keySize);
            kp = kpg.generateKeyPair();
        } catch (NoSuchAlgorithmException e) {
            log.debug("An error occurred while generating keypair: {}", e.getMessage());
        }
        return kp;
    }
}
