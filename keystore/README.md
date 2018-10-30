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
    - password: ``trustword``
    - used for HTTPS clients (e.g. integration tests, services using the gateway) 
    - contains the root certificate of the local CA (not the server certificate)

Local CA:

  * `keystore/local_ca/localca.cer`
    - public certificate of local CA
  
  * `keystore/local_ca/localca.keystore.p12`
    - private key of the local CA 


## Process how create them

Use following script:

    scripts/apiml_cm.sh --action setup


## Import the root certificate of local CA to your browser

*WARNING!* You can import the pre-generated certificate only to a browser that you use for development and testing. Everyone has access to the private key.

Import [keystore/local_ca/localca.cer](/keystore/local_ca/localca.cer) to your root certificate store and trust it. 

You can you following command om Mac: 

    $ sudo security add-trusted-cert -d -r trustRoot -k /Library/Keychains/System.keychain keystore/local_ca/localca.cer 


