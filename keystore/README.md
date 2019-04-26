# TLS Certificates for localhost

This document describes how the certificates for default local configuration are generated.

The certificates are used for integration tests as well.

They are signed by local certificate authority (CA) that is created as part of these steps and is trusted by HTTPS clients in the integration tests.

The root certificate of the local CA can be imported to your browser as well.

You can use the provided key store and trust store or create your own version of the key stores and local CA.

Last section of this document shows how to import and trust the local CA certificate on your system.

Key stores:

  * `keystore/local/localhost.keystore.p12` 
    - password: ``password``
    - used for the HTTPS server(s) 
    - contains the server certificate signed by the local CA and private key for the server
    
  * `keystore/local/localhost.truststore.p12` 
    - password: ``password``
    - used for HTTPS clients (e.g. integration tests, services using the gateway) 
    - contains the root certificate of the local CA (not the server certificate)

Local CA:

  * `keystore/local_ca/localca.cer`
    - public certificate of local CA
  
  * `keystore/local_ca/localca.keystore.p12`
    - private key of the local CA 


## Generating own certificates for localhost

Use following script:

    scripts/apiml_cm.sh --action setup


## Generating certificate for a new service on localhost

Use the following script in the root of the `api-layer` repository:

    scripts/apiml_cm.sh --action new-service --service-alias <alias> --service-ext <ext> --service-keystore <keystore_path> --service-truststore <truststore_path> --service-dname <dname> --service-password <password> --service-validity <days> --local-ca-filename <localca_filename> --local-ca-password <localca_password>

    scripts/apiml_cm.sh --action new-service --service-alias <alias> --service-ext <ext> \
    --service-keystore <keystore_path> --service-truststore <truststore_path> \
    --service-dname <dname> --service-password <password> --service-validity <days> \
    --local-ca-filename $ZOWE_ROOT_DIR/api-mediation/keystore/local_ca/localca \
    --local-ca-password <localca_password>

The `service-alias` is an unique string to identify the key entry. All keystore entries (key and trusted certificate entries) are accessed via unique aliases. Since the keystore will have only one certificate, you can omit this parameter and use the default value `localhost`.

The `service-keystore` is a repository of security certificates plus corresponding private keys. The `<keystore_path>` is the path excluding the extension to the keystore that will be generated. It can be an absolute path or a path relative to the current working directory. The key store is generated in PKCS12 format with `.p12` extension. It should be path in an existing directory where your service expects the keystore. For example: `/opt/myservice/keystore/service.keystore`.

The `service-truststore` contains certificates from other parties that you expect to communicate with, or from Certificate Authorities that you trust to identify other parties. The `<truststore_path>` is the path excluding the extension to the trust store that will be generated. It can be an absolute path or a path relative to the current working directory. The truststore is generated in PKCS12 format.

The `service-ext` specifies the X.509 extension that should be the Subject Alternate Name (SAN). The SAN has contain host names that are used to access the service. You need specify the same hostname that is used by the service during API Mediation Layer registration. For example:

    "SAN=dns:localhost.localdomain,dns:localhost,ip:127.0.0.1"

*Note:* For more details about SAN, see section *SAN or SubjectAlternativeName* at [Java Keytool - Common Options](https://www.ibm.com/support/knowledgecenter/en/SSYKE2_8.0.0/com.ibm.java.security.component.80.doc/security-component/keytoolDocs/commonoptions.html).

The `service-dname` is the X.509 Distinguished Name and is used to identify entities, such as those which are named by the subject and issuer (signer) fields of X.509 certificates. For example:

    "CN=Zowe Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"


The `service-validity` is the number of days after that the certificate will expire.

The `service-password` is the keystore password. The purpose of the password is the integrity check. The access protection for the keystore and keystore need to be achieved by making them accessible only by the ZOVESVR user ID and the system administrator.

The `local-ca-filename` is the path to the keystore that is used to sign your new certificate with the local CA private key. If you an in the `$ZOWE_RUNTIME/api-mediation-directory`, you can omit this parameter. It should point to the `$ZOWE_ROOT_DIR/api-mediation/keystore/local_ca/localca`.

The `local-ca-password` is the local CA keystore password. You can omit it unless you has used a different password during `setup` action.


### Use the service certificate in the PEM format

Some utilities (e.g. `http`) requires the service certificate to be in the PEM format. The `apiml_cm.sh` script is converting the certificate
to the PEM format:

- The public certificate of services: `keystore/localhost/localhost.keystore.cer`
- The public certificate of the local CA: `keystore/local_ca/localca.cer`

The private key of the service certificate: `keystore/localhost/localhost.keystore.key`


### Trust certificates of other services

The APIML needs to validate the certificate of the services that it accessed by the APIML. The APIML needs to validate the full certificate chain. It usually means that:

1. You need to import the public certificate of the root CA that has signed the certificate of the service to the APIML truststore.

2. Your service needs to have its own certificate and all intermediate CA certificates (if it was signed by intermediate CA) in its keystore.

    - If the service does not provide intermediate CA certificates to the APIML then the validation fails. This can be circumvented by importing the intermediate CA certificates to the APIML truststore.

You can add a public certificate to the APIML trust store by calling in the directory with APIML:

    scripts/apiml_cm.sh --action trust --certificate <path-to-certificate-in-PEM-format> --alias <alias>


### Self-signed certificate

You can also use a self-signed certificate.

1. Generate a self-signed certificate for a service:

        mkdir -p keystore/selfsigned
        scripts/apiml_cm.sh --action new-self-signed-service --service-keystore keystore/selfsigned/localhost.keystore --service-truststore keystore/selfsigned/localhost.truststore  --service-dname "CN=Zowe Self-Signed Service, OU=API Mediation Layer, O=Zowe Sample, L=Prague, S=Prague, C=CZ"

2. Trust it in the APIML:

        scripts/apiml_cm.sh --action trust --alias selfsigned --certificate keystore/selfsigned/localhost.keystore.cer


### Example

If you have the sample service described in the User Guide at [this link](https://github.com/zowe/docs-site/blob/api-mediation-doc/docs/user-guide/api-mediation/api-mediation-onboard-overview.md#sample-rest-api-service) you need to complete the following steps:

1. Go to the directory with the sample service:

        cd <your_directory>/swagger-samples/java/java-spring-boot

2. Make directory for keystore:

        mkdir keystore

3. Call the script:

        <api-layer-repository>/scripts/apiml_cm.sh --action new-service --service-alias petstore --service-ext SAN=dns:localhost.localdomain,dns:localhost --service-keystore keystore/localhost.keystore.p12 --service-truststore keystore/localhost.truststore.p12 --service-dname "CN=Petstore Service, OU=orgUnit, O=org, L=city, S=state, C=country" --service-password password --service-validity 365 --local-ca-filename <api-layer-repository>/keystore/local_ca/localca

4. This generated the certificate and private key to the `keystore` directory. You need to configure the HTTPS for the sample service. This can be done by adding the following properties to the `src/main/resources/application.properties`:
        
        server.ssl.keyAlias=petstore
        server.ssl.keyPassword=password
        server.ssl.keyStoreType=PKCS12
        server.ssl.keyStore=keystore/localhost.keystore.p12
        server.ssl.keyStorePassword=password
        server.ssl.trustStore=keystore/localhost.truststore.p12
        server.ssl.trustStorePassword=password
        server.ssl.trustStoreType=PKCS12

5. Repackage the application:

        mvn package

6. Start:

        java -jar target/swaggerhub-spring-boot-sample-1.0.0-SNAPSHOT.jar --server.port=8443 

7. Now you have a service that is using a certificate signed by local CA and will be trusted by your API Mediation Layer.
   You can try it by accessing: [https://localhost:8443/v2/swagger.json](https://localhost:8443/v2/swagger.json)

8. You can onboard the service to the API Mediation Layer by following instructions in [https://github.com/zowe/docs-site/blob/api-mediation-doc/docs/user-guide/api-mediation/api-mediation-onboard-overview.md](https://github.com/zowe/docs-site/blob/api-mediation-doc/docs/user-guide/api-mediation/api-mediation-onboard-overview.md).


## Import the root certificate of local CA to your browser

*WARNING!* You can import the pre-generated certificate only to a browser that you use for development and testing. Everyone has access to the private key.

Import [keystore/local_ca/localca.cer](/keystore/local_ca/localca.cer) to your root certificate store and trust it. 

For **Windows**, you can run the following command:

    certutil -enterprise -f -v -AddStore "Root" keystore/local_ca/localca.cer 
    
You have to open the terminal as administrator. This will install the certificate to the Trusted Root Certification Authorities. 

If you're using **MacOS**, you can run the following command: 

    $ sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain keystore/local_ca/localca.cer 

Firefox uses its own certificate truststore. You can manually import your root certificate via the Firefox settings, or force Firefox to use the Windows trust store:

Create a new Javascript file firefox-windows-truststore.js at C:\Program Files (x86)\Mozilla Firefox\defaults\pref with the following content:

    /* Enable experimental Windows trust store support */
    pref("security.enterprise_roots.enabled", true);


### Testing `apiml_cm.sh` on z/OS

You can use following script from the `ca-api-layer` repository to test the script:

    python3 ~/workspace/ca-api-layer/scripts/zossh.py ca32 /a/plape03/localca scripts/apiml_cm.sh --action setup


### Disabling certificate validation on localhost

The default configuration of services for local development is to verify certificates of all services. Since the APIML service use correctly generated certificates, there should be no issues.

However, you may want to quickly register an existing service without generating a certificate for it using the `apiml_cm.sh --action new-service` command.

You can do it by setting the `apiml.security.ssl.verifySslCertificatesOfServices` configuration property to `false` from the default `true` for the APIML services (gateway, discovery service and API catalog). 
This can be done by adding following options to the startup command of each service in `package.json` or in your IDE:

    --apiml.security.ssl.verifySslCertificatesOfServices=false


