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
    - password: ``password``
    - used for the HTTPS server(s) 
    - contains the server certificate signed by the local CA and private key for the server
    
  * `keystore/local/localhost.truststore.p12` 
    - password: ``password``
    - used for HTTPS clients (e.g. integration tests, services using the gateway) 
    - contains the root certificate of the local CA (not the server certificate)

  * `keystore/local/localhost2.keystore.p12` 
    - password: ``password``
    - used for tests only, please refer to the particular tests for detils
    
  * `keystore/local/localhost2.truststore.p12` 
    - password: ``password``
    - used for tests only, please refer to the particular tests for detils

### Local CA:

  * `keystore/local_ca/localca.cer`
    - public certificate of local CA
  
  * `keystore/local_ca/localca.keystore.p12`
    - private key of the local CA 

### Client certificates:

  * `keystore/client_cert/APIML_External_Certificate_Authority.cer`
  * `keystore/client_cert/APIML_External_Certificate_Authority.key`  
  API ML External Certificate authority - Certificate and private key of additional certificate authority that is trusted by apiml and can sign certificates that are used for authentication. Convenience export only.
  
  * `keystore/client_cert/APIMTST-cert.cer`
  * `keystore/client_cert/APIMTST-PRIVATEKEY.key`
  * `keystore/client_cert/UNKNOWNUSER-cert.cer`
  * `keystore/client_cert/UNKNOWNUSER-PRIVATEKEY.key`
  * `keystore/client_cert/USER-cert.cer`
  * `keystore/client_cert/USER-PRIVATEKEY.key`  
  Client certificates - used for testing of client certificate authentication functionality. APIMTST and UNKNOWNUSER respectively. Convenience export only.
    
  * `keystore/client_cert/client-certs.p12`  
  Keystore containing all the above including private keys. Used for testing client certificate authentication functionality.
  
  * `keystore/client_cert/openssl.conf`  
  openssl Configuration for certificate generation

### Certificates for NGINX proxy (for AT-TLS simulation):

The following files are used by the NGINX proxy to simulate AT_TLS on the CI server:

  * `keystore/localhost/Zowe_Service_Zowe_Development_Instances_Certificate_Authority_.cer`
  * `keystore/localhost/localca.cer`
  * `keystore/localhost/trusted_CAs.cer`

#### Generate additional client certificates using OPENSSL

Use the following script to generate a CSR:
  ```
  openssl req -newkey rsa:2048 -keyout PRIVATEKEY.key -out MYCSR.csr -config openssl.conf -outform PEM
  ```

Use the following script to display the CSR to verify the content:

**Note:** Be sure to contain all extensions (e.g. Extended Key Usage).
  ```
  openssl req -text -noout -verify -in MYCSR.csr
  ```

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

Use the following script to sign the CSR with API ML CA:  
```
openssl x509 -req -days 500 -in MYCSR.csr -CA APIML_External_Certificate_Authority.cer -CAkey APIML_External_Certificate_Authority.key -out server-cert.pem -CAcreateserial -sha256 -outform PEM -extfile openssl.conf -extensions v3_req
```

Use the following script to display the certificate content:

    openssl x509 -in server-cert.pem -text -noout

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

## Generate your own certificates for localhost
**Note:** The `apiml_cm.sh` script has been moved to [zowe/zowe-install-packaging repo](https://github.com/zowe/zowe-install-packaging).  

Use the following script to generate your own certificate for localhost:

    scripts/apiml_cm.sh --action setup

## Generate a certificate for a new service on localhost

Use the following script in the root of the `api-layer` repository:

    scripts/apiml_cm.sh --action new-service --service-alias <alias> --service-ext <ext> --service-keystore <keystore_path> --service-truststore <truststore_path> --service-dname <dname> --service-password <password> --service-validity <days> --local-ca-filename <localca_filename> --local-ca-password <localca_password>

    scripts/apiml_cm.sh --action new-service --service-alias <alias> --service-ext <ext> \
    --service-keystore <keystore_path> --service-truststore <truststore_path> \
    --service-dname <dname> --service-password <password> --service-validity <days> \
    --local-ca-filename $ZOWE_ROOT_DIR/components/api-mediation/keystore/local_ca/localca \
    --local-ca-password <localca_password>

* **`service-alias`**  
 is an unique string to identify the key entry. All keystore entries (key and trusted certificate entries) are accessed via unique aliases. Since the keystore will have only one certificate, you can omit this parameter and use the default value `localhost`.

* **`service-keystore`**  
 is a repository of security certificates plus corresponding private keys. The `<keystore_path>` is the path excluding the extension to the keystore that will be generated. It can be an absolute path or a path relative to the current working directory. The key store is generated in PKCS12 format with `.p12` extension. It should be a path in an existing directory where your service expects the keystore. For example: `/opt/myservice/keystore/service.keystore`.


* **`service-truststore`**  
 contains certificates from other parties that you expect to communicate with, or from Certificate Authorities that you trust to identify other parties. The `<truststore_path>` is the path excluding the extension to the trust store that will be generated. It can be an absolute path or a path relative to the current working directory. The truststore is generated in PKCS12 format.

* **`service-ext`**  
 specifies the X.509 extension that should be the Subject Alternate Name (SAN). The SAN has contain host names that are used to access the service. You need to specify the same hostname that is used by the service during API Mediation Layer registration. 

  **Example:**

    "SAN=dns:localhost.localdomain,dns:localhost,ip:127.0.0.1"

  **Note:** For more details about SAN, see the section *SAN or SubjectAlternativeName* at [Java Keytool - Common Options](https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/keytoolDocs/commonoptions.html).

* **`service-dname`**  
 is the X.509 Distinguished Name and is used to identify entities, such as those which are named by the subject and issuer (signer) fields of X.509 certificates. 

  **Example:**


    "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"


* **`service-validity`**  
is the number of days after that the certificate will expire.

* **`service-password`**  
 is the keystore password. The purpose of the password is the integrity check. The access protection for the keystore and keystore need to be achieved by making them accessible only by the ZOVESVR user ID and the system administrator.

* **`local-ca-filename`**  
is the path to the keystore that is used to sign your new certificate with the local CA private key.  
**Note:** If you are in the `$ZOWE_RUNTIME/components/api-mediation-directory`, you can omit this parameter. It should point to the `$ZOWE_ROOT_DIR/components/api-mediation/keystore/local_ca/localca`.

* **`local-ca-password`**  
is the local CA keystore password. You can omit it unless you has used a different password during `setup` action.


### Use the service certificate in the PEM format

Some utilities (e.g. `http`) require the service certificate to be in PEM format. The `apiml_cm.sh` script converts certificate
to PEM format:

- The public certificate of services: `keystore/localhost/localhost.keystore.cer`
- The public certificate of the local CA: `keystore/local_ca/localca.cer`

The private key of the service certificate: `keystore/localhost/localhost.keystore.key`


### Trust certificates of other services

API ML needs to validate the certificate of services that it accesses by API ML. API ML needs to validate the full certificate chain. Ensure that the following criteria are met:

* Import the public certificate of the root CA that has signed the service certificate to the API ML truststore.

* Ensure that your service has its own certificate and all intermediate CA certificates (if it was signed by intermediate CA) in its keystore.

**Note:** Validation fails if the service does not provide intermediate CA certificates to API ML. This can be circumvented by importing the intermediate CA certificates to the API ML truststore.

You can add a public certificate to the API ML trust store by calling in the directory with API ML.

Issue the following script:

    scripts/apiml_cm.sh --action trust --certificate <path-to-certificate-in-PEM-format> --alias <alias>


### Self-signed certificate

You can also use a self-signed certificate. Use the following procedure.

1. Generate a self-signed certificate for a service:  

  ```
  mkdir -p keystore/selfsigned
  scripts/apiml_cm.sh --action new-self-signed-service  --service-keystore keystore/selfsigned/localhost.keystore --service-truststore keystore/selfsigned/localhost.truststore  --service-dname "CN=Zowe Self-Signed Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"
  ```

2. Trust the certificate in API ML:

```
scripts/apiml_cm.sh --action trust --alias selfsigned --certificate keystore/selfsigned/localhost.keystore.cer
```

**Example:**

If you have the sample service described in the User Guide at [this link](https://github.com/zowe/docs-site/blob/api-mediation-doc/docs/user-guide/api-mediation/api-mediation-onboard-overview.md#sample-rest-api-service) you need to complete the following steps:

1. Go to the directory with the sample service:
    ```
    cd <your_directory>/swagger-samples/java/java-spring-boot
    ```
2. Make a directory for keystore:
    ```
    mkdir keystore
    ```
3. Call the script:
    ```
    <api-layer-repository>/scripts/apiml_cm.sh --action new-service --service-alias petstore --service-ext SAN=dns:localhost.localdomain,dns:localhost --service-keystore keystore/localhost.keystore.p12 --service-truststore keystore/localhost.truststore.p12 --service-dname "CN=Petstore Service, OU=orgUnit, O=org, L=city, S=state, C=country" --service-password password --service-validity 365 --local-ca-filename <api-layer-repository>/keystore/local_ca/localca
    ```
    You have generated the certificate and private key to the `keystore` directory.

4. Configure HTTPS for the sample service by adding the following properties to `src/main/resources/application.properties`:  

    ```
    server.ssl.keyAlias=petstore
    server.ssl.keyPassword=password
    server.ssl.keyStoreType=PKCS12
    server.ssl.keyStore=keystore/localhost.keystore.p12
    server.ssl.keyStorePassword=password
    server.ssl.trustStore=keystore/localhost.truststore.p12
    server.ssl.trustStorePassword=password
    server.ssl.trustStoreType=PKCS12
    ```
  
  
5. Repackage the application:  
    ```
    mvn package
    ```
     
6. Start:
    ```
    java -jar target/swaggerhub-spring-boot-sample-1.0.0-SNAPSHOT.jar --server.port=8443 
    ```

    You now have a service that is using a certificate signed by local CA and will be trusted by your API Mediation Layer.

7. (Optional) Access your service on localhost.  

    [https://localhost:8443/v2/swagger.json](https://localhost:8443/v2/swagger.json)

8. You can onboard the service to the API Mediation Layer by following instructions in [https://github.com/zowe/docs-site/blob/api-mediation-doc/docs/user-guide/api-mediation/api-mediation-onboard-overview.md](https://github.com/zowe/docs-site/blob/api-mediation-doc/docs/user-guide/api-mediation/api-mediation-onboard-overview.md).


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

### Testing `apiml_cm.sh` on z/OS

You can use following script from the `ca-api-layer` repository to test the script.

Issue the following script:

    python3 ~/workspace/ca-api-layer/scripts/zossh.py ca32 /a/plape03/localca scripts/apiml_cm.sh --action setup


### Disabling certificate validation on localhost

The default configuration of services for local development is to verify certificates of all services. Since the API ML service use correctly generated certificates, there should be no issues.

Follow these steps to quickly register an existing service without generating a certificate for it

1. Issue the following command:

    `apiml_cm.sh --action new-service` 

2. Set the `apiml.security.ssl.verifySslCertificatesOfServices` configuration property to `false` from the default `true` for API ML services (Gateway, Discovery service and API Catalog). 

3. Add the following options to the startup command of each service in `package.json` or in your IDE:
    ```
    --apiml.security.ssl.verifySslCertificatesOfServices=false
    ```

