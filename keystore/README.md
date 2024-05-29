# TLS Certificates for localhost

This README describes how to generate certificates for default local configuration.

Such certificates can also be used for integration tests.

TLS certificates are signed by a local certificate authority (CA) that is created as part of steps outlined in this topic and trusted by HTTPS clients in integration tests.

The root certificate of the local CA can also be imported to your browser.

You can use the provided key store and trust store, or create your own version of the key stores and local CA.

The last section of this README describes how to import and trust the local CA certificate on your system.

## Key stores:

  * `keystore/local/localhost.keystore.cer` 
    - convenience 
    - contains the exported server certificate signed by the local CA and private key for the server

  * `keystore/local/localhost.keystore.key` 
    - convenience 
    - contains the exported private key 

  * `keystore/local/localhost.pem` 
    - convenience 
    - contains the exported server certificate in PEM format for use with http clients

  * `keystore/local/localhost.keystore.p12` 
    - password: `password`
    - used for the HTTPS server(s) 
    - contains the server certificate signed by the local CA and private key for the server
    
  * `keystore/local/localhost.truststore.p12` 
    - password: `password`
    - used for HTTPS clients (e.g. integration tests, services using the gateway) 
    - contains the root certificate of the local CA (not the server certificate)

  * `keystore/local/localhost2.keystore.p12` 
    - password: `password`
    - used for tests only, please refer to the particular tests for detils
    
  * `keystore/local/localhost2.truststore.p12` 
    - password: `password`
    - used for tests only, please refer to the particular tests for detils

### Local CA:

  * `keystore/local_ca/localca.cer`
    - public certificate of local CA
  
  * `keystore/local_ca/localca.keystore.p12`
    - password: `local_ca_password`
    - private key of the local CA 

### Client certificates:

  * `keystore/client_cert/ca/apiml_ca.p12`
  API ML External Certificate authority - Certificate and private key of additional certificate authority that is trusted by apiml and can sign certificates that are used for authentication. Convenience export only.

* `keystore/client_cert/client-certs.p12`  

  Client certificates - used for testing of client certificate authentication functionality. APIMTST, USER and UNKNOWNUSER.
  Keystore containing all the above including private keys. Used for testing client certificate authentication functionality.
  


  * `keystore/client_cert/openssl.conf`  
  openssl Configuration for certificate generation

### Certificates for NGINX proxy (for AT-TLS simulation):

The following files are used by the NGINX proxy to simulate AT_TLS on the CI server:

  * `keystore/localhost/Zowe_Service_Zowe_Development_Instances_Certificate_Authority_.cer`
  * `keystore/localhost/localca.cer`
  * `keystore/localhost/trusted_CAs.cer`

## Generate your own certificates for localhost

### (Optional)Generate certificate authority
create private key
```
    openssl genrsa -out local_ca.key 2048
```
create certificate
```
    openssl req -x509 -new -nodes -key local_ca.key -sha256 -days 1825 -out local_ca.pem
```
### Generate certificate

#### generate CSR together with private key in PEM format

    openssl req -newkey rsa:2048 -nodes -keyout localhost.key -sha256 -out localhost.csr -outform PEM

**Verify CSR**

    openssl req -text -noout -verify -in localhost.csr

Example of a valid CSR:

```
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
```
openssl x509 -req -in localhost.csr -CA local_ca.pem -CAkey local_ca.key \
-CAcreateserial -out localhost.crt -days 1825 -sha256 -extfile keystore/client_cert/openssl.conf -extensions v3_req
```

Use the following script to display the certificate content:

    openssl x509 -in localhost.pem -text -noout

Example of a valid signed certificate:

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

#### Create PKCS12 truststore and keystore

Create truststore
```
keytool -import -alias local-ca -file local_ca.pem -keystore localhost.truststore.p12 -storetype pkcs12
```

Convert certificate to PKCS12 package
```
openssl pkcs12 -export -out keystore.p12 -in localhost.crt -inkey localhost.key -name localhost -macalg SHA1
```

Create keystore

```
keytool -J-Dkeystore.pkcs12.legacy -importkeystore -srckeystore mvsde12-keystore.p12 -destkeystore localhost.keystore.p12 -storetype pkcs12
```
**(optional) use legacy flag `-J-Dkeystore.pkcs12.legacy` in case you want ZSS to use this keystore**

### Trust certificates of other services

API ML needs to validate the certificate of services that it accesses by API ML. API ML needs to validate the full certificate chain. Ensure that the following criteria are met:

* Import the public certificate of the root CA that has signed the service certificate to the API ML truststore.

* Ensure that your service has its own certificate and all intermediate CA certificates (if it was signed by intermediate CA) in its keystore.

**Note:** Validation fails if the service does not provide intermediate CA certificates to API ML. This can be circumvented by importing the intermediate CA certificates to the API ML truststore.

You can add a public certificate to the API ML trust store by calling in the directory with API ML.

Issue the following script:

    keytool -import -alias <alias> -file <path-to-certificate-in-PEM-format> -keystore localhost.truststore.p12 -storetype pkcs12

## Import the root certificate of a local CA to your browser

**Warning!** Be sure to only import the pre-generated certificate to a browser that you use for development and testing. Note that the private key is accessible to anyone. 

Import [keystore/local_ca/localca.cer](/keystore/local_ca/localca.cer) to your root certificate store and trust it. 

* For **Windows**, run the following command as an administrator:
    ```
    certutil -enterprise -f -v -AddStore "Root" keystore/local_ca/localca.cer 
    ```
    You have to open the terminal as administrator. This will install the certificate to the Trusted Root Certification Authorities. 

  **Note:** You can use `npm run register-certificates-win` to run the preceding command. This requires `sudo` to be installed. If you don not have `sudo` available, install [chocolatey](https://chocolatey.org/docs/installation#install-downloaded-nuget-package-from-powershell), then run `chocolatey install sudo`.
    
* For **MacOS**, run the following command: 
    ```
    $ sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain keystore/local_ca/localca.cer 
    ```
  Firefox uses its own certificate truststore. You can manually import your root certificate via the Firefox settings, or force Firefox to use the Windows trust store:

  Create a new Javascript file firefox-windows-truststore.js at C:\Program Files (x86)\Mozilla Firefox\defaults\pref with the following content:
  ```
  /* Enable experimental Windows trust store support */
  pref("security.enterprise_roots.enabled", true);
  ```

### Disabling certificate validation on localhost

The default configuration of services for local development is to verify certificates of all services. Since the API ML service use correctly generated certificates, there should be no issues.

Follow these steps to quickly register an existing service without generating a certificate for it

1. Set the `apiml.security.ssl.verifySslCertificatesOfServices` configuration property to `false` from the default `true` for API ML services (Gateway, Discovery service and API Catalog). 

2. Add the following options to the startup command of each service in `package.json` or in your IDE:
    ```
    --apiml.security.ssl.verifySslCertificatesOfServices=false
    ```

