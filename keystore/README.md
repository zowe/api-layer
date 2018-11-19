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


The `service-alias` is an unique string to identify the key entry. All keystore entries (key and trusted certificate entries) are accessed via unique aliases.

The `service-keystore` is a repository of security certificates plus corresponding private keys. The `<keystore_path>` is the path including the extension to the key store that will be generated. It can be an absolute path or a path relative to the current working directory. The key store is generated in PKCS12 format.


The `service-truststore` contains certificates from other parties that you expect to communicate with, or from Certificate Authorities that you trust to identify other parties. The `<truststore_path>` is the path including the extension to the trust store that will be generated. It can be an absolute path or a path relative to the current working directory. The trust store is generated in PKCS12 format.

The `service-ext` specifies the X.509 extension that should be the Subject Alternate Name (SAN) for example:

    "SAN=dns:localhost.localdomain,dns:localhost"

The `service-dname` is the X.509 Distinguished Name and is used to identify entities, such as those which are named by the subject and issuer (signer) fields of X.509 certificates. 

The `service-validity` is the number of days after that the certificate will expire.

The `service-password` is the keystore password.

The `local-ca-filename` follows the format `keystore/{path}/{suffix}` and is used to generate the keystore with the local CA private key and local CA public certificate.

The `local-ca-password` is the local CA keystore password.


### Export the service certificate to PEM format

Some utilities (e.g. `http`) requires the service certificate to be in the PEM format. Use following command to convert it to the PEM format:

    openssl pkcs12 -in keystore/localhost/localhost.keystore.p12 -out keystore/localhost/localhost.pem -passin 'pass:password' -nodes


### Example

If you have the sample service described in the User Guide at [this link](https://github.com/zowe/docs-site/blob/api-mediation-doc/docs/user-guide/api-mediation/api-mediation-onboard-overview.md#sample-rest-api-service) you need to complete the following steps:

1. Go to the directory with the sample service:

        cd <your_directory>/swagger-samples/java/java-spring-boot

2. Make directory for keystore:

        mkdir keystore

3. Call the script:

        <api-layer-repository>/scripts/apiml_cm.sh --action new-service --service-alias petstore --service-ext SAN=dns:localhost.localdomain,dns:localhost --service-keystore keystore/localhost.keystore.p12 --service-truststore keystore/localhost.truststore.p12 --service-dname "CN=Petstore Service, OU=orgUnit, O=org, L=city, S=state, C=country" --service-password password --service-validity 365 --local-ca-filename ~/workspace/zowe/api-layer/keystore/local_ca/localca

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
