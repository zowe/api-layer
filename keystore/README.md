# TLS Certificates for localhost

This README describes how to generate certificates for default local configuration.

Such certificates can also be used for integration tests.

TLS certificates are signed by a local certificate authority (CA) that is created as part of steps outlined in this topic and trusted by HTTPS clients in integration tests.

The root certificate of the local CA can also be imported to your browser.

You can use the provided key store and trust store, or create your own version of the key stores and local CA.

The last section of this README describes how to import and trust the local CA certificate on your system.

## Key stores

* `keystore/local/localhost.keystore.cer`
  * convenience
  * contains the exported server certificate signed by the local CA and private key for the server

* `keystore/local/localhost.keystore.key`
  * convenience
  * contains the exported private key

* `keystore/local/localhost.pem`
  * convenience
  * contains the exported server certificate in PEM format for use with http clients

* `keystore/local/localhost.keystore.p12`
  * password: `password`
  * used for the HTTPS server(s)
  * contains the server certificate signed by the local CA and private key for the server

* `keystore/local/localhost.truststore.p12`
  * password: `password`
  * used for HTTPS clients (e.g. integration tests, services using the gateway)
  * contains the root certificate of the local CA (not the server certificate)

* `keystore/local/localhost2.keystore.p12`
  * password: `password`
  * used for tests only, please refer to the particular tests for detils

* `keystore/local/localhost2.truststore.p12`
  * password: `password`
  * used for tests only, please refer to the particular tests for detils

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

## Generate keystores for local development

Keystores are automatically generated when running `./gradlew build`.
To manually regenerate, run:

```bash
./scripts/generate-keystores.sh
```

Or via Gradle:

```bash
./gradlew generateKeystores
```

Requirements: `openssl` and `keytool` (JRE) on PATH.

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
