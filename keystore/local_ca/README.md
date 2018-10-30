# Certificate for localhost

This guide shows how the certificate for default local settings was created.
The certificate is used for integration tests as well.

It is signed by internal/local certificate authority (CA) that is created as part of these steps and is trusted by HTTPS clients in the integration tests.
The root certificate of the CA can be imported to your browser as well.

You can use the provided keystores or create your own version of the keystore and local CA.

Last section of this document shows how to import and trust the local CA certificate on your system.

Key stores:

  * `keystore/local/localhost_server.jks` 
    - password: ``password``
    - used for the HTTPS server(s) 
    - contains the server certificate signed by the local CA and private key for the server
    
  * `keystore/local/localhost_truststore.jks` 
    - password: ``trustword``
    - used for HTTPS clients (e.g. integration tests, services using the gateway) 
    - contains the root certificate of the local CA (not the server certificate)


## Process how create them

##### Check versions

The following commands were done using:

    openssl version                                                      
    OpenSSL 1.0.2o  27 Mar 2018

    java -version
    java version "1.8.0_162"
    Java(TM) SE Runtime Environment (build 1.8.0_162-b12)
    Java HotSpot(TM) 64-Bit Server VM (build 25.162-b12, mixed mode)


##### Create local certificate authority (CA)

The CA related commands are storing files into directory ``keystore/local_ca``.

All the commands in this document should be executed from the repository root.
    
Generate our private key:

    $ openssl genrsa -des3 -out keystore/local_ca/local_ca.key 2048

The password is stored in [Jenkins Credentials](https://wash.zowe.org:8443/credentials/) as ``api-layer-local-ca-password``.

Generate a root certificate:

    $ openssl req -x509 -new -nodes -key keystore/local_ca/local_ca.key -sha256 -days 3650 -out keystore/local_ca/local_ca.pem

    Country Name (2 letter code) [AU]:CZ
    State or Province Name (full name) [Some-State]:
    Locality Name (eg, city) []:Prague
    Organization Name (eg, company) [Internet Widgits Pty Ltd]:Zowe
    Organizational Unit Name (eg, section) []:API Mediation Layer
    Common Name (e.g. server FQDN or YOUR name) []:Zowe Development Instances Certificate Authority
    Email Address []:

A config file is needed to define the Subject Alternative Name (SAN) extension. It is a file named `local_ca.ext`.


##### Create localhost certificate

    $ keytool -genkey -alias localhost -keyalg RSA -keysize 2048 -keystore keystore/local/localhost_server.jks -dname "CN=localhost,OU=API Mediation Layer,O=Zowe,C=CZ" -ext SAN=dns:localhost.localdomain,dns:localhost -validity 3650 -keypass password -storepass password
    
    $ cp keystore/local/localhost_server.jks keystore/local/localhost_server_orig.jks  

    $ keytool -certreq -alias localhost -file keystore/local/localhost.csr -keystore keystore/local/localhost_server.jks -ext SAN=dns:localhost.localdomain,dns:localhost -validity 3650 -keypass password -storepass password 

    $ keytool -printcertreq -file keystore/local/localhost.csr -keypass password -storepass password 
    # or you can use: openssl req -text -noout -verify -in keystore/local/localhost.csr
    

##### Sign the CSR using local CA    

    $ openssl x509 -req -in keystore/local/localhost.csr -CA keystore/local_ca/local_ca.pem -CAkey keystore/local_ca/local_ca.key -CAcreateserial -out keystore/local/localhost.crt -days 3650 -sha256 -extfile keystore/local_ca/local_ca.ext 

Signed certificate is [/keystore/local/localhost.csr](/keystore/local/localhost.csr).


##### Import it into Java keystore for server

    $ keytool -import -alias local_ca_root -keystore keystore/local/localhost_server.jks -trustcacerts -file keystore/local_ca/local_ca.pem -storepass password
    
    $ keytool -import -alias localhost -keystore keystore/local/localhost_server.jks -file keystore/local/localhost.crt -keypass password -storepass password
    
    Certificate reply was installed in keystore


##### Import the root certificate to client truststore

    $ keytool -import -alias local_ca_root -keystore keystore/local/localhost_truststore.jks -trustcacerts -file keystore/local_ca/local_ca.pem -storepass trustword
    
    Certificate was added to keystore
    

## Import the root certificate to browser

*WARNING!* You can import it only to a browser that you use for development and testing.
People from your team and others who will find out what the private key password is will be able to generate certificates that your browser will trust.    

Import [/keystore/local_ca/local_ca.pem](/keystore/local_ca/local_ca.pem) to your root certificate store and trust it. 

For Mac you can you following command: 

    $ sudo security add-trusted-cert -d -r trustRoot -k ~/Library/Keychains/login.keychain keystore/local_ca/local_ca.pem

    
    