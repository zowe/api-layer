# TLS Certificates for localhost

TLS certificates are signed by a local certificate authority (CA) that is trusted by the
HTTPS clients in integration tests. The root certificate of the local CA can also be
imported to your browser.

**None of this material is committed to the repository.** It is generated locally and
ignored by git.

## Generating the key stores

```shell
./gradlew generateKeystores
```

The Gradle build runs this automatically before anything that needs TLS material — tests,
`bootRun`, `jib`, `build` — and skips it when the tree is already present and current. Run
it explicitly after a fresh clone if you intend to run individual tests from an IDE.

Generation happens inside the Gradle JVM, in
`keystore-plugin/src/main/java/org/zowe/apiml/gradle/keystore/KeystoreGenerator.java`. It needs no
external tools — no `openssl`, no `keytool`, no shell — so it behaves identically on Linux,
macOS and Windows.

Besides the tree described below, generation produces two test fixtures:
`zaas-client/src/test/resources/localhost.*store.p12` and
`common-service-core/src/test/resources/jwt-public-key.pub`.

`keystore/generation.stamp` records the `GENERATOR_VERSION` that produced the tree, so that a
tree built by an older generator is replaced even where Gradle has no execution history to
consult — a fresh clone, or a CI job that unpacked a keystore artifact. Delete it (or the key
stores) to force regeneration.

### Trusting additional certificate authorities

Every generated trust store trusts the local CA plus a small set of public roots. To trust
something else — a corporate root needed to reach an internal z/OSMF or OIDC provider —
drop the certificate into `keystore/extra_ca/` as `.pem`, `.cer` or `.crt` and re-run
generation. That directory is not cleaned between runs and nothing in it is committed.

## Key stores

* `keystore/localhost/localhost.keystore.cer`
  * convenience
  * contains the exported server certificate signed by the local CA and private key for the server

* `keystore/localhost/localhost.keystore.key`
  * convenience
  * contains the exported private key

* `keystore/localhost/localhost.pem`
  * convenience
  * contains the exported server certificate in PEM format for use with http clients

* `keystore/localhost/localhost.keystore.p12`
  * password: `password`
  * used for the HTTPS server(s)
  * contains the server certificate signed by the local CA and private key for the server

* `keystore/localhost/localhost.truststore.p12`
  * password: `password`
  * used for HTTPS clients (e.g. integration tests, services using the gateway)
  * contains the root certificate of the local CA (not the server certificate)

* `keystore/localhost/localhost2.keystore.p12`
  * password: `password`
  * used for tests only — signed by a second, unrelated CA, so that it is *not* trusted
    by `localhost.truststore.p12`

* `keystore/localhost/localhost2.truststore.p12`
  * password: `password`
  * used for tests only — trusts only the second CA, so it does *not* trust
    `localhost.keystore.p12`

### Local CA

* `keystore/local_ca/localca.cer`
  * public certificate of local CA
  
* `keystore/local_ca/localca.keystore.p12`
  * password: `local_ca_password`
  * private key of the local CA

### Client certificates

* `keystore/client_cert/ca/apiml_ca.p12`
  API ML External Certificate authority - Certificate and private key of additional certificate authority that is trusted by apiml and can sign certificates that are used for authentication. Convenience export only.

* `keystore/client_cert/client-certs.p12`  

  Client certificates - used for testing of client certificate authentication functionality. APIMTST, USER and UNKNOWNUSER.
  Keystore containing all the above including private keys. Used for testing client certificate authentication functionality.
  
  * `keystore/client_cert/openssl.conf`  
  openssl Configuration for certificate generation

### Certificates for NGINX proxy (for AT-TLS simulation)

The following files are used by the NGINX proxy to simulate AT_TLS on the CI server:

* `keystore/localhost/Zowe_Service_Zowe_Development_Instances_Certificate_Authority_.cer`
* `keystore/localhost/localca.cer`
* `keystore/localhost/trusted_CAs.cer`

## Generate your own certificates for localhost

The steps below are a reference for building a certificate by hand — for example one with
a subject or SAN set that the generator does not produce. They are not needed for the
normal workflow; `./gradlew generateKeystores` covers that.

### (Optional)Generate certificate authority

create private key

```bash
    openssl genrsa -out local_ca.key 2048
```

create certificate

```bash
    openssl req -x509 -new -nodes -key local_ca.key -sha256 -days 1825 -out local_ca.pem
```

### Generate certificate

#### generate CSR together with private key in PEM format

```bash
    openssl req -newkey rsa:2048 -nodes -keyout localhost.key -sha256 -out localhost.csr -outform PEM -subj "/C=CZ/ST=Czechia/L=Prague/O=Broadcom/OU=MSD/CN=*.zowe.svc.cluster.local" -config ../client_cert/openssl.conf -extensions v3_req
```

**Verify CSR**

```bash
    openssl req -text -noout -verify -in localhost.csr
```

Example of a valid CSR:

```plaintext
    Certificate Request:
    Data:
    Version: 1 (0x0)
    Subject: C = CZ, ST = Czechia, L = Prague, O = Broadcom Inc, OU = IT, CN = localhost
    Subject Public Key Info:
    Public Key Algorithm: rsaEncryption
    RSA Public-Key: (2048 bit)
    Modulus:
    ...
    Exponent: 65537 (0x10001)
    Attributes:
    Requested Extensions:
    X509v3 Key Usage:
    Key Encipherment, Data Encipherment
    X509v3 Extended Key Usage:
    TLS Web Client Authentication, TLS Web Server Authentication
    X509v3 Subject Alternative Name:
    DNS:localhost, DNS:127.0.0.1
    Signature Algorithm: sha1WithRSAEncryption
    ...
```

#### sign the request using CA, this will produce certificate in PEM format

**Note:** You may need to export the public and private key in PEM format from the local CA keystore

With PWD as <project_root>/keystore/local_ca, run the following comnmand:

```bash
openssl x509 -req -in localhost.csr -CA local_ca.pem -CAkey local_ca.key \
-CAcreateserial -out localhost.crt -days 1825 -sha256 -extfile ../client_cert/openssl.conf -extensions v3_req
```

Use the following script to display the certificate content:

```bash
openssl x509 -in localhost.pem -text -noout
```

Example of a valid signed certificate:

```plaintext
    Certificate:
    Data:
    Version: 3 (0x2)
    Serial Number:
    66:ab:1e:0b:6f:f9:69:c5:45:1a:41:06:c6:de:ea:34:bf:d0:20:0f
    Signature Algorithm: sha256WithRSAEncryption
    Issuer: C = CZ, ST = Prague, L = Prague, O = Broadcom, OU = MFD, CN = APIML External         Certificate Authority
    Validity
    Not Before: Oct 11 11:04:06 2021 GMT
    Not After : Feb 23 11:04:06 2023 GMT
    Subject: C = CZ, ST = Czechia, L = Prague, O = Broadcom Inc, OU = IT, CN = localhost
    Subject Public Key Info:
    Public Key Algorithm: rsaEncryption
    RSA Public-Key: (2048 bit)
    Modulus:
    ...
    Exponent: 65537 (0x10001)
    X509v3 extensions:
    X509v3 Key Usage:
    Key Encipherment, Data Encipherment
    X509v3 Extended Key Usage:
    TLS Web Client Authentication, TLS Web Server Authentication
    X509v3 Subject Alternative Name:
    DNS:localhost, DNS:127.0.0.1
    Signature Algorithm: sha256WithRSAEncryption
    ...
```

#### Create PKCS12 truststore and keystore

Create truststore

```bash
keytool -import -alias local-ca -file local_ca.pem -keystore localhost.truststore.p12 -storetype pkcs12
```

Convert certificate to PKCS12 package

```bash
openssl pkcs12 -export -out keystore.p12 -in localhost.crt -inkey localhost.key -name localhost -macalg SHA1
```

Create keystore

```bash
keytool -J-Dkeystore.pkcs12.legacy -importkeystore -srckeystore keystore.p12 -destkeystore localhost.keystore.p12 -storetype pkcs12
```

You will be prompted to set a password for the keystore.

**(optional) use legacy flag `-J-Dkeystore.pkcs12.legacy` in case you want ZSS to use this keystore**

### Trust certificates of other services

API ML needs to validate the certificate of services that it accesses by API ML. API ML needs to validate the full certificate chain. Ensure that the following criteria are met:

* Import the public certificate of the root CA that has signed the service certificate to the API ML truststore.

* Ensure that your service has its own certificate and all intermediate CA certificates (if it was signed by intermediate CA) in its keystore.

**Note:** Validation fails if the service does not provide intermediate CA certificates to API ML. This can be circumvented by importing the intermediate CA certificates to the API ML truststore.

You can add a public certificate to the API ML trust store by calling in the directory with API ML.

Issue the following script:

```bash
    keytool -import -alias <alias> -file <path-to-certificate-in-PEM-format> -keystore localhost.truststore.p12 -storetype pkcs12
```

## Import the root certificate of a local CA to your browser

**Warning!** Be sure to only import the pre-generated certificate to a browser that you use for development and testing. Note that the private key is accessible to anyone.

Import [keystore/local_ca/localca.cer](/keystore/local_ca/localca.cer) to your root certificate store and trust it.

* For **Windows**, run the following command as an administrator:

    ```bash
    certutil -enterprise -f -v -AddStore "Root" keystore/local_ca/localca.cer 
    ```

    You have to open the terminal as administrator. This will install the certificate to the Trusted Root Certification Authorities.

  **Note:** You can use `npm run register-certificates-win` to run the preceding command. This requires `sudo` to be installed. If you don not have `sudo` available, install [chocolatey](https://chocolatey.org/docs/installation#install-downloaded-nuget-package-from-powershell), then run `chocolatey install sudo`.

* For **MacOS**, run the following command:

    ```bash
        sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain keystore/local_ca/localca.cer 
    ```

  Firefox uses its own certificate truststore. You can manually import your root certificate via the Firefox settings, or force Firefox to use the Windows trust store:

  Create a new Javascript file firefox-windows-truststore.js at C:\Program Files (x86)\Mozilla Firefox\defaults\pref with the following content:

  ```js
  /* Enable experimental Windows trust store support */
  pref("security.enterprise_roots.enabled", true);
  ```

### Disabling certificate validation on localhost

The default configuration of services for local development is to verify certificates of all services. Since the API ML service use correctly generated certificates, there should be no issues.

Follow these steps to quickly register an existing service without generating a certificate for it

1. Set the `apiml.security.ssl.verifySslCertificatesOfServices` configuration property to `false` from the default `true` for API ML services (Gateway, Discovery service and API Catalog).

2. Add the following options to the startup command of each service in `package.json` or in your IDE:

    ```bash
    --apiml.security.ssl.verifySslCertificatesOfServices=false
    ```
